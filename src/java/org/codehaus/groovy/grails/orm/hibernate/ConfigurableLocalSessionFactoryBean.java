
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

import groovy.lang.GroovySystem;
import groovy.lang.MetaClassRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

import javax.persistence.Entity;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


/**
 * A SessionFactory bean that allows the configuration class to be changed and customise for usage within Grails
 *
 * @author Graeme Rocher
 * @since 07-Jul-2005
 */
public class ConfigurableLocalSessionFactoryBean extends
		LocalSessionFactoryBean implements ApplicationContextAware {


    private static final Log LOG = LogFactory.getLog(ConfigurableLocalSessionFactoryBean.class);
    private ClassLoader classLoader = null;
    private GrailsApplication grailsApplication;
    private Class configClass = GrailsAnnotationConfiguration.class;
    private ApplicationContext applicationContext;
    private Class currentSessionContextClass;

    /**
     * Sets class to be used for the Hibernate CurrentSessionContext
     *
     * @param currentSessionContextClass An implementation of the CurrentSessionContext interface
     */
    public void setCurrentSessionContextClass(Class currentSessionContextClass) {
        this.currentSessionContextClass = currentSessionContextClass;
    }

    /**
     * Sets the class to be used for Hibernate Configuration
     * @param configClass A subclass of the Hibernate Configuration class
     */
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
        Object config = BeanUtils.instantiateClass(configClass);
        if(config instanceof GrailsDomainConfiguration) {
            GrailsDomainConfiguration grailsConfig = (GrailsDomainConfiguration) config;
            grailsConfig.setGrailsApplication(grailsApplication);
        }
        if(currentSessionContextClass != null) {
            ((Configuration)config).setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, currentSessionContextClass.getName());
            // don't allow Spring's LocaalSessionFactoryBean to override setting
            setExposeTransactionAwareSessionFactory(false);
        }
        return (Configuration)config;
	}


	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

    protected SessionFactory newSessionFactory(Configuration config) throws HibernateException {
        try {
            if(applicationContext!= null && (config instanceof AnnotationConfiguration)) {
                Collection entityBeans = new ArrayList(applicationContext.getBeansWithAnnotation(Entity.class).values());
                if(applicationContext.getParent()!=null) {
                    entityBeans.addAll(applicationContext.getParent().getBeansWithAnnotation(Entity.class).values());
                }
                AnnotationConfiguration ac = (AnnotationConfiguration) config;
                for (Object entityBean : entityBeans) {
                    ac.addAnnotatedClass(entityBean.getClass());    
                }
            }
            return super.newSessionFactory(config);
        }
        catch (HibernateException e) {
            Throwable cause = e.getCause();
            if(isCacheConfigurationError(cause)) {
                LOG.fatal("There was an error configuring the Hibernate second level cache: " + getCauseMessage(e));
                LOG.fatal("This is normally due to one of two reasons. Either you have incorrectly specified the cache provider class name in [DataSource.groovy] or you do not have the cache provider on your classpath (eg. runtime (\"net.sf.ehcache:ehcache:1.6.1\"))");
                if(grails.util.Environment.getCurrent() == grails.util.Environment.DEVELOPMENT && !(getGrailsApplication().isWarDeployed()))
                    System.exit(1);
            }
            throw e;
        }
    }

    private String getCauseMessage(HibernateException e) {
        Throwable cause = e.getCause();
        if(cause instanceof InvocationTargetException) {
            cause = ((InvocationTargetException)cause).getTargetException();
        }        
        return cause.getMessage();
    }

    private boolean isCacheConfigurationError(Throwable cause) {
        if(cause instanceof InvocationTargetException) {
            cause = ((InvocationTargetException)cause).getTargetException();
        }
        return cause != null && (cause instanceof CacheException);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

    @Override
    public void destroy() throws HibernateException {
        MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
        Map classMetaData = getSessionFactory().getAllClassMetadata();
        for (Object o : classMetaData.values()) {
            ClassMetadata classMetadata = (ClassMetadata) o;
            Class mappedClass = classMetadata.getMappedClass(EntityMode.POJO);
            registry.removeMetaClass(mappedClass);
        }
        super.destroy();
    }
}

