/*
 * Copyright 2003-2007 the original author or authors.
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

import groovy.lang.GroovySystem;
import groovy.util.ConfigObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.orm.hibernate.events.SaveOrUpdateEventListener;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.hibernate.HibernateException;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PostLoadEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreDeleteEvent;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.event.SaveOrUpdateEvent;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>Invokes closure events on domain entities such as beforeInsert, beforeUpdate and beforeDelete.
 *
 * <p>Also deals with auto time stamping of domain classes that have properties named 'lastUpdated' and/or 'dateCreated'.
 *
 * @author Graeme Rocher
 * @author Lari Hotari
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class ClosureEventTriggeringInterceptor extends SaveOrUpdateEventListener
       implements ApplicationContextAware,
                  GrailsConfigurationAware,
                  PreLoadEventListener,
                  PostLoadEventListener,
                  PostInsertEventListener,
                  PostUpdateEventListener,
                  PostDeleteEventListener,
                  PreDeleteEventListener,
                  PreUpdateEventListener {

    private static final long serialVersionUID = 1;

    public static final Collection<String> IGNORED = new HashSet<String>(Arrays.asList("version", "id"));
    public static final String ONLOAD_EVENT = "onLoad";
    public static final String ONLOAD_SAVE = "onSave";
    public static final String BEFORE_LOAD_EVENT = "beforeLoad";
    public static final String BEFORE_INSERT_EVENT = "beforeInsert";
    public static final String AFTER_INSERT_EVENT = "afterInsert";
    public static final String BEFORE_UPDATE_EVENT = "beforeUpdate";
    public static final String AFTER_UPDATE_EVENT = "afterUpdate";
    public static final String BEFORE_DELETE_EVENT = "beforeDelete";
    public static final String AFTER_DELETE_EVENT = "afterDelete";
    public static final String AFTER_LOAD_EVENT = "afterLoad";

    private transient ConcurrentMap<SoftKey<Class<?>>, ClosureEventListener> eventListeners =
        new ConcurrentHashMap<SoftKey<Class<?>>, ClosureEventListener>();
    private transient ConcurrentMap<SoftKey<Class<?>>, Boolean> cachedShouldTrigger =
        new ConcurrentHashMap<SoftKey<Class<?>>, Boolean>();

    boolean failOnError = false;
    List failOnErrorPackages = Collections.EMPTY_LIST;

    public void setConfiguration(ConfigObject co) {
        Object failOnErrorConfig = co.flatten().get("grails.gorm.failOnError");
        if (failOnErrorConfig instanceof List)  {
            failOnError = true;
            failOnErrorPackages = (List)failOnErrorConfig;
        }
        else {
            failOnError = DefaultTypeTransformation.castToBoolean(failOnErrorConfig);
        }
    }

    private ClosureEventListener findEventListener(Object entity) {
        if (entity == null) return null;
        Class<?> clazz = entity.getClass();

        SoftKey<Class<?>> key = new SoftKey<Class<?>>(clazz);
        ClosureEventListener eventListener = eventListeners.get(key);
        if (eventListener != null) {
            return eventListener;
        }

        Boolean shouldTrigger = cachedShouldTrigger.get(key);
        if (shouldTrigger == null || shouldTrigger) {
            synchronized(clazz) {
                eventListener = eventListeners.get(key);
                if (eventListener == null) {
                    shouldTrigger = (entity != null &&
                        (GroovySystem.getMetaClassRegistry().getMetaClass(entity.getClass()) != null) &&
                        (DomainClassArtefactHandler.isDomainClass(clazz) ||
                              AnnotationDomainClassArtefactHandler.isJPADomainClass(clazz)));
                    if (shouldTrigger) {
                        eventListener = new ClosureEventListener(clazz, failOnError, failOnErrorPackages);
                        ClosureEventListener previous = eventListeners.putIfAbsent(key, eventListener);
                        if (previous != null) {
                            eventListener = previous;
                        }
                    }
                    cachedShouldTrigger.put(key, shouldTrigger);
                }
            }
        }
        return eventListener;
    }

    @Override
    public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
        Object entity = event.getObject();
        ClosureEventListener eventListener = findEventListener(entity);
        if (eventListener != null) {
            eventListener.onSaveOrUpdate(event);
        }
        super.onSaveOrUpdate(event);
    }

    public void onPreLoad(PreLoadEvent event) {
        Object entity = event.getEntity();
        GrailsHibernateUtil.ensureCorrectGroovyMetaClass(entity, entity.getClass() );
        ClosureEventListener eventListener=findEventListener(entity);
        if (eventListener != null) {
            eventListener.onPreLoad(event);
        }
    }

    public void onPostLoad(PostLoadEvent event) {
        Object entity = event.getEntity();
        ClosureEventListener eventListener=findEventListener(entity);
        if (eventListener != null) {
            if (applicationContext != null && applicationContext.getAutowireCapableBeanFactory() != null) {
                applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(
                     entity, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
            }
            eventListener.onPostLoad(event);
        }
    }

    public void onPostInsert(PostInsertEvent event) {
        ClosureEventListener eventListener=findEventListener(event.getEntity());
        if (eventListener != null) {
            eventListener.onPostInsert(event);
        }
    }

    public boolean onPreUpdate(PreUpdateEvent event) {
        boolean evict = false;
        ClosureEventListener eventListener=findEventListener(event.getEntity());
        if (eventListener != null) {
            evict = eventListener.onPreUpdate(event);
        }
        return evict;
    }

    public void onPostUpdate(PostUpdateEvent event) {
        ClosureEventListener eventListener=findEventListener(event.getEntity());
        if (eventListener != null) {
            eventListener.onPostUpdate(event);
        }
    }

    public void onPostDelete(PostDeleteEvent event) {
        ClosureEventListener eventListener=findEventListener(event.getEntity());
        if (eventListener != null) {
            eventListener.onPostDelete(event);
        }
    }

    public boolean onPreDelete(PreDeleteEvent event) {
        ClosureEventListener eventListener=findEventListener(event.getEntity());
        if (eventListener != null) {
            return eventListener.onPreDelete(event);
        }
        return false;
    }

    private transient ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // Support for Serialization, not sure if Hibernate really requires Serialization support for Interceptors

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeBoolean(failOnError);
        out.writeObject(failOnErrorPackages);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        failOnError = in.readBoolean();
        failOnErrorPackages = (List)in.readObject();
        eventListeners = new ConcurrentHashMap<SoftKey<Class<?>>, ClosureEventListener>();
        cachedShouldTrigger = new ConcurrentHashMap<SoftKey<Class<?>>, Boolean>();
    }
}
