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
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.*;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Allows configuring Grails' hibernate support to work in conjuntion with Hibernate's annotation
 * support
 *
 * @author Graeme Rocher
 * @since 18-Feb-2006
 */
public class GrailsAnnotationConfiguration  extends AnnotationConfiguration implements GrailsDomainConfiguration{
    private static final Log LOG  = LogFactory.getLog(GrailsAnnotationConfiguration.class);
    /**
     *
     */
    private static final long serialVersionUID = -7115087342689305517L;
    private GrailsApplication grailsApplication;
    private Set domainClasses;
    private boolean configLocked;

    /**
     *
     */
    public GrailsAnnotationConfiguration() {
        super();
        this.domainClasses = new HashSet();
    }

    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration#addDomainClass(org.codehaus.groovy.grails.commons.GrailsDomainClass)
      */
    public GrailsDomainConfiguration addDomainClass( GrailsDomainClass domainClass ) {
        if(!AnnotationDomainClassArtefactHandler.isJPADomainClass(domainClass.getClazz())) {
            this.domainClasses.add(domainClass);
        }

        return this;
    }
    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration#setGrailsApplication(org.codehaus.groovy.grails.commons.GrailsApplication)
      */
    public void setGrailsApplication(GrailsApplication application) {
        application.registerArtefactHandler(new AnnotationDomainClassArtefactHandler());
        this.grailsApplication = application;
        if(this.grailsApplication != null) {
            GrailsClass[] existingDomainClasses = this.grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE);
            for(int i = 0; i < existingDomainClasses.length;i++) {
                addDomainClass((GrailsDomainClass)existingDomainClasses[i]);
            }
        }
    }




    /* (non-Javadoc)
      * @see org.hibernate.cfg.Configuration#buildSessionFactory()
      */
    public SessionFactory buildSessionFactory() throws HibernateException {
        // set the class loader to load Groovy classes
        if(this.grailsApplication != null) {
        	if(LOG.isDebugEnabled()) {
        		LOG.debug("[GrailsAnnotationConfiguration] Setting context class loader to Grails GroovyClassLoader");
        	}
        	Thread.currentThread().setContextClassLoader( this.grailsApplication.getClassLoader() );        	
        }             

        SessionFactory sessionFactory =  super.buildSessionFactory();
        return sessionFactory;
    }

    /**
     *  Overrides the default behaviour to including binding of Grails
     *  domain classes
     */
    protected void secondPassCompile() throws MappingException {
        if (configLocked) {
            return;
        }
        if(LOG.isDebugEnabled()) {
        	LOG.debug("[GrailsAnnotationConfiguration] [" + this.domainClasses.size() + "] Grails domain classes to bind to persistence runtime");
		}
        // do Grails class configuration
        for(Iterator i = this.domainClasses.iterator();i.hasNext();) {
            GrailsDomainClass domainClass = (GrailsDomainClass)i.next();
            if(LOG.isDebugEnabled()) {
	        	LOG.debug("[GrailsAnnotationConfiguration] Binding persistent class [" + domainClass.getFullName() + "]");
			}
            GrailsDomainBinder.bindClass(domainClass, super.createMappings());
        }

        // call super
        super.secondPassCompile();
        this.configLocked = true;
    }

}
