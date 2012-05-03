/*
 * Copyright 2004-2008 the original author or authors.
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

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtils;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler;
import org.hibernate.Hibernate;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.proxy.LazyInitializer;

/**
 * Implementation of the ProxyHandler interface for Hibernate.
 *
 * @author Graeme Rocher
 * @since 1.2.2
 */
public class HibernateProxyHandler implements EntityProxyHandler {

    public boolean isInitialized(Object o) {
        if (o instanceof HibernateProxy) {
            return !((HibernateProxy)o).getHibernateLazyInitializer().isUninitialized();
        }

        if (o instanceof PersistentCollection) {
            return ((PersistentCollection)o).wasInitialized();
        }

        return true;
    }

    public boolean isInitialized(Object obj, String associationName) {
        try {
            Object proxy = PropertyUtils.getProperty(obj, associationName);
            return Hibernate.isInitialized(proxy);
        }
        catch (IllegalAccessException e) {
            return false;
        }
        catch (InvocationTargetException e) {
            return false;
        }
        catch (NoSuchMethodException e) {
            return false;
        }
    }

    public Object unwrapIfProxy(Object instance) {
        if (instance instanceof HibernateProxy) {
            final HibernateProxy proxy = (HibernateProxy)instance;
            return unwrapProxy(proxy);
        }

        if (instance instanceof AbstractPersistentCollection) {
            initialize(instance);
            return instance;
        }

        return instance;
    }

    public Object unwrapProxy(final HibernateProxy proxy) {
        final LazyInitializer lazyInitializer = proxy.getHibernateLazyInitializer();
        if (lazyInitializer.isUninitialized()) {
            lazyInitializer.initialize();
        }
        final Object obj = lazyInitializer.getImplementation();
        if (obj != null) {
            GrailsHibernateUtil.ensureCorrectGroovyMetaClass(obj,obj.getClass());
        }
        return obj;
    }

    public HibernateProxy getAssociationProxy(Object obj, String associationName) {
        try {
            Object proxy = PropertyUtils.getProperty(obj, associationName);
            if (proxy instanceof HibernateProxy) {
                return (HibernateProxy) proxy;
            }
            return null;
        }
        catch (IllegalAccessException e) {
            return null;
        }
        catch (InvocationTargetException e) {
            return null;
        }
        catch (NoSuchMethodException e) {
            return null;
        }
    }

    public boolean isProxy(Object o) {
        return (o instanceof HibernateProxy) || (o instanceof AbstractPersistentCollection);
    }

    public void initialize(Object o) {
        if (o instanceof HibernateProxy) {
            final LazyInitializer hibernateLazyInitializer = ((HibernateProxy)o).getHibernateLazyInitializer();
            if (hibernateLazyInitializer.isUninitialized()) {
                hibernateLazyInitializer.initialize();
            }
        }
        else if (o instanceof AbstractPersistentCollection) {
            final AbstractPersistentCollection col = (AbstractPersistentCollection)o;
            if (!col.wasInitialized()) {
                col.forceInitialization();
            }
        }
    }

    public Object getProxyIdentifier(Object o) {
        if (o instanceof HibernateProxy) {
            return ((HibernateProxy)o).getHibernateLazyInitializer().getIdentifier();
        }
        return null;
    }

    public Class<?> getProxiedClass(Object o) {
        return HibernateProxyHelper.getClassWithoutInitializingProxy(o);
    }
}
