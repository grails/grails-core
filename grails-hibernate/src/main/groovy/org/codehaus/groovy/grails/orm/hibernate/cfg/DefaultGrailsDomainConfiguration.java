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

import groovy.lang.Closure;
import groovy.util.Eval;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;

/**
 * Creates runtime configuration mapping for the Grails domain classes
 * based on the work done in the Hibernate Annotations project
 *
 * @author Graeme Rocher
 * @since 06-Jul-2005
 */
public class DefaultGrailsDomainConfiguration extends Configuration implements GrailsDomainConfiguration {

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
        if (domainClass.getMappingStrategy().equalsIgnoreCase(GrailsDomainClass.GORM)) {
            domainClasses.add(domainClass);
        }

        return this;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration#setGrailsApplication(org.codehaus.groovy.grails.commons.GrailsApplication)
     */
    public void setGrailsApplication(GrailsApplication application) {
        grailsApplication = application;
        if (grailsApplication == null) {
            return;
        }

        GrailsClass[] existingDomainClasses = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE);
        for (GrailsClass existingDomainClass : existingDomainClasses) {
            addDomainClass((GrailsDomainClass) existingDomainClass);
        }
    }

    public void setSessionFactoryBeanName(String name) {
        sessionFactoryBeanName = name;
    }

    public void setDataSourceName(String name) {
        dataSourceName = name;
    }

    /**
     * Overrides the default behaviour to including binding of Grails domain classes.
     */
    @Override
    protected void secondPassCompile() throws MappingException {
        if (configLocked) {
            return;
        }

        // set the class loader to load Groovy classes
        if (grailsApplication != null) {
            Thread.currentThread().setContextClassLoader(grailsApplication.getClassLoader());
        }

        configureDomainBinder(grailsApplication, domainClasses);

        for (GrailsDomainClass domainClass : domainClasses) {
            if (!GrailsHibernateUtil.usesDatasource(domainClass, dataSourceName)) {
                continue;
            }
            final Mappings mappings = super.createMappings();
            Mapping m = GrailsDomainBinder.getMapping(domainClass);
            mappings.setAutoImport(m == null || m.getAutoImport());
            GrailsDomainBinder.bindClass(domainClass, mappings, sessionFactoryBeanName);
        }

        super.secondPassCompile();
        configLocked = true;
    }

    public static void configureDomainBinder(GrailsApplication grailsApplication, Set<GrailsDomainClass> domainClasses) {
        Object defaultMapping = Eval.x(grailsApplication, "x.config?.grails?.gorm?.default?.mapping");
        // do Grails class configuration
        for (GrailsDomainClass domainClass : domainClasses) {
            if (defaultMapping instanceof Closure) {
                GrailsDomainBinder.evaluateMapping(domainClass, (Closure<?>)defaultMapping);
            }
            else {
                GrailsDomainBinder.evaluateMapping(domainClass);
            }
        }
    }
}
