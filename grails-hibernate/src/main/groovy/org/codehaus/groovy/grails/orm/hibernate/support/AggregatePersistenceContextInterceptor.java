/*
 * Copyright 2011 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate.support;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Burt Beckwith
 */
public class AggregatePersistenceContextInterceptor implements PersistenceContextInterceptor, InitializingBean, ApplicationContextAware {

    private List<PersistenceContextInterceptor> interceptors = new ArrayList<PersistenceContextInterceptor>();
    private List<String> dataSourceNames = new ArrayList<String>();
    private ApplicationContext applicationContext;

    public boolean isOpen() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            if (interceptor.isOpen()) {
                // true at least one is true
                return true;
            }
        }
        return false;
    }

    public void reconnect() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.reconnect();
        }
    }

    public void destroy() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            if (interceptor.isOpen()) {
                interceptor.destroy();
            }
        }
    }

    public void clear() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.clear();
        }
    }

    public void disconnect() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.disconnect();
        }
    }

    public void flush() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.flush();
        }
    }

    public void init() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.init();
        }
    }

    public void setReadOnly() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.setReadOnly();
        }
    }

    public void setReadWrite() {
        for (PersistenceContextInterceptor interceptor : interceptors) {
            interceptor.setReadWrite();
        }
    }

    /**
     * Dependency injection for the datasource names.
     * @param names  the names
     */
    public void setDataSourceNames(List<String> names) {
        dataSourceNames = names;
    }

    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    public void afterPropertiesSet() {
        // need to lazily create these instead of registering as beans since GrailsPageFilter
        // looks for instances of PersistenceContextInterceptor and picks one assuming
        // there's only one, so this one has to be the only one
        for (String name : dataSourceNames) {
            String suffix = name == GrailsDomainClassProperty.DEFAULT_DATA_SOURCE ? "" : "_" + name;
            HibernatePersistenceContextInterceptor interceptor = new HibernatePersistenceContextInterceptor();
            String beanName = "sessionFactory" + suffix;
            if (applicationContext.containsBean(beanName)) {
                interceptor.setSessionFactory((SessionFactory)applicationContext.getBean(beanName));
            }
            interceptors.add(interceptor);
        }
    }
}
