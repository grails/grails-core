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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import grails.gorm.DetachedCriteria;
import grails.orm.HibernateCriteriaBuilder;
import groovy.lang.Closure;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.metaclass.AbstractStaticMethodInvocation;
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTemplate;
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore;
import org.codehaus.groovy.grails.orm.hibernate.HibernateSession;
import org.codehaus.groovy.grails.orm.hibernate.query.HibernateQuery;
import org.grails.datastore.gorm.finders.DynamicFinder;
import org.grails.datastore.gorm.finders.FinderMethod;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.util.Assert;

import java.util.regex.Pattern;

/**
 * Abstract base class for static persistent methods.
 *
 * @author Steven Devijver
 * @author Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractStaticPersistentMethod extends AbstractStaticMethodInvocation implements FinderMethod {

    private ClassLoader classLoader;
    private GrailsHibernateTemplate hibernateTemplate;
    protected final GrailsApplication application;

    protected AbstractStaticPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader, Pattern pattern, GrailsApplication application) {
        Assert.notNull(sessionFactory, "Session factory is required!");
        setPattern(pattern);
        this.classLoader = classLoader;
        Assert.notNull(application, "Constructor argument 'application' cannot be null");
        this.application = application;
        hibernateTemplate = new GrailsHibernateTemplate(sessionFactory, this.application);
    }

    protected GrailsHibernateTemplate getHibernateTemplate() {
        return hibernateTemplate;
    }

    @Override
    public Object invoke(Class clazz, String methodName, Object[] arguments) {
        return invoke(clazz, methodName, (Closure) null, arguments);
    }

    public Object invoke(Class clazz, String methodName, Closure additionalCriteria, Object[] arguments) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            return doInvokeInternal(clazz, methodName, additionalCriteria, arguments);
        }
        finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Object invoke(Class clazz, String methodName, DetachedCriteria additionalCriteria, Object[] arguments) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            return doInvokeInternal(clazz, methodName, additionalCriteria, arguments);
        }
        finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    protected Criteria getCriteria(HibernateDatastore datastore, GrailsApplication appliation, Session session, DetachedCriteria detachedCriteria, Closure additionalCriteria, Class<?> clazz) {
        if (additionalCriteria != null) {
            HibernateCriteriaBuilder builder = new HibernateCriteriaBuilder(clazz, session.getSessionFactory());
            builder.setGrailsApplication(appliation);
            return builder.buildCriteria(additionalCriteria);
        }

        Criteria criteria = session.createCriteria(clazz);
        if(detachedCriteria != null) {

            HibernateSession hibernateSession = new HibernateSession(datastore, session.getSessionFactory());
            PersistentEntity persistentEntity = datastore.getMappingContext().getPersistentEntity(clazz.getName());
            if(persistentEntity != null) {
                DynamicFinder.applyDetachedCriteria(new HibernateQuery(criteria, hibernateSession, persistentEntity), detachedCriteria);
            }
        }
        hibernateTemplate.applySettings(criteria);
        return criteria;
    }

    protected abstract Object doInvokeInternal(Class clazz, String methodName, Closure additionalCriteria, Object[] arguments);

    protected abstract Object doInvokeInternal(Class clazz, String methodName, DetachedCriteria additionalCriteria, Object[] arguments);
}
