/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.grails.common.reflect

import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import org.apache.grails.common.reflect.beans.PublicFieldBean
import org.apache.grails.common.reflect.beans.PublicThing
import org.apache.grails.common.reflect.beans.ThingFactory

import spock.lang.Specification

/**
 * Covers reading a bean whose class is not public. The fixtures deliberately live in another
 * package: in this one the JVM access check passes and nothing would need widening.
 */
class ReflectionUtilsSpec extends Specification {

    void setup() {
        ReflectionUtils.resetWarnedClasses()
    }

    void cleanup() {
        ReflectionUtils.resetWarnedClasses()
    }

    private static Method readMethodOf(Object bean, String property) {
        PropertyDescriptor descriptor = Introspector.getBeanInfo(bean.getClass()).propertyDescriptors
                .find { it.name == property }
        descriptor.readMethod
    }

    void 'a read method declared by a public interface needs no widening'() {
        given:
        def thing = ThingFactory.anonymousThing('thing')
        Method readMethod = readMethodOf(thing, 'name')

        when:
        Method invokable = ReflectionUtils.resolveInvokableReadMethod(readMethod, thing.getClass(), thing)

        then: 'the interface declares it, so it is invokable as it stands'
            invokable.declaringClass == PublicThing
            invokable.invoke(thing) == 'thing'
    }

    void 'a read method with no accessible declaring type is widened on a copy'() {
        given: 'a package-private class implementing nothing, so there is nowhere to resolve to'
        def thing = ThingFactory.standaloneThing('thing')
        Method readMethod = readMethodOf(thing, 'name')

        expect: 'it cannot be invoked from this package to begin with'
            !readMethod.canAccess(thing)

        when:
        Method invokable = ReflectionUtils.resolveInvokableReadMethod(readMethod, thing.getClass(), thing)

        then:
            invokable.invoke(thing) == 'thing'

        and: 'the method handed in was left alone, so a shared descriptor cache keeps its own flag'
            !invokable.is(readMethod)
            !readMethod.canAccess(thing)
    }

    void 'a covariant read method resolves to the override rather than the bridge'() {
        given:
        def thing = ThingFactory.covariantThing('value')
        Method readMethod = readMethodOf(thing, 'value')

        when:
        Method invokable = ReflectionUtils.resolveInvokableReadMethod(readMethod, thing.getClass(), thing)

        then:
            invokable.invoke(thing) == 'value'
            !invokable.bridge
    }

    void 'a field that is already readable is not widened'() {
        given:
        def bean = new PublicFieldBean('label')
        Field field = PublicFieldBean.getDeclaredField('label')

        expect:
            field.canAccess(bean)
            ReflectionUtils.tryMakeReadable(field, bean)
            field.get(bean) == 'label'
    }

    void 'a class that is not public is reported exactly once'() {
        given:
        Class<?> beanClass = ThingFactory.standaloneThing('thing').getClass()

        expect: 'the first call reports it and the second finds it already reported'
            ReflectionUtils.warnOnNonPublicClass(beanClass)
            !ReflectionUtils.warnOnNonPublicClass(beanClass)

        when:
        ReflectionUtils.resetWarnedClasses()

        then: 'the reset is what tests rely on to be independent of each other'
            ReflectionUtils.warnOnNonPublicClass(beanClass)
    }

    void 'a public class is not reported'() {
        expect:
            !ReflectionUtils.warnOnNonPublicClass(PublicFieldBean)
    }

    void 'a class the application could not have declared public is not reported'() {
        expect: 'java.util.KeyValueHolder is not public, but nobody reading the log can act on it'
            !Modifier.isPublic(Map.entry('a', 'b').getClass().modifiers)
            !ReflectionUtils.warnOnNonPublicClass(Map.entry('a', 'b').getClass())

        and:
            !ReflectionUtils.warnOnNonPublicClass(null)
    }
}
