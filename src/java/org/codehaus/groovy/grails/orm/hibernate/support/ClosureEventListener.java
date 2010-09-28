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
import org.codehaus.groovy.grails.orm.hibernate.metaclass.ValidatePersistentMethod;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.EntityEntry;
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
import org.hibernate.event.SaveOrUpdateEventListener;
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
public class ClosureEventListener implements SaveOrUpdateEventListener, PreLoadEventListener, PostLoadEventListener,
		PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener, PreDeleteEventListener,
		PreUpdateEventListener {
	private static final long serialVersionUID = 1L;
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
	boolean shouldTimestamp = false;
	MetaProperty dateCreatedProperty;
	MetaProperty lastUpdatedProperty;
	MetaClass domainMetaClass;
	boolean failOnErrorEnabled = false;
	MetaProperty errorsProperty;
	@SuppressWarnings("unchecked")
	Map validateParams;
	MetaMethod validateMethod;

	@SuppressWarnings("unchecked")
	public ClosureEventListener(Class<?> domainClazz, boolean failOnError, List failOnErrorPackages) {
		initialize(domainClazz, failOnError, failOnErrorPackages);
	}

	@SuppressWarnings("unchecked")
	private void initialize(Class<?> domainClazz, boolean failOnError, List failOnErrorPackages) {
		domainMetaClass = GroovySystem.getMetaClassRegistry().getMetaClass(domainClazz);
		dateCreatedProperty = domainMetaClass.getMetaProperty(GrailsDomainClassProperty.DATE_CREATED);
		lastUpdatedProperty = domainMetaClass.getMetaProperty(GrailsDomainClassProperty.LAST_UPDATED);
		if (dateCreatedProperty != null || lastUpdatedProperty != null) {
			Mapping m = GrailsDomainBinder.getMapping(domainClazz);
			shouldTimestamp = (m != null && !m.isAutoTimestamp()) ? false : true;
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

		if(failOnErrorPackages.size() > 0) {
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

	@SuppressWarnings("unchecked")
	public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
		Object entity = event.getObject();
		boolean newEntity = !event.getSession().contains(entity);
		if (newEntity) {
			if (beforeInsertCaller != null) {
				beforeInsertCaller.call(entity);
				if (event.getSession().contains(entity)) {
					EntityEntry entry = event.getEntry();
					if (entry != null) {
						Object[] state = entry.getLoadedState();
						synchronizePersisterState(entity, entry.getPersister(), state);
					}
				}
			}
			if (shouldTimestamp) {
				long time = System.currentTimeMillis();
				if (dateCreatedProperty != null && newEntity) {
					Object now = DefaultGroovyMethods.newInstance(dateCreatedProperty.getType(), new Object[] { time });
					dateCreatedProperty.setProperty(entity, now);
				}
				if (lastUpdatedProperty != null) {
					Object now = DefaultGroovyMethods.newInstance(lastUpdatedProperty.getType(), new Object[] { time });
					lastUpdatedProperty.setProperty(entity, now);
				}
			}
		}
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

	public void onPreLoad(PreLoadEvent event) {
		if(preLoadEventCaller != null) {
			preLoadEventCaller.call(event.getEntity());
		}
	}

	public void onPostLoad(PostLoadEvent event) {
		if (postLoadEventListener != null) {
			postLoadEventListener.call(event.getEntity());
		}
	}

	public void onPostInsert(PostInsertEvent event) {
		if (postInsertEventListener != null) {
			postInsertEventListener.call(event.getEntity());
		}
	}

	public void onPostUpdate(PostUpdateEvent event) {
		if (postUpdateEventListener != null) {
			postUpdateEventListener.call(event.getEntity());
		}
	}

	public void onPostDelete(PostDeleteEvent event) {
		if (postDeleteEventListener != null) {
			postDeleteEventListener.call(event.getEntity());
		}
	}

	public boolean onPreDelete(PreDeleteEvent event) {
		if (preDeleteEventListener != null) {
			return preDeleteEventListener.call(event.getEntity());
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean onPreUpdate(PreUpdateEvent event) {
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

	private static abstract class EventTriggerCaller {
		EventTriggerCaller() {

		}

		public abstract boolean call(Object entity);

		boolean resolveReturnValue(Object retval) {
			if (retval instanceof Boolean) {
				return !((Boolean) retval).booleanValue();
			} else {
				return false;
			}
		}
	}

	private static class MethodCaller extends EventTriggerCaller {
		Method method;

		MethodCaller(Method method) {
			this.method = method;
		}

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

		public boolean call(Object entity) {
			Object retval = method.invoke(entity, EMPTY_OBJECT_ARRAY);
			return resolveReturnValue(retval);
		}
	}

	private static abstract class ClosureCaller extends EventTriggerCaller {
		boolean cloneFirst=false;
		
		Object callClosure(Object entity, Closure callable) {
			if(cloneFirst) {
				callable=(Closure)callable.clone();
			}
			callable.setResolveStrategy(Closure.DELEGATE_FIRST);
			callable.setDelegate(entity);
			Object retval = callable.call();
			return retval;
		}
	}

	private static class FieldClosureCaller extends ClosureCaller {
		Field field;

		FieldClosureCaller(Field field) {
			this.field = field;
			if(Modifier.isStatic(field.getModifiers())) {
				cloneFirst=true;
			}
		}

		@Override
		public boolean call(Object entity) {
			Object fieldval = ReflectionUtils.getField(field, entity);
			if (fieldval instanceof Closure) {
				return resolveReturnValue(callClosure(entity, (Closure) fieldval));
			} else {
				log.error("Field " + field + " is not Closure or method.");
				return false;
			}
		}
	}

	private static class MetaPropertyClosureCaller extends ClosureCaller {
		MetaProperty metaProperty;

		MetaPropertyClosureCaller(MetaProperty metaProperty) {
			this.metaProperty = metaProperty;
			if(Modifier.isStatic(metaProperty.getModifiers())) {
				cloneFirst=true;
			}
		}

		@Override
		public boolean call(Object entity) {
			Object fieldval = metaProperty.getProperty(entity);
			if (fieldval instanceof Closure) {
				return resolveReturnValue(callClosure(entity, (Closure) fieldval));
			} else {
				log.error("Field " + metaProperty + " is not Closure.");
				return false;
			}
		}

	}
}
