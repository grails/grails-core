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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
/**
 * A programmable runtime Spring configuration that allows a spring ApplicationContext
 * to be constructed at runtime 
 * 
 * Credit must go to Solomon Duskis and the
 * article: http://jroller.com/page/Solomon?entry=programmatic_configuration_in_spring
 * 
 * @author Graeme
 * @since 0.3
 *
 */
public class DefaultRuntimeSpringConfiguration implements
		RuntimeSpringConfiguration {

	private GrailsWebApplicationContext context;
	private List beanConfigs = new ArrayList();

	public DefaultRuntimeSpringConfiguration() {
		super();
		this.context = new GrailsWebApplicationContext();
	}
	
	public DefaultRuntimeSpringConfiguration(ApplicationContext parent) {
		super();
		this.context = new GrailsWebApplicationContext(parent);
	}	

	public BeanConfiguration addSingletonBean(String name, Class clazz) {
		BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz);
		beanConfigs.add(bc);
		return bc;
	}

	public BeanConfiguration addPrototypeBean(String name, Class clazz) {
		BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz,true);
		beanConfigs.add(bc);
		return bc;
	}

	public WebApplicationContext getApplicationContext() {
		for (Iterator i = beanConfigs.iterator(); i.hasNext();) {
			BeanConfiguration bc = (BeanConfiguration) i.next();
			
			context.registerBeanDefinition(bc.getName(),
												bc.getBeanDefinition()	);
		}
		
		context.refresh();
		return context;
	}

	public BeanConfiguration addSingletonBean(String name) {
		BeanConfiguration bc = new DefaultBeanConfiguration(name);
		beanConfigs.add(bc);
		return bc;
	}

	public BeanConfiguration createSingletonBean(Class clazz) {
        return new DefaultBeanConfiguration(clazz);
	}

	public BeanConfiguration addSingletonBean(String name, Class clazz, Collection args) {
		BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz,args);
		beanConfigs.add(bc);
		return bc;
	}

	public BeanConfiguration addPrototypeBean(String name) {
		BeanConfiguration bc = new DefaultBeanConfiguration(name,true);
		beanConfigs.add(bc);
		return bc;
	}

	public BeanConfiguration createSingletonBean(Class clazz, Collection constructorArguments) {
        return new DefaultBeanConfiguration(clazz, constructorArguments);
	}

	public void setServletContext(ServletContext context) {
		this.context.setServletContext(context);
	}

	public BeanConfiguration createPrototypeBean(String name) {
		return new DefaultBeanConfiguration(name,true);
	}

	public BeanConfiguration createSingletonBean(String name) {
		return new DefaultBeanConfiguration(name);
	}

    public void addBeanConfiguration(String beanName, BeanConfiguration beanConfiguration) {
        beanConfiguration.setName(beanName);
        beanConfigs.add(beanConfiguration);
    }

}
