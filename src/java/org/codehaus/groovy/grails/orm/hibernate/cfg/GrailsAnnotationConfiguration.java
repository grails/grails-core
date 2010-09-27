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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.ArtefactHandler;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Mappings;
import org.hibernate.engine.FilterDefinition;

/**
 * Allows configuring Grails' hibernate support to work in conjuntion with Hibernate's annotation
 * support.
 *
 * @author Graeme Rocher
 * @since 18-Feb-2006
 */
public class GrailsAnnotationConfiguration extends AnnotationConfiguration implements GrailsDomainConfiguration{

    private static final Log LOG = LogFactory.getLog(GrailsAnnotationConfiguration.class);

    private static final long serialVersionUID = -7115087342689305517L;
    private GrailsApplication grailsApplication;
    private Set<GrailsDomainClass> domainClasses = new HashSet<GrailsDomainClass>();
    private boolean configLocked;

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration#addDomainClass(org.codehaus.groovy.grails.commons.GrailsDomainClass)
     */
    public GrailsDomainConfiguration addDomainClass(GrailsDomainClass domainClass) {
        if (shouldMapWithGorm(domainClass)) {
            domainClasses.add(domainClass);
        }

        return this;
    }

    private boolean shouldMapWithGorm(GrailsDomainClass domainClass) {
        return !AnnotationDomainClassArtefactHandler.isJPADomainClass(domainClass.getClazz()) && domainClass.getMappingStrategy().equalsIgnoreCase(GrailsDomainClass.GORM);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration#setGrailsApplication(org.codehaus.groovy.grails.commons.GrailsApplication)
     */
    public void setGrailsApplication(GrailsApplication application) {
        this.grailsApplication = application;
        if (grailsApplication == null) {
            return;
        }

        GrailsClass[] existingDomainClasses = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE);
        for (GrailsClass existingDomainClass : existingDomainClasses) {
            addDomainClass((GrailsDomainClass) existingDomainClass);
        }

        ArtefactHandler handler = grailsApplication.getArtefactHandler(DomainClassArtefactHandler.TYPE);
        if (!(handler instanceof AnnotationDomainClassArtefactHandler)) {
            return;
        }

        Set<String> jpaDomainNames = ((AnnotationDomainClassArtefactHandler)handler).getJpaClassNames();
        if (jpaDomainNames == null) {
            return;
        }

        final ClassLoader loader = grailsApplication.getClassLoader();
        for (String jpaDomainName : jpaDomainNames) {
            try {
                addAnnotatedClass(loader.loadClass(jpaDomainName));
            }
            catch (ClassNotFoundException e) {
                // impossible condition
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hibernate.cfg.Configuration#buildSessionFactory()
     */
    @Override
    public SessionFactory buildSessionFactory() throws HibernateException {
        // set the class loader to load Groovy classes
        if (grailsApplication != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[GrailsAnnotationConfiguration] Setting context class loader to Grails GroovyClassLoader");
            }
            Thread.currentThread().setContextClassLoader(grailsApplication.getClassLoader());
        }

        // work around for HHH-2624
        addFilterDefinition(new FilterDefinition("dynamicFilterEnabler","1=1", Collections.emptyMap()));

        SessionFactory sessionFactory = super.buildSessionFactory();

        if (grailsApplication != null) {
            GrailsHibernateUtil.configureHibernateDomainClasses(sessionFactory, grailsApplication);
        }

        return sessionFactory;
    }

    /**
     * Overrides the default behaviour to including binding of Grails domain classes.
     */
    @Override
    protected void secondPassCompile() throws MappingException {
        if (!configLocked) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[GrailsAnnotationConfiguration] [" + domainClasses.size() + "] Grails domain classes to bind to persistence runtime");
            }

            // do Grails class configuration
            DefaultGrailsDomainConfiguration.configureDomainBinder(grailsApplication,domainClasses);

            // do Grails class configuration
            for (GrailsDomainClass domainClass : domainClasses) {
                final String fullClassName = domainClass.getFullName();

                String hibernateConfig = fullClassName.replace('.', '/') + ".hbm.xml";
                final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                // don't configure Hibernate mapped classes
                if (loader.getResource(hibernateConfig) != null) continue;

                if (LOG.isDebugEnabled()) {
                    LOG.debug("[GrailsAnnotationConfiguration] Binding persistent class [" + fullClassName + "]");
                }
                final Mappings mappings = super.createMappings();
                Mapping m = GrailsDomainBinder.getMapping(domainClass);
                mappings.setAutoImport(m== null || m.getAutoImport());
                GrailsDomainBinder.bindClass(domainClass, mappings);
            }
        }

        super.secondPassCompile();
        configLocked = true;
    }
}
