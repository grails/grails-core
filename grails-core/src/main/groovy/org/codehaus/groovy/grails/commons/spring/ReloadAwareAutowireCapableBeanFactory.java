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
import grails.util.GrailsUtil;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.compiler.GrailsClassLoader;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.util.ClassUtils;

/**
 * A BeanFactory that can deal with class cast exceptions that may occur due to
 * class reload events and then attempt to reload the bean being instantiated to
 * avoid them.
 * 
 * Caches autowiring for beans (mainly controllers & domain class instances).
 * Bypasses autowiring if there are no beans for the properties in the class.
 * Caching is only used in environments where reloading is not enabled.
 * 
 * @author Graeme Rocher
 * @since 1.1.1
 */
public class ReloadAwareAutowireCapableBeanFactory extends
		DefaultListableBeanFactory {
	ConcurrentMap<Class<?>, Set<String>> autowiringByNameCacheForClass = new ConcurrentHashMap<Class<?>, Set<String>>();
	Set<String> dependenciesRegisteredOnce = new ConcurrentSkipListSet<String>();
	
	private boolean reloadEnabled;

	/**
	 * Default constructor.
	 */
	public ReloadAwareAutowireCapableBeanFactory() {
		reloadEnabled = GrailsUtil.isDevelopmentEnv() || Environment.getCurrent().isReloadEnabled();
		if (reloadEnabled) {

			// Implementation note: The default Spring InstantiationStrategy
			// caches constructors.
			// This is no good at development time because if the class reloads
			// then Spring
			// continues to use the old class. We deal with this here by
			// disabling the caching
			// for development time only
			setInstantiationStrategy(new CglibSubclassingInstantiationStrategy() {
				@Override
				public Object instantiate(RootBeanDefinition beanDefinition,
						String beanName, BeanFactory owner) {
					// Don't override the class with CGLIB if no overrides.
					if (beanDefinition.getMethodOverrides().isEmpty()) {
						Constructor<?> constructorToUse;
						Class<?> clazz = beanDefinition.getBeanClass();
						if (clazz.isInterface()) {
							throw new BeanInstantiationException(clazz,
									"Specified class is an interface");
						}
						try {
							constructorToUse = clazz
									.getDeclaredConstructor((Class[]) null);
						} catch (Exception ex) {
							throw new BeanInstantiationException(clazz,
									"No default constructor found", ex);
						}

						return BeanUtils.instantiateClass(constructorToUse);
					}
					// Must generate CGLIB subclass.
					return instantiateWithMethodInjection(beanDefinition,
							beanName, owner);
				}
			});
		}

		setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());
		setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());
		ignoreDependencyType(Closure.class);
	}

	@Override
	protected Object doCreateBean(String beanName, RootBeanDefinition mbd,
			Object[] args) {
		if (!reloadEnabled) {
			return super.doCreateBean(beanName, mbd, args);
		}

		try {
			return super.doCreateBean(beanName, mbd, args);
		} catch (BeanCreationException t) {
			if (t.getCause() instanceof TypeMismatchException) {
				Object bean = handleTypeMismatchException(beanName, mbd, args);
				if (bean != null) {
					return bean;
				}
			}
			throw t;
		}
	}

	private Object handleTypeMismatchException(String beanName,
			RootBeanDefinition mbd, Object[] args) {
		// type mismatch probably occured because another class was reloaded
		final Class<?> beanClass = mbd.getBeanClass();
		if (!GroovyObject.class.isAssignableFrom(beanClass)) {
			return null;
		}

		GrailsApplication application = (GrailsApplication) getBean(GrailsApplication.APPLICATION_ID);
		ClassLoader classLoader = application.getClassLoader();
		if (!(classLoader instanceof GrailsClassLoader)) {
			return null;
		}

		GrailsClassLoader gcl = (GrailsClassLoader) classLoader;
		gcl.reloadClass(beanClass.getName());
		Class<?> newBeanClass;
		try {
			newBeanClass = gcl.loadClass(beanClass.getName());
		} catch (ClassNotFoundException e) {
			return null;
		}

		mbd.setBeanClass(newBeanClass);
		if (newBeanClass.equals(beanClass)) {
			return null;
		}

		GrailsPluginManager pluginManager = (GrailsPluginManager) getBean(GrailsPluginManager.BEAN_NAME);
		pluginManager.informOfClassChange(newBeanClass);
		return super.doCreateBean(beanName, mbd, args);
	}

	@Override
	protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
		// exclude properties generated by the groovy compiler from autowiring
		// checks
		return pd.getName().indexOf('$') > -1 || super.isExcludedFromDependencyCheck(pd);
	}

	@Override
	public void autowireBeanProperties(Object existingBean, int autowireMode,
			boolean dependencyCheck) throws BeansException {
		if (!reloadEnabled && autowireMode == AUTOWIRE_BY_NAME) {
			Set<String> beanProps = autowiringByNameCacheForClass
					.get(ClassUtils.getUserClass(existingBean.getClass()));
			if (beanProps != null && beanProps.isEmpty()) {
				// nothing to autowire
				// doesn't take instance based beanpostprocessors in to account				
				if (logger.isDebugEnabled()) {
					logger.debug("Nothing to autowire for bean of class "
							+ existingBean.getClass().getName());
				}
				return;
			}
		}
		
		if(autowireMode == AUTOWIRE_BY_NAME) {
			RootBeanDefinition bd =
					new RootBeanDefinition(ClassUtils.getUserClass(existingBean), autowireMode, dependencyCheck);
			bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
			bd.setSynthetic(true);
			// use optimized method for autowiring by name, don't register any editors for BeanWrapperImpl
			BeanWrapperImpl bw = new BeanWrapperImpl(false);
			bw.setWrappedInstance(existingBean);
			bw.setConversionService(getConversionService());
			populateBean(bd.getBeanClass().getName(), bd, bw);			
		} else {
			super.autowireBeanProperties(existingBean, autowireMode,
					dependencyCheck);
		}
	}

	@Override
	protected void autowireByName(String beanName, AbstractBeanDefinition mbd,
			BeanWrapper bw, MutablePropertyValues pvs) {
		if (reloadEnabled) {
			super.autowireByName(beanName, mbd, bw, pvs);
			return;
		}

		// caching for autowired bean properties

		Class<?> beanClass = ClassUtils.getUserClass(bw.getWrappedInstance().getClass());
		// list of bean properties for that a bean exists
		Set<String> beanProps = autowiringByNameCacheForClass.get(beanClass);
		if (beanProps == null) {
			beanProps = new LinkedHashSet<String>();
			String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
			for (String propertyName : propertyNames) {
				if (containsBean(propertyName)) {
					beanProps.add(propertyName);
				}
			}
			autowiringByNameCacheForClass.put(beanClass, beanProps);
		}
		for (String propertyName : beanProps) {
			Object bean = getBean(propertyName);
			pvs.addPropertyValue(propertyName, bean);
			if(!dependenciesRegisteredOnce.contains(beanName)) {
				// possible concurrency problem here
				registerDependentBean(propertyName, beanName);
				dependenciesRegisteredOnce.add(beanName);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Added autowiring by name from bean name '"
						+ beanName + "' via property '" + propertyName
						+ "' to bean named '" + propertyName + "'");
			}
		}
	}
}
