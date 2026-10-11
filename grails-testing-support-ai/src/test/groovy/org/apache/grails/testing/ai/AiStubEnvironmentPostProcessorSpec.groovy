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
package org.apache.grails.testing.ai

import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

import spock.lang.Specification

class AiStubEnvironmentPostProcessorSpec extends Specification {

    AiStubEnvironmentPostProcessor postProcessor = new AiStubEnvironmentPostProcessor()
    StandardEnvironment environment = new StandardEnvironment()

    void 'selects no provider for the chat and embedding models and the vector store by default'() {
        when:
        postProcessor.postProcessEnvironment(environment, null)

        then:
        environment.getProperty('spring.ai.model.chat') == 'none'
        environment.getProperty('spring.ai.model.embedding') == 'none'
        environment.getProperty('spring.ai.vectorstore.type') == 'none'
        environment.propertySources.stream().toList().last().name == AiStubEnvironmentPostProcessor.PROPERTY_SOURCE_NAME
    }

    void 'a provider the application selects wins over the default'() {
        given:
        environment.propertySources.addFirst(new MapPropertySource('application', ['spring.ai.model.chat': 'openai', 'spring.ai.vectorstore.type': 'pgvector']))

        when:
        postProcessor.postProcessEnvironment(environment, null)

        then:
        environment.getProperty('spring.ai.model.chat') == 'openai'
        environment.getProperty('spring.ai.model.embedding') == 'none'
        environment.getProperty('spring.ai.vectorstore.type') == 'pgvector'
    }

    void 'does nothing when the stubs are disabled'() {
        given:
        environment.propertySources.addFirst(new MapPropertySource('application', ['grails.ai.stub.enabled': 'false']))

        when:
        postProcessor.postProcessEnvironment(environment, null)

        then:
        environment.getProperty('spring.ai.model.chat') == null
        !environment.propertySources.contains(AiStubEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)
    }

    void 'adds its defaults once'() {
        when:
        postProcessor.postProcessEnvironment(environment, null)
        postProcessor.postProcessEnvironment(environment, null)

        then:
        environment.propertySources.stream().filter { it.name == AiStubEnvironmentPostProcessor.PROPERTY_SOURCE_NAME }.count() == 1
    }

    void 'runs after the configuration has been loaded'() {
        expect:
        postProcessor.order == Integer.MAX_VALUE
    }

}
