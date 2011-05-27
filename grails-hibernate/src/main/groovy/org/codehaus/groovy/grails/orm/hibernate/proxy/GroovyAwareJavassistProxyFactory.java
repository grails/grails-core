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
package org.codehaus.groovy.grails.orm.hibernate.proxy;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Hibernate's default proxying mechanism proxies Groovy's getMetaClass() method. To avoid this
 * we customize the proxying creation proxy here and in #GroovyAwareJavassistLazyInitializer.
 *
 * @author Graeme Rocher
 * @since 1.1.1
 */
public class GroovyAwareJavassistProxyFactory implements ProxyFactory, Serializable {

    private static final long serialVersionUID = 8959336753472691947L;
    protected static final Class<?>[] NO_CLASSES = new Class[0];
    private Class<?> persistentClass;
    private String entityName;
    private Class<?>[] interfaces;
    private Method getIdentifierMethod;
    private Method setIdentifierMethod;
    private CompositeType componentIdType;
    private Class<?> factory;

    @SuppressWarnings({"unchecked", "hiding", "rawtypes"})
    public void postInstantiate(
            final String entityName,
            final Class persistentClass,
            final Set interfaces,
            final Method getIdentifierMethod,
            final Method setIdentifierMethod,
            final CompositeType componentIdType) throws HibernateException {
        this.entityName = entityName;
        this.persistentClass = persistentClass;
        this.interfaces = (Class<?>[])interfaces.toArray(NO_CLASSES);
        this.getIdentifierMethod = getIdentifierMethod;
        this.setIdentifierMethod = setIdentifierMethod;
        this.componentIdType = componentIdType;
        factory = GroovyAwareJavassistLazyInitializer.getProxyFactory(persistentClass, this.interfaces);
    }

    public HibernateProxy getProxy(Serializable id, SessionImplementor session) throws HibernateException {
        return GroovyAwareJavassistLazyInitializer.getProxy(
                factory,
                entityName,
                persistentClass,
                interfaces,
                getIdentifierMethod,
                setIdentifierMethod,
                componentIdType,
                id,
                session);
    }
}
