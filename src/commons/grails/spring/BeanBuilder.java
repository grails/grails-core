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

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.groovy.grails.commons.spring.BeanConfiguration;
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;

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
	private static final String CREATE_APPCTX = "createApplicationContext";
	private static final Object APPLICATION = "application";
	private RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
	private BeanConfiguration currentBeanConfig;
	private Object application;

	
	public BeanBuilder() {
		super();
	}
	
	

	public RuntimeSpringConfiguration getSpringConfig() {
		return springConfig;
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
	 * A constructor that takes a resource pattern as (@see org.springframework.core.io.support.PathMatchingResourcePatternResolver)
	 * This allows you load multiple bean resources in this single builder
	 * 
	 * eg new BeanBuilder("classpath:*Beans.groovy")
	 *  
	 * @param resourcePattern
	 * @throws IOException When the path cannot be matched
	 */
	public BeanBuilder(String resourcePattern) throws IOException {
		loadBeans(resourcePattern);
	}


	public BeanBuilder(Resource resource) throws IOException {
		loadBeans(resource);
	}
	
	public BeanBuilder(Resource[] resources) throws IOException {
		loadBeans(resources);
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
	 * @throws IOException 
	 */
	public void loadBeans(Resource[] resources) throws IOException {
		Closure beans = new Closure(this){
			public Object call(Object[] args) {
				invokeBeanDefiningClosure(args[0]);
				return null;
			}			
		};
		Binding b = new Binding();
		b.setVariable("beans", beans);
		
		GroovyShell shell = new GroovyShell(b);
		for (int i = 0; i < resources.length; i++) {
			Resource resource = resources[i];
			shell.evaluate(resource.getInputStream());
		}
	}



	public Object invokeMethod(String name, Object arg) {
		if(CREATE_APPCTX.equals(name)) {
			return springConfig.getApplicationContext();
		}
		
		Object[] args = (Object[])arg;
		if(args.length == 0) 
			throw new MissingMethodException(name, getClass(),args);
		
		
		if(args[0] instanceof Closure) {
			invokeBeanDefiningClosure(args[0]);
		}
		else if(args[0] instanceof Class) {
			return invokeBeanDefiningMethod(name, args);			
		}
		else {
			return super.invokeMethod(name,arg);
		}
		return this;
	}

	/**
	 * This method is called when a bean definition node is called
	 * 
	 * @param name The name of the bean to define
	 * @param arg The arguments to the bean. The first argument is the class name, the last argument is sometimes a closure. All
	 * the arguments in between are constructor arguments
	 * @return The bean configuration instance
	 */
	private BeanConfiguration invokeBeanDefiningMethod(String name, Object[] args) {
		if(!(args[0] instanceof Class))
			throw new IllegalArgumentException("Argument types to bean defining methods must be a java.lang.Class and a closure for call ["+name+"] and args ["+ArrayUtils.toString(args)+"]");

		Class beanClass = args[0] instanceof Class ? (Class)args[0] : args[0].getClass();
				
		if(args.length > 1) {
			
			if(args.length-1 != 1) {
				Object[] constructorArgs = ArrayUtils.subarray(args, 1, args.length-1);				
				currentBeanConfig = springConfig.addSingletonBean(name, beanClass, Arrays.asList(constructorArgs));
			}
			else {
				currentBeanConfig = springConfig.addSingletonBean(name, beanClass);
			}
			if(args[args.length-1] instanceof Closure) {
				Closure callable = (Closure)args[args.length-1];
				callable.setDelegate(this);
				callable.call();				
			}
		}
		return currentBeanConfig;
	}

	/**
	 * When an methods argument is only a closure it is a set of bean definitions
	 * 
	 * @param arg The closure argument
	 */
	private void invokeBeanDefiningClosure(Object arg) {
		Closure callable = (Closure)arg;
		callable.setDelegate(this);
		callable.call();
	}

	public void setProperty(String name, Object value) {
		if(currentBeanConfig != null) {
			if(value instanceof GString)value = value.toString();
			if(value instanceof List) {
				value = manageListIfNecessary(value);
			}
			else if(value instanceof Map) {
				value = manageMapIfNecessary(value);
			}
			else if(value instanceof Closure) {
				BeanConfiguration current = currentBeanConfig;
				try {
					Closure callable = (Closure)value;

					currentBeanConfig = springConfig.createSingletonBean(callable.getParameterTypes()[0]);
					callable.call(null);
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
			return new ManagedMap(map);
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

	public Object getProperty(String name) {
		if(name.equals(APPLICATION) && application != null) {
			return this.application;
		}
		else {
			return new RuntimeBeanReference(name, false);
		}			
	}

	public void setApplication(Object application) {
		this.application = application;
	}	
}