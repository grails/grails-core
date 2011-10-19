/* Copyright (C) 2011 SpringSource
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

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.FlushModeType;

import org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext;
import org.codehaus.groovy.grails.orm.hibernate.query.HibernateQuery;
import org.grails.datastore.mapping.core.AbstractAttributeStoringSession;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.datastore.mapping.query.jpa.JpaQueryBuilder;
import org.grails.datastore.mapping.query.jpa.JpaQueryInfo;
import org.grails.datastore.mapping.transactions.Transaction;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * Session implementation that wraps a Hibernate {@link org.hibernate.Session}.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class HibernateSession extends AbstractAttributeStoringSession {

    private GrailsHibernateTemplate hibernateTemplate;
    private HibernateDatastore datastore;
    private boolean connected = true;

    public HibernateSession(HibernateDatastore hibernateDatastore, SessionFactory sessionFactory) {
        datastore = hibernateDatastore;

        if (datastore.getMappingContext() instanceof GrailsDomainClassMappingContext) {
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory,
                    ((GrailsDomainClassMappingContext)datastore.getMappingContext()).getGrailsApplication());
        }
        else {
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory);
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void disconnect() {
        connected = false; // don't actually do any disconnection here. This
                           // will be handled by OSVI
    }

    public Transaction beginTransaction() {
        throw new UnsupportedOperationException("Use HibernatePlatformTransactionManager instead");
    }

    public MappingContext getMappingContext() {
        return getDatastore().getMappingContext();
    }

    public Serializable persist(Object o) {
        return hibernateTemplate.save(o);
    }

    public void refresh(Object o) {
        hibernateTemplate.refresh(o);
    }

    public void attach(Object o) {
        hibernateTemplate.lock(o, LockMode.NONE);
    }

    public void flush() {
        hibernateTemplate.flush();
    }

    public void clear() {
        hibernateTemplate.clear();
    }

    public void clear(Object o) {
        hibernateTemplate.evict(o);
    }

    public boolean contains(Object o) {
        return hibernateTemplate.contains(o);
    }

    public void setFlushMode(FlushModeType flushMode) {
        if (flushMode == FlushModeType.AUTO) {
            hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_AUTO);
        }
        else if (flushMode == FlushModeType.COMMIT) {
            hibernateTemplate.setFlushMode(HibernateTemplate.FLUSH_COMMIT);
        }
    }

    public FlushModeType getFlushMode() {
        switch (hibernateTemplate.getFlushMode()) {
            case HibernateTemplate.FLUSH_AUTO:
                return FlushModeType.AUTO;
            case HibernateTemplate.FLUSH_COMMIT:
                return FlushModeType.COMMIT;
            case HibernateTemplate.FLUSH_ALWAYS:
                return FlushModeType.AUTO;
            default:
                return FlushModeType.AUTO;
        }
    }

    public void lock(Object o) {
        hibernateTemplate.lock(o, LockMode.PESSIMISTIC_WRITE);
    }

    public void unlock(Object o) {
        // do nothing
    }

    public List<Serializable> persist(Iterable objects) {
        List<Serializable> identifiers = new ArrayList<Serializable>();
        for (Object object : objects) {
            identifiers.add(hibernateTemplate.save(object));
        }
        return identifiers;
    }

    public <T> T retrieve(Class<T> type, Serializable key) {
        return hibernateTemplate.get(type, key);
    }

    public <T> T proxy(Class<T> type, Serializable key) {
        return hibernateTemplate.load(type, key);
    }

    public <T> T lock(Class<T> type, Serializable key) {
        return hibernateTemplate.get(type, key, LockMode.PESSIMISTIC_WRITE);
    }

    public void delete(Iterable objects) {
        Collection list = getIterableAsCollection(objects);
        hibernateTemplate.deleteAll(list);
    }

    @SuppressWarnings("unchecked")
    Collection getIterableAsCollection(Iterable objects) {
        Collection list;
        if (objects instanceof Collection) {
            list = (Collection)objects;
        }
        else {
            list = new ArrayList();
            for (Object object : objects) {
                list.add(object);
            }
        }
        return list;
    }

    public void delete(Object obj) {
        hibernateTemplate.delete(obj);
    }

    /**
     * Deletes all objects matching the given criteria.
     *
     * @param criteria The criteria
     * @return The total number of records deleted
     */
    public int deleteAll(final QueryableCriteria criteria) {
        return hibernateTemplate.execute(new HibernateCallback<Integer>() {
            public Integer doInHibernate(Session session) throws HibernateException, SQLException {
                JpaQueryBuilder builder = new JpaQueryBuilder(criteria);
                builder.setHibernateCompatible(true);
                JpaQueryInfo jpaQueryInfo = builder.buildDelete();

                org.hibernate.Query query = session.createQuery(jpaQueryInfo.getQuery());
                hibernateTemplate.applySettings(query);

                List parameters = jpaQueryInfo.getParameters();
                if (parameters != null) {
                    for (int i = 0, count = parameters.size(); i < count; i++) {
                        query.setParameter(i, parameters.get(i));
                    }
                }
                return query.executeUpdate();
            }
        });
    }

    /**
     * Updates all objects matching the given criteria and property values.
     *
     * @param criteria The criteria
     * @param properties The properties
     * @return The total number of records updated
     */
    public int updateAll(final QueryableCriteria criteria, final Map<String, Object> properties) {
        return hibernateTemplate.execute(new HibernateCallback<Integer>() {
            public Integer doInHibernate(Session session) throws HibernateException, SQLException {
                JpaQueryBuilder builder = new JpaQueryBuilder(criteria);
                builder.setHibernateCompatible(true);
                JpaQueryInfo jpaQueryInfo = builder.buildUpdate(properties);

                org.hibernate.Query query = session.createQuery(jpaQueryInfo.getQuery());
                hibernateTemplate.applySettings(query);
                List parameters = jpaQueryInfo.getParameters();
                if (parameters != null) {
                    for (int i = 0, count = parameters.size(); i < count; i++) {
                        query.setParameter(i, parameters.get(i));
                    }
                }
                return query.executeUpdate();
            }
        });
    }

    public List retrieveAll(final Class type, final Iterable keys) {
        final PersistentEntity persistentEntity = getMappingContext().getPersistentEntity(type.getName());
        return hibernateTemplate.execute(new HibernateCallback<List>() {
            public List doInHibernate(org.hibernate.Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(type);
                hibernateTemplate.applySettings(criteria);
                return criteria.add(
                        Restrictions.in(persistentEntity.getIdentity().getName(), getIterableAsCollection(keys)))
                        .list();
            }
        });
    }

    public List retrieveAll(Class type, Serializable... keys) {
        return retrieveAll(type, Arrays.asList(keys));
    }

    public Query createQuery(Class type) {
        final PersistentEntity persistentEntity = getMappingContext().getPersistentEntity(type.getName());
        final Criteria criteria = hibernateTemplate.getSessionFactory().getCurrentSession().createCriteria(type);
        hibernateTemplate.applySettings(criteria);
        return new HibernateQuery(criteria, this, persistentEntity);
    }

    public HibernateTemplate getNativeInterface() {
        return hibernateTemplate;
    }

    public Persister getPersister(Object o) {
        return null;
    }

    public Transaction getTransaction() {
        throw new UnsupportedOperationException("Use HibernatePlatformTransactionManager instead");
    }

    public Datastore getDatastore() {
        return datastore;
    }

    public boolean isDirty(Object o) {
        // not used, Hibernate manages dirty checking itself
        return true;
    }
}
