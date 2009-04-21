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

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.orm.hibernate.proxy.GroovyAwareJavassistProxyFactory;
import org.hibernate.HibernateException;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.SingleTableEntityPersister;

import java.io.Serializable;

/**
 * A customized EntityPersisteer that creates proxies valid for use with Groovy
 * 
 * @author Graeme Rocher
 * @since 1.1.1
 *        <p/>
 *        Created: Apr 21, 2009
 */
public class GroovyAwareSingleTableEntityPersister extends SingleTableEntityPersister{

    private GroovyAwareJavassistProxyFactory proxyFactory;

    public GroovyAwareSingleTableEntityPersister(PersistentClass persistentClass, EntityRegionAccessStrategy cacheAccessStrategy, SessionFactoryImplementor factory, Mapping mapping) throws HibernateException {
        super(persistentClass, cacheAccessStrategy, factory, mapping);
        this.proxyFactory = GrailsHibernateUtil.buildProxyFactory(persistentClass);
    }

    @Override
    public Object createProxy(Serializable id, SessionImplementor session) throws HibernateException {
        if(proxyFactory!=null) {
            return proxyFactory.getProxy(id,session);
        }
        else {
            return super.createProxy(id, session);
        }
    }
}
