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

import javax.servlet.ServletContext;

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
public interface RuntimeSpringConfiguration {

	/**
	 * Adds a singleton bean definition
	 * 
	 * @param name The name of the bean
	 * @param clazz The class of the bean
	 * @return A BeanConfiguration instance
	 */
	public BeanConfiguration addSingletonBean(String name, Class clazz);
	/**
	 * Adds a prototype bean definition
	 * 
	 * @param name The name of the bean
	 * @param clazz The class of the bean
	 * @return A BeanConfiguration instance
	 */
	public BeanConfiguration addPrototypeBean(String name, Class clazz);
	
	/**
	 * Retrieves the application context from the current state
	 * 
	 * @return The ApplicationContext instance
	 */
	WebApplicationContext getApplicationContext();
	
	/**
	 * Adds an empty singleton bean configuration
	 * @param The name of the singleton bean
	 * 
	 * @return A BeanConfiguration instance
	 */
	public BeanConfiguration addSingletonBean(String name);
	
	/**
	 * Adds an empty prototype bean configuration
	 * 
	 * @param name The name of the prototype bean
	 * @return A BeanConfiguration instance
	 */
	public BeanConfiguration addPrototypeBean(String name);
	
	/**
	 * Creates a singleton bean configuration. Differs from addSingletonBean in that
	 * it doesn't add the bean to the list of bean references. Hence should be used for
	 * creating nested beans
	 * 
	 * @param clazz
	 * @return A BeanConfiguration instance
	 */
	public BeanConfiguration createSingletonBean(Class clazz);
	
	/**
	 * Creates a new singleton bean and adds it to the list of bean references
	 * 
	 * @param name The name of the bean
	 * @param clazz The class of the bean 
	 * @param args The constructor arguments of the bean
	 * @return A BeanConfiguration instance
	 */
	public BeanConfiguration addSingletonBean(String name, Class clazz, Collection args);
	
	/**
	 * Creates a singleton bean configuration. Differs from addSingletonBean in that
	 * it doesn't add the bean to the list of bean references. Hence should be used for
	 * creating nested beans
	 * 
	 * @param clazz The bean class
	 * @param constructorArguments The constructor arguments
	 * @return A BeanConfiguration instance
	 */	
	public BeanConfiguration createSingletonBean(Class class1, Collection constructorArguments);
	
	/**
	 * Sets the servlet context
	 * 
	 * @param context The servlet Context
	 */
	public void setServletContext(ServletContext context);
	
}
