/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.orm.hibernate.validation;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.lifecycle.ShutdownOperations;
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateDomainClass;
import org.codehaus.groovy.grails.validation.AbstractConstraint;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * Constraints that require access to the HibernateTemplate should subclass this class.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public abstract class AbstractPersistentConstraint extends AbstractConstraint implements PersistentConstraint {

    public static ThreadLocal<SessionFactory> sessionFactory = new ThreadLocal<SessionFactory>();

    static {
        ShutdownOperations.addOperation(new Runnable() {
            public void run() {
                sessionFactory.remove();
            }
        });
    }

    protected ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public HibernateTemplate getHibernateTemplate() {
        SessionFactory sf = sessionFactory.get();
        if (sf == null) {
            sf = applicationContext.getBean("sessionFactory", SessionFactory.class);
        }
        return new HibernateTemplate(sf, true);
    }

    /**
     * Returns whether the constraint supports being applied against the specified type;
     *
     * @param type The type to support
     * @return true if the constraint can be applied against the specified type
     */
    public boolean supports(@SuppressWarnings("rawtypes") Class type) {
        return true;
    }

    /**
     * Return whether the constraint is valid for the owning class
     *
     * @return true if it is
     */
    @Override
    public boolean isValid() {
        if (applicationContext.containsBean("sessionFactory")) {
            GrailsApplication grailsApplication = applicationContext.getBean(
                    GrailsApplication.APPLICATION_ID, GrailsApplication.class);
            GrailsDomainClass domainClass = (GrailsDomainClass)grailsApplication.getArtefact(
                    DomainClassArtefactHandler.TYPE, constraintOwningClass.getName());
            if (domainClass != null) {
                String mappingStrategy = domainClass.getMappingStrategy();
                return mappingStrategy.equals(GrailsDomainClass.GORM)
                    || mappingStrategy.equals(GrailsHibernateDomainClass.HIBERNATE);
            }
        }
        return false;
    }
}
