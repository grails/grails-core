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
package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.EmptyInterceptor
import org.hibernate.type.Type

/**
 * An interceptor that invokes closure events on domain entities
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class ClosureEventTriggeringInterceptor extends EmptyInterceptor {

    static final String ONLOAD_EVENT = 'onLoad'
    static final String BEFORE_INSERT_EVENT = 'beforeInsert'
    static final String BEFORE_UPDATE_EVENT = 'beforeUpdate'
    static final String BEFORE_DELETE_EVENT = 'beforeDelete'

    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)  {
        if(entity.metaClass.hasProperty(entity, ONLOAD_EVENT)) {
            def propertyList = propertyNames.toList()
            def callable = entity."$ONLOAD_EVENT"
            callable.resolveStrategy = Closure.DELEGATE_FIRST
            def capturedProperties = [:]
            callable.delegate = capturedProperties
            callable.call()
            for(entry in capturedProperties) {
                  def i = propertyList.indexOf(entry.key)
                  if(i > -1) {
                      state[i] = entry.value
                  }
            }
        }
    }
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        return triggerEvent(BEFORE_INSERT_EVENT, entity)
    }

    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        triggerEvent(BEFORE_DELETE_EVENT, entity)
    }

    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types)  {
        return triggerEvent(BEFORE_UPDATE_EVENT, entity)
    }

    boolean triggerEvent(event, entity) {
        if(entity.metaClass.hasProperty(entity,event)) {
            def callable = entity."$event"
            if(callable instanceof Closure) {
                def result = callable.call()
                if(result instanceof Boolean) return result
                else return false
            }
        }
        return false

    }

}