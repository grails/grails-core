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
package org.apache.grails.core.aot

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.cglib.proxy.Enhancer
import org.springframework.cglib.proxy.NoOp
import spock.lang.Specification

/**
 * Covers telling the beans the generated code injected from the ones it did not: those declared after it,
 * and those of a class it never saw, of which it injected only the members of the type it declared.
 */
class GeneratedBeanDefinitionsSpec extends Specification {

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory()

    void 'a bean of the type its generated definition declares was injected by the generated code'() {
        given:
            beanFactory.registerBeanDefinition('generated', new RootBeanDefinition(ConfigurationFixture))

        when:
            GeneratedBeanDefinitions.record(beanFactory)

        then:
            GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'generated', ConfigurationFixture)
    }

    void 'so was the subclass Spring generates for a configuration class'() {
        given:
            beanFactory.registerBeanDefinition('generated', new RootBeanDefinition(ConfigurationFixture))
            GeneratedBeanDefinitions.record(beanFactory)
            Class<?> enhanced = new Enhancer(superclass: ConfigurationFixture, callback: NoOp.INSTANCE).create().getClass()

        expect:
            enhanced != ConfigurationFixture
            GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'generated', enhanced)
    }

    void 'the type a factory method product is declared as is the one compared'() {
        given: 'a factory method product, whose definition holds the factory class and the type it returns'
            beanFactory.registerBeanDefinition('product', new RootBeanDefinition(ConfigurationFixture).tap {
                targetType = ProductFixture
            })
            GeneratedBeanDefinitions.record(beanFactory)

        expect:
            GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'product', ProductFixture)
            !GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'product', ConfigurationFixture)
    }

    void 'a bean of another class than its definition declares was not'() {
        given: 'a factory method declaring an interface, as the link generator is declared'
            beanFactory.registerBeanDefinition('declaredAsInterface', new RootBeanDefinition(ProductType))
            GeneratedBeanDefinitions.record(beanFactory)

        expect: 'the generated code never saw the members of the class it returns'
            !GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'declaredAsInterface', ProductFixture)
    }

    void 'a bean declared afterwards was not'() {
        given:
            GeneratedBeanDefinitions.record(beanFactory)

        when:
            beanFactory.registerBeanDefinition('declaredLater', new RootBeanDefinition(ConfigurationFixture))

        then:
            !GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'declaredLater', ConfigurationFixture)
    }

    void 'a bean declared again in place of a generated one was not'() {
        given: 'a plugin declaring a bean the generated code also contributed, as the bean DSL does at run time'
            beanFactory.registerBeanDefinition('redeclared', new RootBeanDefinition(ConfigurationFixture))
            GeneratedBeanDefinitions.record(beanFactory)

        when:
            beanFactory.registerBeanDefinition('redeclared', new RootBeanDefinition(ConfigurationFixture))

        then: 'the definition that will be used is not the one the generated code wired'
            !GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'redeclared', ConfigurationFixture)
    }

    void 'a definition that declares no type is left to the processors'() {
        given:
            beanFactory.registerBeanDefinition('untyped', new RootBeanDefinition())
            GeneratedBeanDefinitions.record(beanFactory)

        expect:
            !GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'untyped', ConfigurationFixture)
    }

    void 'nothing was injected by generated code where nothing was recorded'() {
        given: 'a context not running on generated artifacts'
            beanFactory.registerBeanDefinition('ordinary', new RootBeanDefinition(ConfigurationFixture))

        expect:
            !GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'ordinary', ConfigurationFixture)
            !GeneratedBeanDefinitions.isInjectedByGeneratedCode(null, 'ordinary', ConfigurationFixture)
    }

    void 'recording twice keeps what was recorded first'() {
        given:
            beanFactory.registerBeanDefinition('generated', new RootBeanDefinition(ConfigurationFixture))
            GeneratedBeanDefinitions.record(beanFactory)
            beanFactory.registerBeanDefinition('declaredLater', new RootBeanDefinition(ConfigurationFixture))

        when:
            GeneratedBeanDefinitions.record(beanFactory)

        then:
            GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'generated', ConfigurationFixture)
            !GeneratedBeanDefinitions.isInjectedByGeneratedCode(beanFactory, 'declaredLater', ConfigurationFixture)
    }

    void 'the members the generated code injected are marked on the definition of a bean of a subclass'() {
        given: 'a factory method product declared as a superclass of the class it returns'
            beanFactory.registerBeanDefinition('product', new RootBeanDefinition(SuperclassFixture))
            GeneratedBeanDefinitions.record(beanFactory)
            def definition = new RootBeanDefinition(SuperclassFixture)

        when:
            GeneratedBeanDefinitions.markMembersInjectedByGeneratedCode(beanFactory, 'product', SubclassFixture,
                    definition, new AutowiredAnnotationBeanPostProcessor())

        then: 'those of the superclass, and the override of one that the generated code called, not what only the subclass declares'
            definition.externallyManagedConfigMembers == [
                    SuperclassFixture.getDeclaredField('collaborator'),
                    SuperclassFixture.getMethod('setOverridable', String),
                    SubclassFixture.getMethod('setOverridable', String)
            ] as Set
    }

    void 'nothing is marked when #reason'() {
        given:
            beanFactory.registerBeanDefinition('generated', new RootBeanDefinition(SuperclassFixture))
            GeneratedBeanDefinitions.record(beanFactory)
            beanFactory.registerBeanDefinition('declaredLater', new RootBeanDefinition(SuperclassFixture))
            def definition = new RootBeanDefinition(SuperclassFixture)

        when:
            GeneratedBeanDefinitions.markMembersInjectedByGeneratedCode(beanFactory, beanName, beanType, definition,
                    new AutowiredAnnotationBeanPostProcessor())

        then:
            definition.externallyManagedConfigMembers.empty

        where:
            beanName        | beanType             | reason
            'generated'     | SuperclassFixture    | 'the bean is of the declared type, which the processors leave alone'
            'declaredLater' | SubclassFixture      | 'the definition was not generated'
            'generated'     | ConfigurationFixture | 'the bean is not of the declared type at all'
    }

    static class ConfigurationFixture {
    }

    static class SuperclassFixture {

        @Autowired
        protected String collaborator

        @Autowired
        void setOverridable(String collaborator) {
        }
    }

    static class SubclassFixture extends SuperclassFixture {

        @Override
        @Autowired
        void setOverridable(String collaborator) {
        }

        @Autowired
        void setOwn(String collaborator) {
        }
    }

    static interface ProductType {
    }

    static class ProductFixture implements ProductType {
    }
}
