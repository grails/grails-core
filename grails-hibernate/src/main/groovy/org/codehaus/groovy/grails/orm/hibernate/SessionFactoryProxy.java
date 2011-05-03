/*
 * Copyright 2011 SpringSource
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

import org.hibernate.*;
import org.hibernate.classic.Session;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.naming.NamingException;
import javax.naming.Reference;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

/**
 * Proxies the SessionFactory allowing for the underlying SessionFactory instance to be replaced at runtime.
 * Used to enable rebuilding of the SessionFactory at development time
 *
 * @since 1.4
 * @author Graeme Rocher
 */
public class SessionFactoryProxy implements SessionFactory, ApplicationContextAware{

    private String targetBean;
    private ApplicationContext applicationContext;

    public void setTargetBean(String targetBean) {
        this.targetBean = targetBean;
    }

    private SessionFactory getCurrentSessionFactory() {
         return applicationContext.getBean(targetBean, SessionFactoryHolder.class).getSessionFactory();
    }

    public Session openSession() throws HibernateException {
        return getCurrentSessionFactory().openSession();
    }


    public Session openSession(Interceptor interceptor) throws HibernateException {
        return getCurrentSessionFactory().openSession(interceptor);
    }

    public Session openSession(Connection connection) {
        return getCurrentSessionFactory().openSession(connection);
    }

    public Session openSession(Connection connection, Interceptor interceptor) {
        return getCurrentSessionFactory().openSession(connection, interceptor);
    }

    public Session getCurrentSession() throws HibernateException {
        return getCurrentSessionFactory().getCurrentSession();
    }

    public StatelessSession openStatelessSession() {
        return getCurrentSessionFactory().openStatelessSession();
    }

    public StatelessSession openStatelessSession(Connection connection) {
        return getCurrentSessionFactory().openStatelessSession(connection);
    }

    public ClassMetadata getClassMetadata(Class entityClass) {
        return getCurrentSessionFactory().getClassMetadata(entityClass);
    }

    public ClassMetadata getClassMetadata(String entityName) {
        return getCurrentSessionFactory().getClassMetadata(entityName);
    }

    public CollectionMetadata getCollectionMetadata(String roleName) {
        return getCurrentSessionFactory().getCollectionMetadata(roleName);
    }

    public Map<String, ClassMetadata> getAllClassMetadata() {
        return getCurrentSessionFactory().getAllClassMetadata();
    }

    public Map getAllCollectionMetadata() {
        return getCurrentSessionFactory().getAllCollectionMetadata();
    }

    public Statistics getStatistics() {
        return getCurrentSessionFactory().getStatistics();
    }

    public void close() throws HibernateException {
        getCurrentSessionFactory().close();
    }

    public boolean isClosed() {
        return getCurrentSessionFactory().isClosed();
    }

    public Cache getCache() {
        return getCurrentSessionFactory().getCache();
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void evict(Class persistentClass) throws HibernateException {
        getCurrentSessionFactory().evict(persistentClass);
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void evict(Class persistentClass, Serializable id) throws HibernateException {
        getCurrentSessionFactory().evict(persistentClass, id);
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void evictEntity(String entityName) throws HibernateException {
        getCurrentSessionFactory().evictEntity(entityName);
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void evictEntity(String entityName, Serializable id) throws HibernateException {
        getCurrentSessionFactory().evictEntity(entityName, id);
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void evictCollection(String roleName) throws HibernateException {
        getCurrentSessionFactory().evictCollection(roleName);
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void evictCollection(String roleName, Serializable id) throws HibernateException {
        getCurrentSessionFactory().evictCollection(roleName, id);
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void evictQueries(String cacheRegion) throws HibernateException {
        getCurrentSessionFactory().evictQueries(cacheRegion);
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    public void evictQueries() throws HibernateException {
        getCurrentSessionFactory().evictQueries();
    }

    public Set getDefinedFilterNames() {
        return getCurrentSessionFactory().getDefinedFilterNames();
    }

    public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
        return getCurrentSessionFactory().getFilterDefinition(filterName);
    }

    public boolean containsFetchProfileDefinition(String name) {
        return getCurrentSessionFactory().containsFetchProfileDefinition(name);
    }

    public TypeHelper getTypeHelper() {
        return getCurrentSessionFactory().getTypeHelper();
    }

    public Reference getReference() throws NamingException {
        return getCurrentSessionFactory().getReference();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
