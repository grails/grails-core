/*
 * Copyright 2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.core.cfg

import grails.util.Environment
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class MicronautYamlPropertySourceLoaderSpec extends Specification implements EnvironmentAwareSpec, MicronautContextAwareSpec {

    void cleanup() {
        resetEnvironment()
    }

    void "test reading yaml configuration from envrionment specific block takes precendenc regardless of the order"() {
        setup:
        environment = Environment.TEST
        ApplicationContext context = micronautContextBuilder()
                .build()
                .start()

        expect:
        context.getProperty('dataSources.testDb.url', String).get() == "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
    }

    void "test reading micronaut configuration"() {
        setup:
        ApplicationContext context = micronautContextBuilder()
                .build()
                .start()

        expect:
        context.getProperty('dataSources.testDb.url', String).get() == "jdbc:h2:mem:testDbDefault;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
    }

    void "test reading yaml configuration"() {
        setup:
        final MicronautYamlPropertySourceLoader loader = new MicronautYamlPropertySourceLoader()
        final URL resource = getClass().getClassLoader().getResource("foo-plugin-environments.yml")

        when:
        environment = Environment.TEST
        final Map<String, Object> finalMap = loader.read("foo-plugin-environments.yml", resource.openStream())

        then:
        finalMap.get("dataSources.testDb.url") == "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
    }

    void "test that it only reads environment specific entries"() {
        setup:
        final MicronautYamlPropertySourceLoader loader = new MicronautYamlPropertySourceLoader()
        final URL resource = getClass().getClassLoader().getResource("foo-plugin-environments.yml")

        when:
        environment = null
        final Map<String, Object> finalMap = loader.read("foo-plugin-environments.yml", resource.openStream())

        then:
        finalMap.get('dataSources.testDb.url') == null

    }
}
