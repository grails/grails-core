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
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.FilterDefinition;

/**
 * Allows configuring Grails' hibernate support to work in conjuntion with Hibernate's annotation
 * support.
 *
 * @author Graeme Rocher
 * @since 18-Feb-2006
 */
public class GrailsAnnotationConfiguration extends Configuration implements GrailsDomainConfiguration {

    private static final Log LOG = LogFactory.getLog(GrailsAnnotationConfiguration.class);
    private static final long serialVersionUID = -7115087342689305517L;

    private GrailsApplication grailsApplication;
    private Set<GrailsDomainClass> domainClasses = new HashSet<GrailsDomainClass>();
    private boolean configLocked;
    private String sessionFactoryBeanName = "sessionFactory";
    private String dataSourceName = GrailsDomainClassProperty.DEFAULT_DATA_SOURCE;

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
        return !AnnotationDomainClassArtefactHandler.isJPADomainClass(domainClass.getClazz()) &&
               domainClass.getMappingStrategy().equalsIgnoreCase(GrailsDomainClass.GORM);
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration#setGrailsApplication(org.codehaus.groovy.grails.commons.GrailsApplication)
     */
    public void setGrailsApplication(GrailsApplication application) {
        grailsApplication = application;
        if (grailsApplication == null) {
            return;
        }

        configureNamingStrategy();
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

    public void setSessionFactoryBeanName(String name) {
        sessionFactoryBeanName = name;
    }

    public void setDataSourceName(String name) {
        dataSourceName = name;
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
        addFilterDefinition(new FilterDefinition("dynamicFilterEnabler", "1=1", Collections.emptyMap()));

        SessionFactory sessionFactory = super.buildSessionFactory();

        if (grailsApplication != null) {
            GrailsHibernateUtil.configureHibernateDomainClasses(
                    sessionFactory, sessionFactoryBeanName, grailsApplication);
        }

        return sessionFactory;
    }

    /**
     * Overrides the default behaviour to including binding of Grails domain classes.
     */
    @Override
    protected void secondPassCompile() throws MappingException {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader originalContextLoader = currentThread.getContextClassLoader();
        if (!configLocked) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[GrailsAnnotationConfiguration] [" + domainClasses.size() +
                          "] Grails domain classes to bind to persistence runtime");
            }

            // do Grails class configuration
            DefaultGrailsDomainConfiguration.configureDomainBinder(grailsApplication, domainClasses);

            for (GrailsDomainClass domainClass : domainClasses) {

                final String fullClassName = domainClass.getFullName();

                String hibernateConfig = fullClassName.replace('.', '/') + ".hbm.xml";
                final ClassLoader loader = originalContextLoader;
                // don't configure Hibernate mapped classes
                if (loader.getResource(hibernateConfig) != null) continue;

                final Mappings mappings = super.createMappings();
                if (!GrailsHibernateUtil.usesDatasource(domainClass, dataSourceName)) {
                    continue;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("[GrailsAnnotationConfiguration] Binding persistent class [" + fullClassName + "]");
                }

                Mapping m = GrailsDomainBinder.getMapping(domainClass);
                mappings.setAutoImport(m == null || m.getAutoImport());
                GrailsDomainBinder.bindClass(domainClass, mappings, sessionFactoryBeanName);
            }
        }

        try {
            currentThread.setContextClassLoader(grailsApplication.getClassLoader());
            super.secondPassCompile();
        } finally {
            currentThread.setContextClassLoader(originalContextLoader);
        }

        configLocked = true;
    }

    /**
     * Sets custom naming strategy specified in configuration or the default {@link ImprovedNamingStrategy}.
     */
    private void configureNamingStrategy() {
        NamingStrategy strategy = null;
        Object customStrategy = grailsApplication.getFlatConfig().get("hibernate.naming_strategy");
        if (customStrategy != null) {
            Class<?> namingStrategyClass = null;
            if (customStrategy instanceof Class<?>) {
                namingStrategyClass = (Class<?>)customStrategy;
            } else {
                try {
                    namingStrategyClass = grailsApplication.getClassLoader().loadClass(customStrategy.toString());
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }

            if (namingStrategyClass != null) {
                try {
                    strategy = (NamingStrategy)namingStrategyClass.newInstance();
                } catch (InstantiationException e) {
                    // ignore
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
        }

        if (strategy == null) {
            strategy = ImprovedNamingStrategy.INSTANCE;
        }

        setNamingStrategy(strategy);
    }
}
