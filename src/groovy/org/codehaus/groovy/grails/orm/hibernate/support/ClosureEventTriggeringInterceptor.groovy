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

import org.hibernate.EmptyInterceptor
import org.hibernate.type.Type
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.orm.hibernate.cfg.Mapping
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.springframework.beans.factory.config.AutowireCapableBeanFactory

/**
 * <p>An interceptor that invokes closure events on domain entities such as beforeInsert, beforeUpdate and beforeDelete
 *
 * <p>This class also deals with auto time stamping of domain classes that have properties named 'lastUpdated' and/or 'dateCreated'
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class ClosureEventTriggeringInterceptor extends EmptyInterceptor implements ApplicationContextAware {

    static final String ONLOAD_EVENT = 'onLoad'
    static final String BEFORE_INSERT_EVENT = 'beforeInsert'
    static final String BEFORE_UPDATE_EVENT = 'beforeUpdate'
    static final String BEFORE_DELETE_EVENT = 'beforeDelete'

    private transient ApplicationContext applicationContext

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)  {
        applicationContext?.autowireCapableBeanFactory?.autowireBeanProperties(entity,AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
        return triggerEvent(ONLOAD_EVENT, entity, state, propertyNames.toList())
    }
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        def propertyList = propertyNames.toList()
        def modified = triggerEvent(BEFORE_INSERT_EVENT, entity, state, propertyList)

        def metaClass = entity.metaClass
        Mapping m = GrailsDomainBinder.getMapping(entity.getClass())
        boolean shouldTimestamp = m && !m.autoTimestamp ? false : true

        MetaProperty property = metaClass.hasProperty(entity, GrailsDomainClassProperty.DATE_CREATED)
        def time = System.currentTimeMillis()
        if(property && shouldTimestamp) {
            def now = property.getType().newInstance([time] as Object[] )
            modifyStateForProperty(propertyList, property.name, state, now)
            modified = true
        }
        property = metaClass.hasProperty(entity,GrailsDomainClassProperty.LAST_UPDATED)
        if(property && shouldTimestamp) {
            def now = property.getType().newInstance([time] as Object[] )
            modifyStateForProperty(propertyList, GrailsDomainClassProperty.LAST_UPDATED, state, now)
        }
        modified
    }

    private modifyStateForProperty(List propertyList, String propertyName, state, value) {
        def i = propertyList.indexOf(propertyName)
        if(i > -1) {
            state[i] = value
        }

    }

    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        triggerEvent(BEFORE_DELETE_EVENT, entity, state, propertyNames.toList())
    }

    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types)  {
        List propertyList = propertyNames.toList()
        boolean modified = triggerEvent(BEFORE_UPDATE_EVENT, entity, currentState, propertyList)
        Mapping m = GrailsDomainBinder.getMapping(entity.getClass())
        boolean shouldTimestamp = m && !m.autoTimestamp ? false : true
        MetaProperty property = entity.metaClass.hasProperty(entity, GrailsDomainClassProperty.LAST_UPDATED)
        if(property && shouldTimestamp) {
            def now = property.getType().newInstance([System.currentTimeMillis()] as Object[] )
            modifyStateForProperty(propertyList, GrailsDomainClassProperty.LAST_UPDATED, currentState, now)
            modified = true                
        }

        return modified
    }

    private boolean triggerEvent(event, entity, state, propertyList) {
        if(entity.metaClass.hasProperty(entity, event)) {
             def callable = entity."$event"
             callable.resolveStrategy = Closure.DELEGATE_FIRST
             def capturedProperties = [:]
             for(i in 0..<propertyList.size()) {
                 capturedProperties[propertyList[i]] = state[i]
             }
             callable.delegate = capturedProperties
             callable.call()
             boolean stateModified = false
             for(entry in capturedProperties) {
                   def i = propertyList.indexOf(entry.key)
                   if(i > -1) {
                       state[i] = entry.value
                       stateModified = true
                   }
             }
             return stateModified
         }
        return false

    }



}