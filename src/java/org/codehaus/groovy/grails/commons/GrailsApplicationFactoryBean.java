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
package org.codehaus.groovy.grails.commons;

import grails.util.Environment;
import grails.util.Metadata;
import groovy.lang.GroovyClassLoader;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.grails.compiler.injection.GrailsInjectionOperation;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.codehaus.groovy.grails.compiler.GrailsClassLoader;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Factory bean that creates a Grails application object based on Groovy files.
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
 * @author Chanwit Kaewkasi 
 *
 * @since 0.1
 *
 * Created - Jul 2, 2005
 */
public class GrailsApplicationFactoryBean implements FactoryBean, InitializingBean {
	
	private static Log LOG = LogFactory.getLog(GrailsApplicationFactoryBean.class);
	private GrailsInjectionOperation injectionOperation = null;
	private GrailsApplication grailsApplication = null;
    private GrailsResourceLoader resourceLoader;
    private Resource descriptor;


    public GrailsInjectionOperation getInjectionOperation() {
		return injectionOperation;
	}

	public void setInjectionOperation(GrailsInjectionOperation injectionOperation) {
		this.injectionOperation = injectionOperation;
	}

	public GrailsApplicationFactoryBean() {
		super();
	}


	public void afterPropertiesSet() throws Exception {
        if(descriptor != null && descriptor.exists()) {
        	LOG.info("Loading Grails application with information from descriptor.");
        	
        	ClassLoader classLoader=null;
        	if(Environment.getCurrent().isReloadEnabled()) {
        		LOG.info("Reloading is enabled, using GrailsClassLoader.");
	            // Enforce UTF-8 on source code for reloads
	            final ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
                CompilerConfiguration config = CompilerConfiguration.DEFAULT;
                config.setSourceEncoding("UTF-8");
                classLoader = new GrailsClassLoader(parentLoader, config, resourceLoader);
        	} else {
        		LOG.info("No reloading, using standard classloader.");
        		classLoader = Thread.currentThread().getContextClassLoader();
        	}
        	
            List classes = new ArrayList();
            InputStream inputStream = null;
            try {
                inputStream = descriptor.getInputStream();

                // Get all the resource nodes in the descriptor.
                // Xpath: /grails/resources/resource, where root is /grails
                GPathResult root = new XmlSlurper().parse(inputStream);
                GPathResult resources = (GPathResult) root.getProperty("resources");
                GPathResult grailsClasses = (GPathResult) resources.getProperty("resource");

                // Each resource node should contain a full class name,
                // so we attempt to load them as classes.
                for (int i = 0; i < grailsClasses.size(); i++) {
                    GPathResult node = (GPathResult) grailsClasses.getAt(i);
                    String className = node.text();
                    try {
                    	Class clazz;
                    	if(classLoader instanceof GrailsClassLoader) {
                    		clazz=classLoader.loadClass(className);
                    	} else {
                    		clazz=Class.forName(className, true, classLoader);
                    	}
                    	classes.add(clazz);
                    } catch (ClassNotFoundException e) {
                        LOG.warn("Class with name ["+className+"] was not found, and hence not loaded. Possible empty class or script definition?");
                    }
                }
            } finally {
                if(inputStream!=null)
                    inputStream.close();
            }
            Class[] loadedClasses = (Class[])classes.toArray(new Class[classes.size()]);
            this.grailsApplication = new DefaultGrailsApplication(loadedClasses, classLoader);
        }
        else {
            Assert.notNull(resourceLoader, "Property [resourceLoader] must be set!");

            this.grailsApplication = new DefaultGrailsApplication(this.resourceLoader);
        }

        ApplicationHolder.setApplication(this.grailsApplication);
    }
	
	public Object getObject() throws Exception {
		return this.grailsApplication;
	}

	public Class getObjectType() {
		return GrailsApplication.class;
	}

	public boolean isSingleton() {
		return true;
	}

    public void setGrailsResourceLoader(GrailsResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setGrailsDescriptor(Resource r) {
        this.descriptor = r;
    }
}
