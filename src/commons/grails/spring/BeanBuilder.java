/*
 * Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package grails.spring;

import groovy.lang.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.spring.BeanConfiguration;
import org.codehaus.groovy.grails.commons.spring.DefaultBeanConfiguration;
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.*;

/**
 * <p>Runtime bean configuration wrapper. Like a Groovy builder, but more of a DSL for
 * Spring configuration. Allows syntax like:</p>
 * 
 * <pre>
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 * 
 * BeanBuilder builder = new BeanBuilder()
 * builder.beans {
 *   dataSource(BasicDataSource) {                  // <--- invokeMethod
 *      driverClassName = "org.hsqldb.jdbcDriver"
 *      url = "jdbc:hsqldb:mem:grailsDB"
 *      username = "sa"                            // <-- setProperty
 *      password = ""
 *      settings = [mynew:"setting"]
 *  } 
 *  sessionFactory(SessionFactory) {
 *  	   dataSource = dataSource                 // <-- getProperty for retrieving refs
 *  }
 *  myService(MyService) {
 *      nestedBean = { AnotherBean bean->          // <-- setProperty with closure for nested bean
 *      		dataSource = dataSource
 *      }
 *  }
 * }
 * </pre>
 * <p>
 *   You can also use the Spring IO API to load resources containing beans defined as a Groovy
 *   script using either the constructors or the loadBeans(Resource[] resources) method
 * </p>
 * 
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public class BeanBuilder extends GroovyObjectSupport {
	private static final Log LOG = LogFactory.getLog(BeanBuilder.class);
	private static final String CREATE_APPCTX = "createApplicationContext";
    private static final String REGISTER_BEANS = "registerBeans";
    private static final String BEANS = "beans";
    private static final String REF = "ref";
    private RuntimeSpringConfiguration springConfig;
    private BeanConfiguration currentBeanConfig;
    private Map deferredProperties = new HashMap();
    private ApplicationContext parentCtx;
    private Map binding = Collections.EMPTY_MAP;
    private ClassLoader classLoader = null;


    public BeanBuilder() {
		this(null,null);
	}
    
    public BeanBuilder(ClassLoader classLoader) {
		this(null, classLoader);
	}    
	
	public BeanBuilder(ApplicationContext parent) {
        this(parent, null);
	}	
	
	public BeanBuilder(ApplicationContext parent,ClassLoader classLoader) {
		super();
        this.classLoader = classLoader == null ? getClass().getClassLoader() : classLoader;
        this.parentCtx = parent;
        this.springConfig = createRuntimeSpringConfiguration(parent, classLoader);
    }

    protected RuntimeSpringConfiguration createRuntimeSpringConfiguration(ApplicationContext parent, ClassLoader classLoader) {
        return new DefaultRuntimeSpringConfiguration(parent, classLoader);
    }

    public Log getLog() {
		return LOG;
	}
	
	/**
	 * Retrieves the parent ApplicationContext
	 * @return The parent ApplicationContext
	 */
	public ApplicationContext getParentCtx() {
		return parentCtx;
	}

	/**
	 * Retrieves the RuntimeSpringConfiguration instance used the the BeanBuilder
	 * @return The RuntimeSpringConfiguration instance
	 */
	public RuntimeSpringConfiguration getSpringConfig() {
		return springConfig;
	}

	/**
	 * Retrieves a BeanDefinition for the given name
	 * @param name The bean definition
	 * @return The BeanDefinition instance
	 */
	public BeanDefinition getBeanDefinition(String name) {
		if(!getSpringConfig().containsBean(name))
			return null;
		return getSpringConfig().getBeanConfig(name).getBeanDefinition();
	}

    /**
     * Retrieves all BeanDefinitions for this BeanBuilder
     *
     * @return A map of BeanDefinition instances with the bean id as the key
     */
    public Map getBeanDefinitions() {

        Map beanDefinitions = new HashMap();
        final List beanNames = getSpringConfig().getBeanNames();
        for (Iterator i = beanNames.iterator(); i.hasNext();) {
            String beanName = (String) i.next();
            BeanDefinition bd = getSpringConfig()
                                    .getBeanConfig(beanName)
                                    .getBeanDefinition();
            beanDefinitions.put(beanName,bd);
        }
        return beanDefinitions;
    }

    /**
	 * Sets the runtime Spring configuration instance to use. This is not necessary to set
	 * and is configured to default value if not, but is useful for integrating with other
	 * spring configuration mechanisms @see org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
	 * 
	 * @param springConfig The spring config
	 */
	public void setSpringConfig(RuntimeSpringConfiguration springConfig) {
		this.springConfig = springConfig;
	}



	/**
	 * This class is used to defer the adding of a property to a bean definition until later
	 * This is for a case where you assign a property to a list that may not contain bean references at
	 * that point of asignment, but may later hence it would need to be managed
	 * 
	 * @author Graeme Rocher
	 */
	private class DeferredProperty {
		private BeanConfiguration config;
		private String name;
		private Object value;

		DeferredProperty(BeanConfiguration config, String name, Object value) {
			this.config = config;
			this.name = name;
			this.value = value;
		}

		public void setInBeanConfig() {
			this.config.addProperty(name, value);
		}
	}
	
	/**
	 * A RuntimeBeanReference that takes care of adding new properties to runtime references
	 * 
	 * @author Graeme Rocher
	 * @since 0.4
	 *
	 */
	private class ConfigurableRuntimeBeanReference extends RuntimeBeanReference implements GroovyObject {

		private MetaClass metaClass;
		private BeanConfiguration beanConfig;

		public ConfigurableRuntimeBeanReference(String beanName, BeanConfiguration beanConfig) {
			this(beanName, beanConfig, false);			
		}

		public ConfigurableRuntimeBeanReference(String beanName, BeanConfiguration beanConfig, boolean toParent) {
			super(beanName, toParent);
			this.beanConfig = beanConfig;
			if(beanConfig == null)
				throw new IllegalArgumentException("Argument [beanConfig] cannot be null");
			this.metaClass = InvokerHelper.getMetaClass(this);
		}

		public MetaClass getMetaClass() {
			return this.metaClass;
		}

		public Object getProperty(String property) {
			if(property.equals("beanName"))
				return getBeanName();
			else if(property.equals("source"))
				return getSource();
			else if(this.beanConfig != null) {
				return new WrappedPropertyValue(property,beanConfig.getPropertyValue(property));
			}				
			else
				return this.metaClass.getProperty(this, property);
		}



        /**
		 * Wraps a BeanConfiguration property an ensures that any RuntimeReference additions to it are
		 * deferred for resolution later
		 *  
		 * @author Graeme Rocher
		 * @since 0.4
		 *
		 */
		private class WrappedPropertyValue extends GroovyObjectSupport {
			private Object propertyValue;
			private String propertyName;
			public WrappedPropertyValue(String propertyName, Object propertyValue) {
				this.propertyValue = propertyValue;
				this.propertyName = propertyName;
			}

			public void leftShift(Object value) {
				InvokerHelper.invokeMethod(propertyValue, "leftShift", value);
				if(value instanceof RuntimeBeanReference) {
					deferredProperties.put(beanConfig.getName(), new DeferredProperty(beanConfig, propertyName, propertyValue));				
				}
			}			
		}
		public Object invokeMethod(String name, Object args) {
			return this.metaClass.invokeMethod(this, name, args);
		}

		public void setMetaClass(MetaClass metaClass) {
			this.metaClass = metaClass;
		}

		public void setProperty(String property, Object newValue) {
            if(!addToDeferred(beanConfig,property, newValue)) {
                beanConfig.setPropertyValue(property, newValue);
            }
		}
	}	
	/**
	 * Takes a resource pattern as (@see org.springframework.core.io.support.PathMatchingResourcePatternResolver)
	 * This allows you load multiple bean resources in this single builder
	 * 
	 * eg loadBeans("classpath:*Beans.groovy")
	 *  
	 * @param resourcePattern
	 * @throws IOException When the path cannot be matched
	 */	
	public void loadBeans(String resourcePattern) throws IOException {
		loadBeans(new PathMatchingResourcePatternResolver().getResources(resourcePattern));
	}
	
	/**
	 * Loads a single Resource into the bean builder
	 * 
	 * @param resource The resource to load
	 * @throws IOException When an error occurs
	 */
	public void loadBeans(Resource resource) throws IOException {
		loadBeans(new Resource[]{resource});
	}	
	
	/**
	 * Loads a set of given beans
	 * @param resources The resources to load
	 * @throws IOException Thrown if there is an error reading one of the passes resources
	 */
	public void loadBeans(Resource[] resources) throws IOException {
		Closure beans = new Closure(this){
			public Object call(Object[] args) {
				invokeBeanDefiningClosure((Closure)args[0]);
				return null;
			}			
		};
		Binding b = new Binding();
		b.setVariable("beans", beans);
		
		GroovyShell shell = classLoader != null ? new GroovyShell(classLoader,b) : new GroovyShell(b);
		for (int i = 0; i < resources.length; i++) {
			Resource resource = resources[i];
			shell.evaluate(resource.getInputStream());
		}
	}

    /**
     * Register a set of beans with the given
     * @param ctx  The GenericApplicationContext instance
     */
    public void registerBeans(GenericApplicationContext ctx) {
            finalizeDeferredProperties();
            ctx.setClassLoader(this.classLoader);
            ctx.getBeanFactory().setBeanClassLoader(this.classLoader);
            springConfig.registerBeansWithContext(ctx);        
    }

    /**
	 * This method overrides method invocation to create beans for each method name that
	 * takes a class argument
	 */
	public Object invokeMethod(String name, Object arg) {
        Object[] args = (Object[])arg;
        
        if(CREATE_APPCTX.equals(name)) {
            return createApplicationContext();
		}
        else if(REGISTER_BEANS.equals(name) && args.length == 1 && args[0] instanceof GenericApplicationContext) {
            registerBeans((GenericApplicationContext)args[0]);
            return null;
        }
        else if(BEANS.equals(name) && args.length == 1 && args[0] instanceof Closure) {
            return beans((Closure)args[0]);
        }
		
		if(REF.equals(name)) {
			String refName;
			if(args[0] == null)
				throw new IllegalArgumentException("Argument to ref() is not a valid bean or was not found");
			
			if(args[0] instanceof RuntimeBeanReference) {
				refName = ((RuntimeBeanReference)args[0]).getBeanName();
			}
			else {
				refName = args[0].toString();	
			}
			
			boolean parentRef = false;
			if(args.length > 1) {
				if(args[1] instanceof Boolean) {
					parentRef = ((Boolean)args[1]).booleanValue();
				}
			}
			return new RuntimeBeanReference(refName, parentRef);
		}
		
		if(args.length > 0 && args[0] instanceof Closure) {
            // abstract bean definition
            return invokeBeanDefiningMethod(name, args);
		}
		else if(args.length > 0 && args[0] instanceof Class || args.length > 0 && args[0] instanceof RuntimeBeanReference || args.length > 0 &&args[0] instanceof Map) {
			return invokeBeanDefiningMethod(name, args);			
		}
		else if (args.length > 1 && args[args.length -1] instanceof Closure) {
			return invokeBeanDefiningMethod(name, args);
		}
        ApplicationContext ctx = springConfig.getUnrefreshedApplicationContext();
        MetaClass mc = DefaultGroovyMethods.getMetaClass(ctx);
        if(!mc.respondsTo(ctx, name, args).isEmpty()){
            return mc.invokeMethod(ctx,name, args);
        }
        
        return this;
	}


    /**
     * Defines a set of beans for the given block or closure.
     *
     * @param c The block or closure
     * @return This BeanBuilder instance
     */
    public BeanBuilder beans(Closure c) {
        return invokeBeanDefiningClosure(c);
    }

    /**
     * Creates an ApplicationContext from the current state of the BeanBuilder
     * @return The ApplicationContext instance
     */
    public ApplicationContext createApplicationContext() {
        finalizeDeferredProperties();
        return springConfig.getApplicationContext();
    }

    private void finalizeDeferredProperties() {
		for (Iterator i = deferredProperties.values().iterator(); i.hasNext();) {
			DeferredProperty dp = (DeferredProperty) i.next();
			
			if(dp.value instanceof List) {
				dp.value = manageListIfNecessary(dp.value);
			}
			else if(dp.value instanceof Map) {
				dp.value = manageMapIfNecessary(dp.value);
			}
			dp.setInBeanConfig();
		}
		deferredProperties.clear();
	}
	
	private boolean addToDeferred(BeanConfiguration beanConfig,String property, Object newValue) {
		if(newValue instanceof List) {
			deferredProperties.put(currentBeanConfig.getName()+property,new DeferredProperty(currentBeanConfig, property, newValue));
			return true;
		}
		else if(newValue instanceof Map) {
			deferredProperties.put(currentBeanConfig.getName()+property,new DeferredProperty(currentBeanConfig, property, newValue));
			return true;
		}
		return false;
	}
	/**
	 * This method is called when a bean definition node is called
	 * 
	 * @param name The name of the bean to define
	 * @param args The arguments to the bean. The first argument is the class name, the last argument is sometimes a closure. All
	 * the arguments in between are constructor arguments
	 * @return The bean configuration instance
	 */
	private BeanConfiguration invokeBeanDefiningMethod(String name, Object[] args) {

		if(args[0] instanceof Class) {
			Class beanClass = args[0] instanceof Class ? (Class)args[0] : args[0].getClass();
			
			if(args.length >= 1) {
				if(args[args.length-1] instanceof Closure) {
					if(args.length-1 != 1) {
						Object[] constructorArgs = subarray(args, 1, args.length-1);
                        filterGStringReferences(constructorArgs);
                        currentBeanConfig = springConfig.addSingletonBean(name, beanClass, Arrays.asList(constructorArgs));
					}
					else {
						currentBeanConfig = springConfig.addSingletonBean(name, beanClass);
					}				
				}
				else  {
					Object[] constructorArgs = subarray(args, 1, args.length);
                    filterGStringReferences(constructorArgs);
                    currentBeanConfig = springConfig.addSingletonBean(name, beanClass, Arrays.asList(constructorArgs));
				}

			}			
		}
		else if(args[0] instanceof RuntimeBeanReference) {
			currentBeanConfig = springConfig.addSingletonBean(name);
			currentBeanConfig.setFactoryBean(((RuntimeBeanReference)args[0]).getBeanName());
		}
		else if(args[0] instanceof Map) {
			currentBeanConfig = springConfig.addSingletonBean(name);
			Map.Entry factoryBeanEntry = (Map.Entry)((Map)args[0]).entrySet().iterator().next();
			currentBeanConfig.setFactoryBean(factoryBeanEntry.getKey().toString());
			currentBeanConfig.setFactoryMethod(factoryBeanEntry.getValue().toString());
		}
        else if(args[0] instanceof Closure) {
            currentBeanConfig = springConfig.addAbstractBean(name);
        }
        else {
            Object[] constructorArgs;
            if(args[args.length-1] instanceof Closure) {
                constructorArgs= subarray(args, 0, args.length-1);
            }
            else {
                constructorArgs= subarray(args, 0, args.length);
            }
            filterGStringReferences(constructorArgs);
            currentBeanConfig = new DefaultBeanConfiguration(name, null, Arrays.asList(constructorArgs));
            springConfig.addBeanConfiguration(name,currentBeanConfig);
        }
        if(args[args.length-1] instanceof Closure) {
			Closure callable = (Closure)args[args.length-1];
			callable.setDelegate(this);
            callable.setResolveStrategy(Closure.DELEGATE_FIRST);
            callable.call(new Object[]{currentBeanConfig});
							
		}

		return currentBeanConfig;
	}

    private Object[] subarray(Object[] args, int i, int j) {
        if(j > args.length) throw new IllegalArgumentException("Upper bound can't be greater than array length");
        Object[] b = new Object[j-i];
        int n = 0;
        for (int k = i;  k < j; k++,n++) {
            b[n] = args[k];
            
        }
        return b;
    }

    private void filterGStringReferences(Object[] constructorArgs) {
        for (int i = 0; i < constructorArgs.length; i++) {
            Object constructorArg = constructorArgs[i];
            if(constructorArg instanceof GString) constructorArgs[i] = constructorArg.toString();
        }
    }

    /**
	 * When an methods argument is only a closure it is a set of bean definitions
	 * 
	 * @param callable The closure argument
     * @return This BeanBuilder instance
	 */
	private BeanBuilder invokeBeanDefiningClosure(Closure callable) {

		callable.setDelegate(this);
  //      callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.call();
		finalizeDeferredProperties();

        return this;
    }

	/**
	 * This method overrides property setting in the scope of the BeanBuilder to set
	 * properties on the current BeanConfiguration
	 */
	public void setProperty(String name, Object value) {
		if(currentBeanConfig != null) {
			if(value instanceof GString)value = value.toString();
			if(addToDeferred(currentBeanConfig, name, value)) {
				return;
			}
			else if(value instanceof Closure) {
				BeanConfiguration current = currentBeanConfig;
				try {
					Closure callable = (Closure)value;

					Class parameterType = callable.getParameterTypes()[0];
					if(parameterType.equals(Object.class)) {
						currentBeanConfig = springConfig.createSingletonBean("");
						callable.call(new Object[]{currentBeanConfig});
					}
					else {
						currentBeanConfig = springConfig.createSingletonBean(parameterType);
						callable.call(null);
					}					

					value = currentBeanConfig.getBeanDefinition();
				}
				finally {
					currentBeanConfig = current;
				}
			}
			currentBeanConfig.addProperty(name, value);
		}		
	}

	/**
	 * Checks whether there are any runtime refs inside a Map and converts
	 * it to a ManagedMap if necessary
	 * 
	 * @param value The current map
	 * @return A ManagedMap or a normal map
	 */
	private Object manageMapIfNecessary(Object value) {
		Map map = (Map)value;
		boolean containsRuntimeRefs = false;
		for (Iterator i = map.values().iterator(); i.hasNext();) {
			Object e = i.next();
			if(e instanceof RuntimeBeanReference) {
				containsRuntimeRefs = true;
				break;
			}						
		}
		if(containsRuntimeRefs) {
            Map managedMap = new ManagedMap();
            managedMap.putAll(map);
            return managedMap; 
		}
		return value;
	}

	/**
	 * Checks whether there are any runtime refs inside the list and 
	 * converts it to a ManagedList if necessary
	 * 
	 * @param value The object that represents the list
	 * @return Either a new list or a managed one
	 */
	private Object manageListIfNecessary(Object value) {
		List list = (List)value;
		boolean containsRuntimeRefs = false;
		for (Iterator i = list.iterator(); i.hasNext();) {
			Object e = i.next();
			if(e instanceof RuntimeBeanReference) {
				containsRuntimeRefs = true;
				break;
			}						
		}
		if(containsRuntimeRefs) {
			List tmp = new ManagedList();
			tmp.addAll((List)value);
			value = tmp;					
		}
		return value;
	}

	/**
	 * This method overrides property retrieval in the scope of the BeanBuilder to either:
	 * 
	 * a) Retrieve a variable from the bean builder's binding if it exists
	 * b) Retrieve a RuntimeBeanReference for a specific bean if it exists
	 * c) Otherwise just delegate to super.getProperty which will resolve properties from the BeanBuilder itself
	 */
	public Object getProperty(String name) {
		if(binding.containsKey(name)) {
			return binding.get(name);
		}
		else {
			if(springConfig.containsBean(name)) {
				BeanConfiguration beanConfig = springConfig.getBeanConfig(name);
				if(beanConfig != null) {
					return new ConfigurableRuntimeBeanReference(name, springConfig.getBeanConfig(name) ,false);
				}					
				else
					return new RuntimeBeanReference(name,false);
			}
			// this is to deal with the case where the property setter is the last
			// statement in a closure (hence the return value)
			else if(currentBeanConfig != null) {
				if(currentBeanConfig.hasProperty(name))
					return currentBeanConfig.getPropertyValue(name);
				else {
					DeferredProperty dp = (DeferredProperty)deferredProperties.get(currentBeanConfig.getName()+name);
					if(dp!=null) {
						return dp.value;
					}
					else {
						return super.getProperty(name);
					}
				}
			}
			else {
				return super.getProperty(name);
			}			
		}			
	}

	/**
	 * Sets the binding (the variables available in the scope of the BeanBuilder)
	 * @param b The Binding instance
	 */
	public void setBinding(Binding b) {
		this.binding = b.getVariables();
	}

}