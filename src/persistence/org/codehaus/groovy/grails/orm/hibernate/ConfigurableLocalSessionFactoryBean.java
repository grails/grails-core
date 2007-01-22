
/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.DefaultGrailsDomainConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.cfg.Configuration;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;


/**
 * A SessionFactory bean that allows the configuration class to be changed and customise for usage within Grails
 *
 * @author Graeme Rocher
 * @since 07-Jul-2005
 */
public class ConfigurableLocalSessionFactoryBean extends
		LocalSessionFactoryBean implements ApplicationContextAware {

	
	private ClassLoader classLoader = null;
	private GrailsApplication grailsApplication;
    private Class configClass = DefaultGrailsDomainConfiguration.class;
	private ApplicationContext applicationContext;

    public void setConfigClass(Class configClass) {
        this.configClass = configClass;
    }

    /**
	 * 
	 */
	public ConfigurableLocalSessionFactoryBean() {
		super();		
	}
	
	/**
	 * @return Returns the grailsApplication.
	 */
	public GrailsApplication getGrailsApplication() {
		return grailsApplication;
	}

	/**
	 * @param grailsApplication The grailsApplication to set.
	 */
	public void setGrailsApplication(GrailsApplication grailsApplication) {
		this.grailsApplication = grailsApplication;
	}
	
	/**
	 * Overrides default behaviour to allow for a configurable configuration class 
	 */
	protected Configuration newConfiguration() {
		GrailsDomainConfiguration config = (GrailsDomainConfiguration)BeanUtils.instantiateClass(configClass);
		config.setGrailsApplication(grailsApplication);
        return (Configuration)config;
	}


	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	public void afterPropertiesSet() throws Exception {
		ClassLoader originalClassLoader = null;
		if (this.classLoader != null) {
			originalClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(this.classLoader);
		}
        super.afterPropertiesSet();

        if(this.applicationContext!= null) {
        	GrailsHibernateUtil.configureDynamicMethods(applicationContext,this.grailsApplication);
        }
                
        if (originalClassLoader != null) {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}

