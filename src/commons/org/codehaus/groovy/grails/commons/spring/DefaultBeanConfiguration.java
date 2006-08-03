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
package org.codehaus.groovy.grails.commons.spring;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Default implementation of the BeanConfiguration interface 
 * 
 * Credit must go to Solomon Duskis and the
 * article: http://jroller.com/page/Solomon?entry=programmatic_configuration_in_spring
 * 
 * @author Graeme
 * @since 0.3
 *
 */
public class DefaultBeanConfiguration implements BeanConfiguration {

	private Class clazz;
	private String name;
	private boolean singleton = true;
	private AbstractBeanDefinition definition;
	private Collection constructorArgs = Collections.EMPTY_LIST;

	public DefaultBeanConfiguration(String name, Class clazz) {
		this.name = name;
		this.clazz = clazz;
	}
	
	public DefaultBeanConfiguration(String name, Class clazz, boolean prototype) {
		this.name = name;
		this.clazz = clazz;
		this.singleton = !prototype;
	}	

	public DefaultBeanConfiguration(String name) {
		this.name= name;
	}

	public DefaultBeanConfiguration(Class clazz2) {
		this.clazz = clazz2;
	}

	public DefaultBeanConfiguration(String name2, Class clazz2, Collection args) {
		this.name = name2;
		this.clazz = clazz2;
		this.constructorArgs = args;
	}

	public DefaultBeanConfiguration(String name2, boolean prototype) {
		this.name = name2;
		this.singleton = !prototype;
	}

	public DefaultBeanConfiguration(Class clazz2, Collection constructorArguments) {
		this.clazz = clazz2;
		this.constructorArgs = constructorArguments;
	}

	public String getName() {
		return this.name;
	}

	public boolean isSingleton() {
		return this.singleton ;
	}

	public AbstractBeanDefinition getBeanDefinition() {
		if (definition == null)
			definition = createBeanDefinition();
		return definition;
	}

	protected AbstractBeanDefinition createBeanDefinition() {
		if(constructorArgs.size() > 0) {
			ConstructorArgumentValues cav = new ConstructorArgumentValues();
			for (Iterator i = constructorArgs.iterator(); i.hasNext();) {
				cav.addGenericArgumentValue(i.next());
			}
			AbstractBeanDefinition bd = new RootBeanDefinition(clazz,cav,null);
			bd.setSingleton(singleton);
			return bd;
		}
		else {
			return new RootBeanDefinition(clazz,singleton);
		}
	}
	
	public BeanConfiguration addProperty(String propertyName, Object propertyValue) {
		if(propertyValue instanceof BeanConfiguration) {
			propertyValue = ((BeanConfiguration)propertyValue).getBeanDefinition();
		}
		getBeanDefinition()
			.getPropertyValues()
			.addPropertyValue(propertyName,propertyValue);
				
		return this;
	}

	public BeanConfiguration setDestroyMethod(String methodName) {
		getBeanDefinition().setDestroyMethodName(methodName);
		return this;
	}

	public BeanConfiguration setDependsOn(String[] dependsOn) {
		getBeanDefinition().setDependsOn(dependsOn);
		return this;		
	}

	public BeanConfiguration setFactoryBean(String beanName) {
		getBeanDefinition().setFactoryBeanName(beanName);
		
		return this;
	}

	public BeanConfiguration setFactoryMethod(String methodName) {
		getBeanDefinition().setFactoryMethodName(methodName);
		return this;
	}

	public BeanConfiguration setAutowire(String type) {
		if("byName".equals(type)) {
			getBeanDefinition().setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
		}
		else if("byType".equals(type)){
			getBeanDefinition().setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		}
		return this;
	}

    public void setName(String beanName) {
        this.name = beanName;
    }

}
