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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.compiler.injection.GrailsInjectionOperation;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * <p>Factory bean that creates a Grails application object based on Groovy files.
 * 
 * @author Steven Devijver
 * @since Jul 2, 2005
 */
public class GrailsApplicationFactoryBean implements FactoryBean, InitializingBean {
	
	private static Log LOG = LogFactory.getLog(GrailsApplicationFactoryBean.class);
	private GrailsInjectionOperation injectionOperation = null;
	private GrailsApplication grailsApplication = null;
    private GrailsResourceLoader resourceLoader;


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

        Assert.notNull(resourceLoader, "Property [resourceLoader] must be set!");

		this.grailsApplication = new DefaultGrailsApplication(this.resourceLoader);
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
}
