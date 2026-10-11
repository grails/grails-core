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
package org.grails.orm.hibernate.proxy

import org.hibernate.HibernateException
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper
import spock.lang.Specification

class ByteBuddyGroovyProxyFactorySpec extends Specification {

    static class FakeEntity { }

    static class FakeHibernateProxy extends FakeEntity { }

    void "factory can be instantiated"() {
        expect:
        new ByteBuddyGroovyProxyFactory(Mock(ByteBuddyProxyHelper)) != null
    }

    void "postInstantiate configures entity name, class, and interfaces"() {
        given:
        def helper = Mock(ByteBuddyProxyHelper) {
            buildProxy(_, _ as Class[]) >> FakeHibernateProxy
        }
        def factory = new ByteBuddyGroovyProxyFactory(helper)

        when:
        factory.postInstantiate("FakeEntity", FakeEntity, [] as Set, null, null, null)

        then:
        noExceptionThrown()
    }

    void "postInstantiate wraps the Hibernate proxy class so getMetaClass() can answer before the interceptor is assigned"() {
        given:
        def helper = Mock(ByteBuddyProxyHelper) {
            buildProxy(_, _ as Class[]) >> FakeHibernateProxy
        }
        def factory = new ByteBuddyGroovyProxyFactory(helper)

        when:
        factory.postInstantiate("FakeEntity", FakeEntity, [] as Set, null, null, null)

        then: "the proxy class is a subclass of the Hibernate-built proxy that overrides getMetaClass()"
        def proxyClass = proxyClassOf(factory)
        proxyClass != FakeHibernateProxy
        FakeHibernateProxy.isAssignableFrom(proxyClass)
        proxyClass.getMethod("getMetaClass").declaringClass == proxyClass
    }

    void "postInstantiate keeps the Hibernate proxy class for non-Groovy entities"() {
        given:
        def helper = Mock(ByteBuddyProxyHelper) {
            buildProxy(_, _ as Class[]) >> Object
        }
        def factory = new ByteBuddyGroovyProxyFactory(helper)

        when:
        factory.postInstantiate("PlainEntity", Object, [] as Set, null, null, null)

        then:
        proxyClassOf(factory) == Object
    }

    void "getProxy wraps instantiation failure in HibernateException"() {
        given: "a factory where proxyClass cannot be cast to HibernateProxy"
        def helper = Mock(ByteBuddyProxyHelper) {
            buildProxy(_, _ as Class[]) >> FakeHibernateProxy
        }
        def factory = new ByteBuddyGroovyProxyFactory(helper)
        factory.postInstantiate("FakeEntity", FakeEntity, [] as Set, null, null, null)
        def session = Mock(SharedSessionContractImplementor)

        when: "getProxy is called - FakeHibernateProxy cannot be cast to HibernateProxy"
        factory.getProxy(1L, session)

        then:
        def e = thrown(HibernateException)
        e.message.contains("FakeEntity")
    }

    private static Class<?> proxyClassOf(ByteBuddyGroovyProxyFactory factory) {
        def field = ByteBuddyGroovyProxyFactory.getDeclaredField("proxyClass")
        field.accessible = true
        field.get(factory) as Class<?>
    }
}
