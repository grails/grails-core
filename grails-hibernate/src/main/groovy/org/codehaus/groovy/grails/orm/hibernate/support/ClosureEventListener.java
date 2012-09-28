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

import grails.validation.ValidationException;
import groovy.lang.Closure;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder;
import org.codehaus.groovy.grails.orm.hibernate.cfg.Mapping;
import org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractDynamicPersistentMethod;
import org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractSavePersistentMethod;
import org.codehaus.groovy.grails.orm.hibernate.metaclass.BeforeValidateHelper;
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ValidatePersistentMethod;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.grails.datastore.mapping.engine.event.ValidationEvent;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.event.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;

/**
 * <p>Invokes closure events on domain entities such as beforeInsert, beforeUpdate and beforeDelete.
 *
 * <p>Also deals with auto time stamping of domain classes that have properties named 'lastUpdated' and/or 'dateCreated'.
 *
 * @author Lari Hotari
 * @since 1.3.5
 */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class ClosureEventListener implements SaveOrUpdateEventListener,
                                             PreLoadEventListener,
                                             PostLoadEventListener,
                                             PostInsertEventListener,
                                             PostUpdateEventListener,
                                             PostDeleteEventListener,
                                             PreDeleteEventListener,
                                             PreUpdateEventListener {

    private static final long serialVersionUID = 1;
    private static final Log log = LogFactory.getLog(ClosureEventListener.class);
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[] {};

    EventTriggerCaller saveOrUpdateCaller;
    EventTriggerCaller beforeInsertCaller;
    EventTriggerCaller preLoadEventCaller;
    EventTriggerCaller postLoadEventListener;
    EventTriggerCaller postInsertEventListener;
    EventTriggerCaller postUpdateEventListener;
    EventTriggerCaller postDeleteEventListener;
    EventTriggerCaller preDeleteEventListener;
    EventTriggerCaller preUpdateEventListener;
    private BeforeValidateHelper beforeValidateHelper = new BeforeValidateHelper();
    boolean shouldTimestamp = false;
    MetaProperty dateCreatedProperty;
    MetaProperty lastUpdatedProperty;
    MetaClass domainMetaClass;
    boolean failOnErrorEnabled = false;
    MetaProperty errorsProperty;
    Map validateParams;
    MetaMethod validateMethod;

    public ClosureEventListener(Class<?> domainClazz, boolean failOnError, List failOnErrorPackages) {
        domainMetaClass = GroovySystem.getMetaClassRegistry().getMetaClass(domainClazz);
        dateCreatedProperty = domainMetaClass.getMetaProperty(GrailsDomainClassProperty.DATE_CREATED);
        lastUpdatedProperty = domainMetaClass.getMetaProperty(GrailsDomainClassProperty.LAST_UPDATED);
        if (dateCreatedProperty != null || lastUpdatedProperty != null) {
            Mapping m = GrailsDomainBinder.getMapping(domainClazz);
            shouldTimestamp = m == null || m.isAutoTimestamp();
        }

        saveOrUpdateCaller = buildCaller(domainClazz, ClosureEventTriggeringInterceptor.ONLOAD_SAVE);
        beforeInsertCaller = buildCaller(domainClazz, ClosureEventTriggeringInterceptor.BEFORE_INSERT_EVENT);
        preLoadEventCaller = buildCaller(domainClazz, ClosureEventTriggeringInterceptor.ONLOAD_EVENT);
        if (preLoadEventCaller == null) {
            preLoadEventCaller = buildCaller(domainClazz, ClosureEventTriggeringInterceptor.BEFORE_LOAD_EVENT);
        }
        postLoadEventListener = buildCaller(domainClazz, ClosureEventTriggeringInterceptor.AFTER_LOAD_EVENT);
        postInsertEventListener = buildCaller(domainClazz, ClosureEventTriggeringInterceptor.AFTER_INSERT_EVENT);
        postUpdateEventListener = buildCaller(domainClazz, ClosureEventTriggeringInterceptor.AFTER_UPDATE_EVENT);
        postDeleteEventListener = buildCaller(domainClazz, ClosureEventTriggeringInterceptor.AFTER_DELETE_EVENT);
        preDeleteEventListener = buildCaller(domainClazz, ClosureEventTriggeringInterceptor.BEFORE_DELETE_EVENT);
        preUpdateEventListener = buildCaller(domainClazz, ClosureEventTriggeringInterceptor.BEFORE_UPDATE_EVENT);

        if (failOnErrorPackages.size() > 0) {
            failOnErrorEnabled = GrailsClassUtils.isClassBelowPackage(domainClazz, failOnErrorPackages);
        } else {
            failOnErrorEnabled = failOnError;
        }

        validateParams = new HashMap();
        validateParams.put(ValidatePersistentMethod.ARGUMENT_DEEP_VALIDATE, Boolean.FALSE);

        errorsProperty = domainMetaClass.getMetaProperty(AbstractDynamicPersistentMethod.ERRORS_PROPERTY);

        validateMethod = domainMetaClass.getMetaMethod(ValidatePersistentMethod.METHOD_SIGNATURE,
                new Object[] { Map.class });
    }

    private EventTriggerCaller buildCaller(Class<?> domainClazz, String event) {
        Method method = ReflectionUtils.findMethod(domainClazz, event);
        if (method != null) {
            ReflectionUtils.makeAccessible(method);
            return new MethodCaller(method);
        }

        Field field = ReflectionUtils.findField(domainClazz, event);
        if (field != null) {
            ReflectionUtils.makeAccessible(field);
            return new FieldClosureCaller(field);
        }

        MetaMethod metaMethod = domainMetaClass.getMetaMethod(event, EMPTY_OBJECT_ARRAY);
        if (metaMethod != null) {
            return new MetaMethodCaller(metaMethod);
        }

        MetaProperty metaProperty = domainMetaClass.getMetaProperty(event);
        if (metaProperty != null) {
            return new MetaPropertyClosureCaller(metaProperty);
        }

        return null;
    }

    public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
        // no-op, merely a hook for plugins to override
    }

    private void synchronizePersisterState(Object entity, EntityPersister persister, Object[] state) {
        String[] propertyNames = persister.getPropertyNames();
        for (int i = 0; i < propertyNames.length; i++) {
            String p = propertyNames[i];
            MetaProperty metaProperty = domainMetaClass.getMetaProperty(p);
            if (ClosureEventTriggeringInterceptor.IGNORED.contains(p) || metaProperty == null) {
                continue;
            }
            Object value = metaProperty.getProperty(entity);
            state[i] = value;
            persister.setPropertyValue(entity, i, value, EntityMode.POJO);
        }
    }

    public void onPreLoad(final PreLoadEvent event) {
        if (preLoadEventCaller == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                preLoadEventCaller.call(event.getEntity());
                return null;
            }
        });
    }

    public void onPostLoad(final PostLoadEvent event) {
        if (postLoadEventListener == null) {
            return;
        }

       doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postLoadEventListener.call(event.getEntity());
                return null;
            }
        });
    }

    public void onPostInsert(PostInsertEvent event) {
        final Object entity = event.getEntity();
        AbstractSavePersistentMethod.clearDisabledValidations(entity);
        if (postInsertEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postInsertEventListener.call(entity);
                return null;
            }
        });
    }

    public void onPostUpdate(PostUpdateEvent event) {
        final Object entity = event.getEntity();
        AbstractSavePersistentMethod.clearDisabledValidations(entity);
        if (postUpdateEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postUpdateEventListener.call(entity);
                return null;
            }
        });
    }

    public void onPostDelete(PostDeleteEvent event) {
        final Object entity = event.getEntity();
        AbstractSavePersistentMethod.clearDisabledValidations(entity);
        if (postDeleteEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postDeleteEventListener.call(entity);
                return null;
            }
        });
    }

    public boolean onPreDelete(final PreDeleteEvent event) {
        if (preDeleteEventListener == null) {
            return false;
        }

        return doWithManualSession(event, new Closure<Boolean>(this) {
            @Override
            public Boolean call() {
                return preDeleteEventListener.call(event.getEntity());
            }
        });
    }

    public boolean onPreUpdate(final PreUpdateEvent event) {
        return doWithManualSession(event, new Closure<Boolean>(this) {
            @Override
            public Boolean call() {
                Object entity = event.getEntity();
                boolean evict = false;
                if (preUpdateEventListener != null) {
                    evict = preUpdateEventListener.call(entity);
                    synchronizePersisterState(entity, event.getPersister(), event.getState());
                }
                if (lastUpdatedProperty != null && shouldTimestamp) {
                    Object now = DefaultGroovyMethods.newInstance(lastUpdatedProperty.getType(), new Object[] { System
                            .currentTimeMillis() });
                    event.getState()[ArrayUtils.indexOf(event.getPersister().getPropertyNames(), GrailsDomainClassProperty.LAST_UPDATED)] = now;
                    lastUpdatedProperty.setProperty(entity, now);
                }
                if (!AbstractSavePersistentMethod.isAutoValidationDisabled(entity)
                        && !DefaultTypeTransformation.castToBoolean(validateMethod.invoke(entity,
                                new Object[] { validateParams }))) {
                    evict = true;
                    if (failOnErrorEnabled) {
                        Errors errors = (Errors) errorsProperty.getProperty(entity);
                        throw new ValidationException("Validation error whilst flushing entity [" + entity.getClass().getName()
                                + "]", errors);
                    }
                }
                return evict;
            }
        });
    }

    private <T> T doWithManualSession(AbstractEvent event, Closure<T> callable) {
        Session session = event.getSession();
        FlushMode current = session.getFlushMode();
        try {
           session.setFlushMode(FlushMode.MANUAL);
           return callable.call();
        } finally {
            session.setFlushMode(current);
        }
    }

    public boolean onPreInsert(final PreInsertEvent event) {
        return doWithManualSession(event, new Closure<Boolean>(this) {
            @Override
            public Boolean call() {
                Object entity = event.getEntity();
                boolean synchronizeState = false;
                if (beforeInsertCaller != null) {
                    if (beforeInsertCaller.call(entity)) {
                        return true;
                    }
                    synchronizeState = true;
                }
                if (shouldTimestamp) {
                    long time = System.currentTimeMillis();
                    if (dateCreatedProperty != null) {
                        Object now = DefaultGroovyMethods.newInstance(dateCreatedProperty.getType(), new Object[] { time });
                        dateCreatedProperty.setProperty(entity, now);
                        synchronizeState = true;
                    }
                    if (lastUpdatedProperty != null) {
                        Object now = DefaultGroovyMethods.newInstance(lastUpdatedProperty.getType(), new Object[] { time });
                        lastUpdatedProperty.setProperty(entity, now);
                        synchronizeState = true;
                    }
                }

                if (synchronizeState) {
                    synchronizePersisterState(entity, event.getPersister(), event.getState());
                }

                boolean evict = false;
                if (!AbstractSavePersistentMethod.isAutoValidationDisabled(entity)
                        && !DefaultTypeTransformation.castToBoolean(validateMethod.invoke(entity,
                                new Object[] { validateParams }))) {
                    evict = true;
                    if (failOnErrorEnabled) {
                        Errors errors = (Errors) errorsProperty.getProperty(entity);
                        throw new ValidationException("Validation error whilst flushing entity [" + entity.getClass().getName()
                                + "]", errors);
                    }
                }
                return evict;
            }
        });
    }

    public void onValidate(ValidationEvent event) {
        beforeValidateHelper.invokeBeforeValidate(
                event.getEntityObject(), event.getValidatedFields());
    }

    private static abstract class EventTriggerCaller {

        public abstract boolean call(Object entity);

        boolean resolveReturnValue(Object retval) {
            if (retval instanceof Boolean) {
                return !(Boolean)retval;
            }
            return false;
        }
    }

    private static class MethodCaller extends EventTriggerCaller {
        Method method;

        MethodCaller(Method method) {
            this.method = method;
        }

        @Override
        public boolean call(Object entity) {
            Object retval = ReflectionUtils.invokeMethod(method, entity);
            return resolveReturnValue(retval);
        }
    }

    private static class MetaMethodCaller extends EventTriggerCaller {
        MetaMethod method;

        MetaMethodCaller(MetaMethod method) {
            this.method = method;
        }

        @Override
        public boolean call(Object entity) {
            Object retval = method.invoke(entity, EMPTY_OBJECT_ARRAY);
            return resolveReturnValue(retval);
        }
    }

    private static abstract class ClosureCaller extends EventTriggerCaller {
        boolean cloneFirst = false;

        Object callClosure(Object entity, Closure callable) {
            if (cloneFirst) {
                callable = (Closure)callable.clone();
            }
            callable.setResolveStrategy(Closure.DELEGATE_FIRST);
            callable.setDelegate(entity);
            return callable.call();
        }
    }

    private static class FieldClosureCaller extends ClosureCaller {
        Field field;

        FieldClosureCaller(Field field) {
            this.field = field;
            if (Modifier.isStatic(field.getModifiers())) {
                cloneFirst = true;
            }
        }

        @Override
        public boolean call(Object entity) {
            Object fieldval = ReflectionUtils.getField(field, entity);
            if (fieldval instanceof Closure) {
                return resolveReturnValue(callClosure(entity, (Closure) fieldval));
            }
            log.error("Field " + field + " is not Closure or method.");
            return false;
        }
    }

    private static class MetaPropertyClosureCaller extends ClosureCaller {
        MetaProperty metaProperty;

        MetaPropertyClosureCaller(MetaProperty metaProperty) {
            this.metaProperty = metaProperty;
            if (Modifier.isStatic(metaProperty.getModifiers())) {
                cloneFirst = true;
            }
        }

        @Override
        public boolean call(Object entity) {
            Object fieldval = metaProperty.getProperty(entity);
            if (fieldval instanceof Closure) {
                return resolveReturnValue(callClosure(entity, (Closure) fieldval));
            }
            log.error("Field " + metaProperty + " is not Closure.");
            return false;
        }
    }
}
