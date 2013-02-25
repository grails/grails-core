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

import groovy.util.ConfigObject;

import java.util.Map;

import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.model.MappingContext;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

/**
 * Datastore implementation that uses a Hibernate SessionFactory underneath.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class HibernateDatastore extends AbstractDatastore implements ApplicationContextAware {

    private SessionFactory sessionFactory;
    private ConfigObject config;
    private EventTriggeringInterceptor eventTriggeringInterceptor;

    public HibernateDatastore(MappingContext mappingContext, SessionFactory sessionFactory, ConfigObject config) {
        super(mappingContext);
        this.sessionFactory = sessionFactory;
        this.config = config;
        super.initializeConverters(mappingContext);
    }

    /**
     * @return The Hibernate {@link SessionFactory} being used by this datastore instance
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    protected Session createSession(Map<String, String> connectionDetails) {
        return new HibernateSession(this, sessionFactory);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext,
            "ApplicationContext must be an instanceof ConfigurableApplicationContext");

        ConfigurableApplicationContext configurableContext = (ConfigurableApplicationContext)applicationContext;
        super.setApplicationContext(configurableContext);

        // support for callbacks in domain classes
        eventTriggeringInterceptor = new EventTriggeringInterceptor(this, config);
        configurableContext.addApplicationListener(eventTriggeringInterceptor);
    }

    @Override
    protected boolean registerValidationListener() {
        return false;
    }

    // for testing
    public EventTriggeringInterceptor getEventTriggeringInterceptor() {
        return eventTriggeringInterceptor;
    }
}
