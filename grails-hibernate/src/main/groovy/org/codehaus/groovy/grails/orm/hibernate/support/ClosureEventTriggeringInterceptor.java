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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
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
import org.hibernate.LockMode;
import org.hibernate.action.EntityIdentityInsertAction;
import org.hibernate.action.EntityInsertAction;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.Nullability;
import org.hibernate.engine.Status;
import org.hibernate.engine.Versioning;
import org.hibernate.event.*;
import org.hibernate.event.def.AbstractSaveEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ReflectionUtils;

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
                  PreUpdateEventListener,
                  PreInsertEventListener{

    private static final Logger log = LoggerFactory.getLogger(AbstractSaveEventListener.class);
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
    private Method markInterceptorDirtyMethod;

    public ClosureEventTriggeringInterceptor() {
        try {
            this.markInterceptorDirtyMethod = ReflectionUtils.findMethod(AbstractSaveEventListener.class, "markInterceptorDirty", new Class[]{Object.class, EntityPersister.class, EventSource.class});
            ReflectionUtils.makeAccessible(markInterceptorDirtyMethod);
        } catch (Exception e) {
            // ignore
        }
    }

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
        GrailsHibernateUtil.ensureCorrectGroovyMetaClass(entity, entity.getClass());
        ClosureEventListener eventListener=findEventListener(entity);
        if (eventListener != null) {
            eventListener.onPreLoad(event);
        }
    }

    public void onPostLoad(PostLoadEvent event) {
        Object entity = event.getEntity();
        ClosureEventListener eventListener=findEventListener(entity);
        if (eventListener != null) {
            eventListener.onPostLoad(event);
        }
    }

    public void onPostInsert(PostInsertEvent event) {
        ClosureEventListener eventListener=findEventListener(event.getEntity());
        if (eventListener != null) {
            eventListener.onPostInsert(event);
        }
    }

    public boolean onPreInsert(PreInsertEvent event) {
        boolean evict = false;
        ClosureEventListener eventListener=findEventListener(event.getEntity());
        if (eventListener != null) {
            evict = eventListener.onPreInsert(event);
        }
        return evict;
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

    public void setApplicationContext(ApplicationContext applicationContext) {
        // not used
    }

    // Support for Serialization, not sure if Hibernate really requires Serialization support for Interceptors

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(failOnError);
        out.writeObject(failOnErrorPackages);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        failOnError = in.readBoolean();
        failOnErrorPackages = (List)in.readObject();
        eventListeners = new ConcurrentHashMap<SoftKey<Class<?>>, ClosureEventListener>();
        cachedShouldTrigger = new ConcurrentHashMap<SoftKey<Class<?>>, Boolean>();
    }

    /*
     * TODO: This is a horrible hack due to a bug in Hibernate's post-insert event processing (HHH-3904)
     */
    @Override
    protected Serializable performSaveOrReplicate(Object entity, EntityKey key, EntityPersister persister, boolean useIdentityColumn, Object anything, EventSource source, boolean requiresImmediateIdAccess) {
        validate(entity, persister, source);

        Serializable id = key == null ? null : key.getIdentifier();

        boolean inTxn = source.getJDBCContext().isTransactionInProgress();
        boolean shouldDelayIdentityInserts = !inTxn && !requiresImmediateIdAccess;

        // Put a placeholder in entries, so we don't recurse back and try to save() the
        // same object again. QUESTION: should this be done before onSave() is called?
        // likewise, should it be done before onUpdate()?
        source.getPersistenceContext().addEntry(
                entity,
                Status.SAVING,
                null,
                null,
                id,
                null,
                LockMode.WRITE,
                useIdentityColumn,
                persister,
                false,
                false);

        cascadeBeforeSave(source, persister, entity, anything);

        if (useIdentityColumn && !shouldDelayIdentityInserts) {
            log.trace("executing insertions");
            source.getActionQueue().executeInserts();
        }

        Object[] values = persister.getPropertyValuesToInsert(entity, getMergeMap(anything), source);
        Type[] types = persister.getPropertyTypes();

        boolean substitute = substituteValuesIfNecessary(entity, id, values, persister, source);

        if (persister.hasCollections()) {
            substitute = substitute || visitCollectionsBeforeSave(entity, id, values, types, source);
        }

        if (substitute) {
            persister.setPropertyValues(entity, values, source.getEntityMode());
        }

        TypeHelper.deepCopy(
                values,
                types,
                persister.getPropertyUpdateability(),
                values,
                source);

        new ForeignKeys.Nullifier(entity, false, useIdentityColumn, source)
                .nullifyTransientReferences(values, types);
        new Nullability(source).checkNullability(values, persister, false);

        if (useIdentityColumn) {
            EntityIdentityInsertAction insert = new EntityIdentityInsertAction(
                    values, entity, persister, source, shouldDelayIdentityInserts);
            if (!shouldDelayIdentityInserts) {
                log.debug("executing identity-insert immediately");
                source.getActionQueue().execute(insert);
                id = insert.getGeneratedId();
                if (id != null) {
                    // As of HHH-3904, if the id is null the operation was vetoed so we bail
                    key = new EntityKey(id, persister, source.getEntityMode());
                    source.getPersistenceContext().checkUniqueness(key, entity);
                }
            }
            else {
                log.debug("delaying identity-insert due to no transaction in progress");
                source.getActionQueue().addAction(insert);

                key = insert.getDelayedEntityKey();
            }
        }

        if (key != null) {
            Object version = Versioning.getVersion(values, persister);
            source.getPersistenceContext().addEntity(
                    entity,
                    (persister.isMutable() ? Status.MANAGED : Status.READ_ONLY),
                    values,
                    key,
                    version,
                    LockMode.WRITE,
                    useIdentityColumn,
                    persister,
                    isVersionIncrementDisabled(),
                    false);
            //source.getPersistenceContext().removeNonExist(new EntityKey(id, persister, source.getEntityMode()));

            if (!useIdentityColumn) {
                source.getActionQueue().addAction(
                        new EntityInsertAction(id, values, entity, version, persister, source));
            }

            cascadeAfterSave(source, persister, entity, anything);
            // Very unfortunate code, but markInterceptorDirty is private. Once HHH-3904 is resolved remove this overridden method!
            if (markInterceptorDirtyMethod != null) {
                ReflectionUtils.invokeMethod(markInterceptorDirtyMethod, this,new Object[]{entity, persister, source});
            }
        }

        return id;
    }
}
