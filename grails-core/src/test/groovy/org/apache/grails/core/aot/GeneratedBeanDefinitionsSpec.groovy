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

import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import spock.lang.Specification

/**
 * Covers telling the beans the generated code contributed from the ones declared after it.
 */
class GeneratedBeanDefinitionsSpec extends Specification {

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory()

    void 'a bean whose definition was there when they were recorded is generated'() {
        given:
            beanFactory.registerBeanDefinition('generated', new RootBeanDefinition(String))

        when:
            GeneratedBeanDefinitions.record(beanFactory)

        then:
            GeneratedBeanDefinitions.isGenerated(beanFactory, 'generated')
    }

    void 'a bean declared afterwards is not'() {
        given:
            GeneratedBeanDefinitions.record(beanFactory)

        when:
            beanFactory.registerBeanDefinition('declaredLater', new RootBeanDefinition(String))

        then:
            !GeneratedBeanDefinitions.isGenerated(beanFactory, 'declaredLater')
    }

    void 'a bean declared again in place of a generated one is not'() {
        given: 'a plugin declaring a bean the generated code also contributed, as the bean DSL does at run time'
            beanFactory.registerBeanDefinition('redeclared', new RootBeanDefinition(String))
            GeneratedBeanDefinitions.record(beanFactory)

        when:
            beanFactory.registerBeanDefinition('redeclared', new RootBeanDefinition(StringBuilder))

        then: 'the definition that will be used is not the one the generated code wired'
            !GeneratedBeanDefinitions.isGenerated(beanFactory, 'redeclared')
    }

    void 'nothing is generated where nothing was recorded'() {
        given: 'a context not running on generated artifacts'
            beanFactory.registerBeanDefinition('ordinary', new RootBeanDefinition(String))

        expect:
            !GeneratedBeanDefinitions.isGenerated(beanFactory, 'ordinary')
            !GeneratedBeanDefinitions.isGenerated(null, 'ordinary')
    }

    void 'recording twice keeps what was recorded first'() {
        given:
            beanFactory.registerBeanDefinition('generated', new RootBeanDefinition(String))
            GeneratedBeanDefinitions.record(beanFactory)
            beanFactory.registerBeanDefinition('declaredLater', new RootBeanDefinition(String))

        when:
            GeneratedBeanDefinitions.record(beanFactory)

        then:
            !GeneratedBeanDefinitions.isGenerated(beanFactory, 'declaredLater')
    }
}
