/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.commons.spring;

import grails.util.Environment;
import groovy.lang.GroovyObject;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.compiler.GrailsClassLoader;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.springframework.beans.*;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.support.*;
import org.springframework.util.ClassUtils;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

/**
 * A BeanFactory that can deal with class cast exceptions that may occur due to class reload events and then
 * attempt to reload the bean being instantiated to avoid them.
 * 
 * Caches autowiring for beans (mainly controllers & domain class instances). Bypasses autowiring if there are no beans for the properties in the class. 
 * Caching is only used in environments where reloading is not enabled.
 * 
 *
 * @author Graeme Rocher
 * @since 1.1.1
 *        <p/>
 *        Created: May 8, 2009
 */
public class ReloadAwareAutowireCapableBeanFactory extends DefaultListableBeanFactory{
    ConcurrentHashMap<Class, Set<String>> autowiringByNameCacheForClass=new ConcurrentHashMap<Class, Set<String>>();

    public ReloadAwareAutowireCapableBeanFactory() {
        if(Environment.getCurrent().isReloadEnabled()) {

            // Implementation note: The default Spring InstantiationStrategy caches constructors. This is no good at development time
            // because if the class reloads then Spring continues to use the old class. We deal with this here by disabling the caching
            // for development time only
            setInstantiationStrategy(new CglibSubclassingInstantiationStrategy() {
                @Override
                public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {
                    // Don't override the class with CGLIB if no overrides.
                    if (beanDefinition.getMethodOverrides().isEmpty()) {
                        Constructor constructorToUse;
                        Class clazz = beanDefinition.getBeanClass();
                        if (clazz.isInterface()) {
                            throw new BeanInstantiationException(clazz, "Specified class is an interface");
                        }
                        try {
                            constructorToUse = clazz.getDeclaredConstructor((Class[]) null);

                        }
                        catch (Exception ex) {
                            throw new BeanInstantiationException(clazz, "No default constructor found", ex);
                        }

                        return BeanUtils.instantiateClass(constructorToUse, null);
                    }
                    else {
                        // Must generate CGLIB subclass.
                        return instantiateWithMethodInjection(beanDefinition, beanName, owner);
                    }

                }

            });
        }

        setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());
        setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());        
    }

    @Override
    protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {
        if(Environment.getCurrent().isReloadEnabled()) {
            try {
                return super.doCreateBean(beanName, mbd, args);
            }
            catch (BeanCreationException t) {
                if(t.getCause() instanceof TypeMismatchException)  {
                    // type mismatch probably occured because another class was reloaded
                    final Class beanClass = mbd.getBeanClass();
                    if(GroovyObject.class.isAssignableFrom(beanClass)) {
                        GrailsApplication application = (GrailsApplication) getBean(GrailsApplication.APPLICATION_ID);
                        ClassLoader classLoader = application.getClassLoader();
                        if(classLoader instanceof GrailsClassLoader) {
                            GrailsClassLoader gcl = (GrailsClassLoader) classLoader;
                            gcl.reloadClass(beanClass.getName());
                            try {
                                Class  newBeanClass = gcl.loadClass(beanClass.getName());
                                mbd.setBeanClass(newBeanClass);
                                if(!newBeanClass.equals(beanClass)) {
                                    GrailsPluginManager pluginManager = (GrailsPluginManager) getBean(GrailsPluginManager.BEAN_NAME);
                                    pluginManager.informOfClassChange(newBeanClass);
                                    return super.doCreateBean(beanName, mbd, args);
                                }

                            }
                            catch (ClassNotFoundException e) {
                                throw t;
                            }
                        }
                    }
                }
                throw t;
            }
        }
        else {
            return super.doCreateBean(beanName, mbd, args);
        }

    }

    @Override
	protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
    	// exclude properties generated by the groovy compiler from autowiring checks 
		return super.isExcludedFromDependencyCheck(pd) || pd.getName().indexOf('$') > -1;
	}

	ThreadLocal<Boolean> autowiringBeanPropertiesFlag = new ThreadLocal<Boolean>() {
    	protected Boolean initialValue() {
    		return Boolean.FALSE;
    	}
    };
    
    @Override
	public void autowireBeanProperties(Object existingBean, int autowireMode,
			boolean dependencyCheck) throws BeansException {
    	if(autowireMode==AUTOWIRE_BY_NAME) {
	    	try {
	        	autowiringBeanPropertiesFlag.set(Boolean.TRUE);
	        	if(!Environment.getCurrent().isReloadEnabled()) {
		        	Set<String> beanProps=autowiringByNameCacheForClass.get(ClassUtils.getUserClass(existingBean.getClass()));
		        	if(beanProps != null && beanProps.size()==0) {
		        		// nothing to autowire
						if (logger.isDebugEnabled()) {
							logger.debug("Nothing to autowire for bean of class " + existingBean.getClass().getName());
						}
		        		return;
		        	}
	        	}
	    		super.autowireBeanProperties(existingBean, autowireMode, dependencyCheck);
	    	} finally {
	    		autowiringBeanPropertiesFlag.remove();
	    	}
    	} else {
    		super.autowireBeanProperties(existingBean, autowireMode, dependencyCheck);
    	}
	}
    
	@Override
	protected void autowireByName(String beanName, AbstractBeanDefinition mbd,
			BeanWrapper bw, MutablePropertyValues pvs) {
		if(!autowiringBeanPropertiesFlag.get() || Environment.getCurrent().isReloadEnabled()) {
			super.autowireByName(beanName, mbd, bw, pvs);
		} else {
			// caching for autowired bean properties
			
			// list of bean properties for that a bean exists
			Set<String> beanProps=autowiringByNameCacheForClass.get(ClassUtils.getUserClass(bw.getWrappedInstance().getClass()));
			if(beanProps==null) {
				beanProps=new LinkedHashSet<String>();
				String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
				for (String propertyName : propertyNames) {
					if (containsBean(propertyName)) {
						beanProps.add(propertyName);
					}
				}
				autowiringByNameCacheForClass.put(ClassUtils.getUserClass(bw.getWrappedInstance().getClass()), beanProps);
			}
			for(String propertyName : beanProps) {
				Object bean = getBean(propertyName);
				pvs.addPropertyValue(propertyName, bean);
				registerDependentBean(propertyName, beanName);
				if (logger.isDebugEnabled()) {
					logger.debug(
							"Added autowiring by name from bean name '" + beanName + "' via property '" + propertyName +
									"' to bean named '" + propertyName + "'");
				}
			}
		}
	}
}