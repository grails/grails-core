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
package org.codehaus.groovy.grails.orm.hibernate.support

import org.apache.commons.lang.ArrayUtils
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.codehaus.groovy.grails.orm.hibernate.cfg.Mapping
import org.codehaus.groovy.grails.orm.hibernate.events.SaveOrUpdateEventListener
import org.hibernate.EntityMode
import org.hibernate.engine.EntityEntry
import org.hibernate.persister.entity.EntityPersister
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.hibernate.event.*

/**
 * <p>An interceptor that invokes closure events on domain entities such as beforeInsert, beforeUpdate and beforeDelete
 *
 * <p>This class also deals with auto time stamping of domain classes that have properties named 'lastUpdated' and/or 'dateCreated'
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class ClosureEventTriggeringInterceptor extends SaveOrUpdateEventListener implements  ApplicationContextAware,
                                                                                        PreLoadEventListener,
                                                                                        PostLoadEventListener,
                                                                                        PostInsertEventListener,
                                                                                        PostUpdateEventListener,
                                                                                        PostDeleteEventListener,
                                                                                        PreDeleteEventListener,
                                                                                        PreUpdateEventListener{

    public void onSaveOrUpdate(SaveOrUpdateEvent event) {

        def entity = event.getObject()

        if(shouldTrigger(entity)) {
            boolean newEntity = !event.session.contains(entity)
            if(newEntity) {
                triggerEvent(BEFORE_INSERT_EVENT, entity, null)

                def metaClass = entity.metaClass
                Mapping m = GrailsDomainBinder.getMapping(entity.getClass())
                boolean shouldTimestamp = m && !m.autoTimestamp ? false : true

                MetaProperty property = metaClass.hasProperty(entity, GrailsDomainClassProperty.DATE_CREATED)
                def time = System.currentTimeMillis()
                if(property && shouldTimestamp && newEntity) {
                    def now = property.getType().newInstance([time] as Object[] )
                    entity."$property.name" = now
                }
                property = metaClass.hasProperty(entity,GrailsDomainClassProperty.LAST_UPDATED)
                if(property && shouldTimestamp) {
                    def now = property.getType().newInstance([time] as Object[] )
                    entity."$property.name" = now
                }
            }
        }

        super.onSaveOrUpdate event
    }

    private boolean shouldTrigger(entity) {
        return entity && entity?.metaClass!=null && DomainClassArtefactHandler.isDomainClass(entity.class)
    }

    static final String ONLOAD_EVENT = 'onLoad'
    static final String ONLOAD_SAVE = 'onSave'
    static final String BEFORE_LOAD_EVENT = "beforeLoad"
    static final String BEFORE_INSERT_EVENT = 'beforeInsert'
    static final String AFTER_INSERT_EVENT = 'afterInsert'
    static final String BEFORE_UPDATE_EVENT = 'beforeUpdate'
    static final String AFTER_UPDATE_EVENT = 'afterUpdate'
    static final String BEFORE_DELETE_EVENT = 'beforeDelete'
    static final String AFTER_DELETE_EVENT = 'afterDelete'
    static final String AFTER_LOAD_EVENT = "afterLoad"

    public void onPreLoad(PreLoadEvent event) {
        def entity = event.getEntity()

        if(shouldTrigger(entity)) {
            if(entity.metaClass.hasProperty(entity, ONLOAD_EVENT))
                triggerEvent(ONLOAD_EVENT, event.entity, event)
            else if(entity.metaClass.hasProperty(entity, BEFORE_LOAD_EVENT))
                triggerEvent(BEFORE_LOAD_EVENT, event.entity, event)
        }
    }

    public void onPostLoad(PostLoadEvent event) {
        def entity = event.getEntity()

        if(shouldTrigger(entity)) {
            applicationContext?.autowireCapableBeanFactory?.autowireBeanProperties(entity,AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
            triggerEvent(AFTER_LOAD_EVENT, entity, event)
        }
    }

    public void onPostInsert(PostInsertEvent event) {
        if(shouldTrigger(event.entity)) {
            triggerEvent(AFTER_INSERT_EVENT, event.entity, event)
        }
    }

    public boolean onPreUpdate(PreUpdateEvent event) {
        def entity = event.getEntity()
        def evict = false
        if(shouldTrigger(entity)) {
            evict = triggerEvent(BEFORE_UPDATE_EVENT, event.entity, event)

            Mapping m = GrailsDomainBinder.getMapping(entity.getClass())
            boolean shouldTimestamp = m && !m.autoTimestamp ? false : true
            MetaProperty property = entity.metaClass.hasProperty(entity, GrailsDomainClassProperty.LAST_UPDATED)
            if(property && shouldTimestamp) {

                def now = property.getType().newInstance([System.currentTimeMillis()] as Object[] )
                event.getState()[ArrayUtils.indexOf(event.persister.propertyNames, GrailsDomainClassProperty.LAST_UPDATED)] = now;
                entity."$property.name" = now
            }

        }

        if(!entity.validate(deepValidate:false)) {
            evict = true
        }
        return evict
    }

    public void onPostUpdate(PostUpdateEvent event) {
        if(shouldTrigger(event.entity))
            triggerEvent(AFTER_UPDATE_EVENT, event.entity, event)
    }

    public void onPostDelete(PostDeleteEvent event) {
        if(shouldTrigger(event.entity))
            triggerEvent(AFTER_DELETE_EVENT, event.entity, event)
    }

    public boolean onPreDelete(PreDeleteEvent event) {
        if(shouldTrigger(event.entity))
            return triggerEvent(BEFORE_DELETE_EVENT,event.entity, event)
    }

    private transient ApplicationContext applicationContext

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }


    private boolean triggerEvent(String event, entity, Object eventObject) {
        def result = false
        boolean eventTriggered = false
        if(entity.respondsTo(event, [] as Object[])) {
            eventTriggered = true
            result = entity."$event"()
            if(result instanceof Boolean) result = !result
            else {
                result = false
            }            
        }
        else if(entity.hasProperty(event)) {
             eventTriggered = true
             def callable = entity."$event"
             if(callable instanceof Closure) {
                 callable.resolveStrategy = Closure.DELEGATE_FIRST
                 callable.delegate = entity
                 result = callable.call()
                 if(result instanceof Boolean) result = !result
                 else {
                     result = false
                 }
             }
        }

        if(eventTriggered) {
            if(eventObject instanceof PreUpdateEvent) {
                PreUpdateEvent updateEvent = eventObject
                EntityPersister persister = updateEvent.persister
                def propertyNames = persister.propertyNames.toList()
                def state = updateEvent.state
                for(p in propertyNames) {
                    if(['version','id'].contains(p)) continue
                   def i = propertyNames.indexOf(p)
                   def value = entity."$p"
                   state[i] = value
                   persister.setPropertyValue(entity,i,value,EntityMode.POJO)
                }
            }
            else if(eventObject instanceof SaveOrUpdateEvent) {
                SaveOrUpdateEvent updateEvent = eventObject

                if(updateEvent.session.contains(entity)) {
                    EntityEntry entry = updateEvent.getEntry()
                    if(entry) {
                        def propertyNames = entry.persister.propertyNames.toList()
                        def state = entry.loadedState
                        for(p in propertyNames) {
                           if(['version','id'].contains(p)) continue
                           def i = propertyNames.indexOf(p)
                           def value = entity."$p"
                           state[i] = value
                           entry.persister.setPropertyValue(entity,i,value,EntityMode.POJO)
                        }

                    }
                }
            }
        }

        return result

    }



}