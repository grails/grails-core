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

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

/**
 * A WebApplicationContext that extends StaticApplicationContext to allow for programmatic
 * configuration at runtime. The code is adapted from StaticWebApplicationContext.
 * 
 * @author Graeme
 * @since 0.3
 *
 */
public class GrailsWebApplicationContext extends StaticApplicationContext
		implements ConfigurableWebApplicationContext {

	private ServletContext servletContext;
	private String namespace;
	private ThemeSource themeSource;

	public GrailsWebApplicationContext() throws BeansException {
		super();
	}

	public GrailsWebApplicationContext(ApplicationContext parent) throws BeansException {
		super(parent);
	}

	/**
	 * Set the ServletContext that this WebApplicationContext runs in.
	 */
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
		if (namespace != null) {
			setDisplayName("WebApplicationContext for namespace '" + namespace + "'");
		}
	}

	protected String getNamespace() {
		return this.namespace;
	}

	public void setConfigLocations(String[] configLocations) {
		throw new UnsupportedOperationException("StaticWebApplicationContext does not support configLocations");
	}


	/**
	 * Register ServletContextAwareProcessor.
	 * @see ServletContextAwareProcessor
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
	}

	/**
	 * This implementation supports file paths beneath the root of the ServletContext.
	 * @see ServletContextResource
	 */
	protected Resource getResourceByPath(String path) {
		return new ServletContextResource(this.servletContext, path);
	}

	/**
	 * This implementation supports pattern matching in unexpanded WARs too.
	 * @see ServletContextResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new ServletContextResourcePatternResolver(this);
	}

	/**
	 * Initialize the theme capability.
	 */
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}
}
