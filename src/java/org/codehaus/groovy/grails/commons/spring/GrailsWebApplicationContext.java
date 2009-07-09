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

import grails.spring.BeanBuilder;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.IOException;

/**
 * A WebApplicationContext that extends StaticApplicationContext to allow for programmatic
 * configuration at runtime. The code is adapted from StaticWebApplicationContext.
 * 
 * @author Graeme
 * @since 0.3
 *
 */
public class GrailsWebApplicationContext extends GrailsApplicationContext
        implements ConfigurableWebApplicationContext, GroovyObject, ThemeSource {

	private ServletContext servletContext;
	private String namespace;
    private ServletConfig servletConfig;
    private String[] configLocations = new String[0];

    public GrailsWebApplicationContext() throws BeansException {
		super();
	}

	public GrailsWebApplicationContext(ApplicationContext parent) throws BeansException {
		super(parent);
	}

    public GrailsWebApplicationContext(DefaultListableBeanFactory defaultListableBeanFactory) {
        super(defaultListableBeanFactory);
    }

    public GrailsWebApplicationContext(DefaultListableBeanFactory defaultListableBeanFactory, ApplicationContext parent) {
        super(defaultListableBeanFactory, parent);
    }

    public ClassLoader getClassLoader() {
        ApplicationContext parent = getParent();
        if(parent != null && parent.containsBean(GrailsApplication.APPLICATION_ID)) {
            final GrailsApplication application = (GrailsApplication) parent.getBean(GrailsApplication.APPLICATION_ID);
            return application.getClassLoader();
        }
        else
            return super.getClassLoader();
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

	public String getNamespace() {
		return namespace;
	}

    public void setConfigLocation(String configLocation) {
        Assert.notNull(configLocation, "Argument [configLocation] cannot be null");
        this.configLocations = new String[] { configLocation };
    }

    public void setConfigLocations(String[] configLocations) {
        Assert.notNull(configLocations, "Argument [configLocations] cannot be null");
        this.configLocations = configLocations;
	}

    public String[] getConfigLocations() {
        return configLocations;
    }



    /**
	 * Register ServletContextAwareProcessor.
	 * @see ServletContextAwareProcessor
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
        beanFactory.ignoreDependencyInterface(ServletContextAware.class);
        beanFactory.registerResolvableDependency(ServletContext.class, this.servletContext);

        WebApplicationContextUtils.registerWebApplicationScopes(beanFactory);
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

    public void refresh() throws BeansException, IllegalStateException {
        super.refresh();
    }

    @Override
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        if(configLocations.length >0) {
            for (String configLocation : configLocations) {
                BeanBuilder beanBuilder = new BeanBuilder(getParent(),getClassLoader());
                final ServletContextResource resource = new ServletContextResource(getServletContext(), configLocation);
                try {
                    beanBuilder.loadBeans(resource);
                    beanBuilder.registerBeans(this);
                }
                catch (IOException e) {
                    throw new BeanDefinitionParsingException(new Problem("Error loading beans from ["+resource+"]: " + e.getMessage(),new Location(resource),null, e));
                }
            }
        }
        super.prepareBeanFactory(beanFactory);
    }

    public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
	}

    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }

}
