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
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;

import java.util.HashSet;
import java.util.Set;

/**
 * Creates runtime configuration mapping for the Grails domain classes
 * based on the work done in the Hibernate Annotations project
 * 
 * @author Graeme Rocher
 * @since 06-Jul-2005
 */
public class DefaultGrailsDomainConfiguration extends Configuration implements GrailsDomainConfiguration {

    //private static final Log LOG  = LogFactory.getLog(DefaultGrailsDomainConfiguration.class);
    /**
     *
     */
    private static final long serialVersionUID = -7115087342689305517L;
    private GrailsApplication grailsApplication;
    private Set<GrailsDomainClass> domainClasses;
    private boolean configLocked;
    /**
     *
     */
    public DefaultGrailsDomainConfiguration() {
        super();
        this.domainClasses = new HashSet<GrailsDomainClass>();
    }

    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration#addDomainClass(org.codehaus.groovy.grails.commons.GrailsDomainClass)
      */
    public GrailsDomainConfiguration addDomainClass( GrailsDomainClass domainClass ) {
        if(domainClass.getMappingStrategy().equalsIgnoreCase( GrailsDomainClass.GORM )) {
            this.domainClasses.add(domainClass);
        }

        return this;
    }
    /* (non-Javadoc)
      * @see org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainConfiguration#setGrailsApplication(org.codehaus.groovy.grails.commons.GrailsApplication)
      */
    public void setGrailsApplication(GrailsApplication application) {
        this.grailsApplication = application;
        if(this.grailsApplication != null) {
            GrailsClass[] existingDomainClasses = this.grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE);
            for (GrailsClass existingDomainClass : existingDomainClasses) {
                addDomainClass((GrailsDomainClass) existingDomainClass);
            }
        }
    }

    /**
     *  Overrides the default behaviour to including binding of Grails
     *  domain classes
     */
    protected void secondPassCompile() throws MappingException {
        if (configLocked) {
            return;
        }
        // set the class loader to load Groovy classes
        if(this.grailsApplication != null)
            Thread.currentThread().setContextClassLoader( this.grailsApplication.getClassLoader() );

        configureDomainBinder(this.grailsApplication,this.domainClasses);

        for (GrailsDomainClass domainClass : this.domainClasses) {
            final Mappings mappings = super.createMappings();
            Mapping m = GrailsDomainBinder.getMapping(domainClass);
            mappings.setAutoImport(m== null || m.getAutoImport());
            GrailsDomainBinder.bindClass(domainClass, mappings);
        }

        // call super
        super.secondPassCompile();
        this.configLocked = true;
    }

    public static void configureDomainBinder(GrailsApplication grailsApplication, Set<GrailsDomainClass> domainClasses) {
        Object defaultMapping = Eval.x(grailsApplication, "x.config?.grails?.gorm?.default?.mapping");
        // do Grails class configuration
        for (GrailsDomainClass domainClass : domainClasses) {
            if(defaultMapping instanceof Closure) {
                GrailsDomainBinder.evaluateMapping(domainClass, (Closure)defaultMapping);                
            }
            else {
                GrailsDomainBinder.evaluateMapping(domainClass);
            }
        }

        for (GrailsDomainClass domainClass : domainClasses) {
            GrailsDomainBinder.evaluateNamedQueries(domainClass);
        }
    }
}
