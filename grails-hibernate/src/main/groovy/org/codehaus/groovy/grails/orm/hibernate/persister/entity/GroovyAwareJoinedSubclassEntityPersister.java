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
package org.codehaus.groovy.grails.orm.hibernate.persister.entity;

import java.io.Serializable;

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.orm.hibernate.proxy.GroovyAwareJavassistProxyFactory;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;

/**
 * A customized EntityPersisteer that creates proxies valid for use with Groovy.
 *
 * @author Graeme Rocher
 * @since 1.1.1
 */
public class GroovyAwareJoinedSubclassEntityPersister extends JoinedSubclassEntityPersister {

    private GroovyAwareJavassistProxyFactory proxyFactory;

    public GroovyAwareJoinedSubclassEntityPersister(PersistentClass persistentClass,
            EntityRegionAccessStrategy cacheAccessStrategy, SessionFactoryImplementor factory,
            Mapping mapping) throws HibernateException {
        super(persistentClass, cacheAccessStrategy, factory, mapping);
        proxyFactory = GrailsHibernateUtil.buildProxyFactory(persistentClass);
    }

    @Override
    public Object createProxy(Serializable id, SessionImplementor session) throws HibernateException {
        if (proxyFactory != null) {
            return proxyFactory.getProxy(id,session);
        }

        return super.createProxy(id, session);
    }
}
