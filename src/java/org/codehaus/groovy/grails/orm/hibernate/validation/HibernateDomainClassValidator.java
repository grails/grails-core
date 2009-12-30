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

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator;
import org.hibernate.SessionFactory;
import org.hibernate.FlushMode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.classic.Session;
import org.hibernate.collection.PersistentCollection;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.validation.Errors;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * A validator that first checks if the Hibernate PersistentCollection instance has been initialised before bothering
 * to cascade
 *
 * @author Graeme Rocher
 * @since 0.5
 * 
 *        <p/>
 *        Created: Apr 13, 2007
 *        Time: 6:32:08 PM
 */
public class HibernateDomainClassValidator extends GrailsDomainClassValidator implements ApplicationContextAware {


    private static ThreadLocal validatedInstances = new ThreadLocal() {
        protected Object initialValue() {
            return new ArrayList();
        }
    };
    private ApplicationContext applicationContext;
    private SessionFactory sessionFactory;

    @Override
    protected GrailsDomainClass getAssociatedDomainClassFromApplication(Object associatedObject) {
        String associatedObjectType = associatedObject.getClass().getName();
        if (associatedObject instanceof HibernateProxy) {
            associatedObjectType = ((HibernateProxy) associatedObject).getHibernateLazyInitializer().getEntityName();
        }
        return (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, associatedObjectType);
    }

    @Override
    public void validate(Object obj, Errors errors, boolean cascade) {
        final Session session = sessionFactory.getCurrentSession();
        FlushMode previousMode = null;
        try {
            if(session!=null) {
                previousMode = session.getFlushMode();
                session.setFlushMode(FlushMode.MANUAL);
            }

            super.validate(obj, errors, cascade);
        }
        finally {
            if(session!=null && previousMode!=null) {
                session.setFlushMode(previousMode);
            }
        }
    }

    /**
     * Overrides the default behaviour and first checks if a PersistentCollection instance has been initialised using the
     * wasInitialised() method before cascading
     *
     * @param errors The Spring Errors instance
     * @param bean The BeanWrapper for the bean
     * @param persistentProperty The GrailsDomainClassProperty instance
     * @param propertyName The name of the property
     *
     * @see org.hibernate.collection.PersistentCollection#wasInitialized()
     */
    protected void cascadeValidationToMany(Errors errors, BeanWrapper bean, GrailsDomainClassProperty persistentProperty, String propertyName) {
        Object collection = bean.getPropertyValue(propertyName);
        if(collection != null) {
            if(collection instanceof PersistentCollection) {
                PersistentCollection persistentCollection = (PersistentCollection)collection;
                if(persistentCollection.wasInitialized()) {
                    super.cascadeValidationToMany(errors, bean, persistentProperty, propertyName);
                }
            }
            else {
                super.cascadeValidationToMany(errors, bean, persistentProperty, propertyName);
            }

        }
    }

    protected void cascadeValidationToOne(Errors errors, BeanWrapper bean, Object associatedObject, GrailsDomainClassProperty persistentProperty, String propertyName) {
        List validatedInstancesList = (List)validatedInstances.get();
        validatedInstancesList.add(associatedObject);
        super.cascadeValidationToOne(errors, bean, associatedObject, persistentProperty, propertyName);
    }

    protected void postValidate(Object obj, Errors errors) {
        try {
            if(applicationContext != null && applicationContext.containsBean("sessionFactory")) {
                SessionFactory sessionFactory = (SessionFactory) applicationContext.getBean("sessionFactory");
                if(errors.hasErrors()) {
                    GrailsHibernateUtil.setObjectToReadyOnly(obj, sessionFactory);
                    List invalidInstances = (List) validatedInstances.get();
                    for (Iterator i = invalidInstances.iterator(); i.hasNext();) {
                        Object instance = i.next();
                        GrailsHibernateUtil.setObjectToReadyOnly(instance, sessionFactory);
                    }
                }
                else {
                    GrailsHibernateUtil.setObjectToReadWrite(obj, sessionFactory);
                }
            }
        } finally {
            List validatedInstancesList = (List)validatedInstances.get();
            if(validatedInstancesList!=null) {
                validatedInstancesList.clear();
            }
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        if(applicationContext!=null) {
            try {
                this.sessionFactory = applicationContext.getBean("sessionFactory", SessionFactory.class);
            }
            catch (BeansException e) {
                // no session factory, continue
            }
        }
    }
}
