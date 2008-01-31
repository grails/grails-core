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

import groovy.lang.GroovyClassLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.compiler.injection.GrailsInjectionOperation;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Factory bean that creates a Grails application object based on Groovy files.
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
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
            // Enforce UTF-8 on source code for reloads
            CompilerConfiguration config = CompilerConfiguration.DEFAULT;
            config.setSourceEncoding("UTF-8");

            GroovyClassLoader classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), config);
            List classes = new ArrayList();
            SAXReader reader = new SAXReader();
            InputStream inputStream = null;
            try {
                inputStream = descriptor.getInputStream();
                Document doc = reader.read(inputStream);
                List grailsClasses = doc.selectNodes("/grails/resources/resource");
                for (Iterator i = grailsClasses.iterator(); i.hasNext();) {
                    Node node = (Node) i.next();
                    try {
                        classes.add(classLoader.loadClass(node.getText()));
                    } catch (ClassNotFoundException e) {
                        LOG.warn("Class with name ["+node.getText()+"] was not found, and hence not loaded. Possible empty class or script definition?");
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
