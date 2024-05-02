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
import grails.util.Metadata
import spock.lang.Specification

@SuppressWarnings("GrMethodMayBeStatic")
class MicronautGroovyPropertySourceLoaderSpec extends Specification implements EnvironmentAwareSpec{

    void setup() {
        resetEnvironment()
    }

    void "test read environment specific property" () {
        setup:
        final URL resource = getClass().getClassLoader().getResource("test-application.groovy")
        final MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()
        environment = Environment.TEST

        when:
        final Map<String, Object> finalMap = groovyPropertySourceLoader.read("test-application.groovy", resource.openStream())

        then:
        finalMap.get("foo.bar") == "test"
    }

    void "test read environment specific property takes precedence"() {
        setup:
        final URL resource = getClass().getClassLoader().getResource("test-application.groovy")
        final MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()

        when:
        final Map<String, Object> finalMap = groovyPropertySourceLoader.read("test-application.groovy", resource.openStream())

        then:
        finalMap.get("foo.bar") == "default"

        when:
        environment = Environment.TEST

        then:
        final Map<String, Object> finalMap2 = groovyPropertySourceLoader.read("test-application.groovy", resource.openStream())
        finalMap2.get("foo.bar") == "test"
    }

    void "test parsing configuration file with DSL"() {

        setup:
        InputStream inputStream = new ByteArrayInputStream(applicationGroovyWithDsl)
        MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()
        Map<String, Object> finalMap = [:]

        when:
        groovyPropertySourceLoader.processInput("test-application.groovy", inputStream, finalMap)

        then:
        finalMap.size() == 1
        finalMap.get("grails.gorm.default.constraints") instanceof Closure
    }

    void "test parsing configuration file unknown variables as assignment"() {
        setup:
        InputStream inputStream = new ByteArrayInputStream(applicationGroovyWithUnknownVars)
        MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()
        Map<String, Object> finalMap = [:]

        when:
        groovyPropertySourceLoader.processInput("test-application.groovy", inputStream, finalMap)

        then:
        finalMap.isEmpty()
    }

    void "test parsing configuration for built-in variables"() {
        setup:
        Metadata.getInstance(new ByteArrayInputStream('''
info:
    app:
      name: test
      version: 0.0
'''.bytes))
        InputStream inputStream = new ByteArrayInputStream(applicationGroovyBuiltInVars)
        MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()
        Map<String, Object> finalMap = [:]

        when:
        groovyPropertySourceLoader.processInput("test-application.groovy", inputStream, finalMap)

        then:
        finalMap.size() == 4
        finalMap.containsKey("grailsHomeVar") & !finalMap.get("grailsHomeVar")
        finalMap.get("userHomeVar")
        finalMap.get("appNameVar")
        finalMap.get("appVersionVar")

        cleanup:
        Metadata.reset()
    }

    void "test nested configurations are flattened"() {

        setup:
        InputStream inputStream = new ByteArrayInputStream(applicationGroovyWithNesting)
        MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()
        Map<String, Object> finalMap = [:]

        when:
        groovyPropertySourceLoader.processInput("test-application.groovy", inputStream, finalMap)

        then:
        finalMap.size() == 2
        finalMap.containsKey("micronaut.http.services.exampleService.url")
        finalMap.containsKey("micronaut.http.services.exampleService.path")
        finalMap.get("micronaut.http.services.exampleService.url")
        finalMap.get("micronaut.http.services.exampleService.path")
    }

    void "test parsing configuration file with duplicated keys"() {
        setup:
        InputStream inputStream = new ByteArrayInputStream(applicationGroovyWithDuplicateEntries)
        MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()
        Map<String, Object> finalMap = [:]

        when:
        groovyPropertySourceLoader.processInput("test-application.groovy", inputStream, finalMap)

        then:
        finalMap.size() == 1
        finalMap.get("micronaut.http.services.exampleService.url") == "http://localhost:8080"
    }

    void "test loading multiple configuration files"() {
        setup:
        InputStream inputStreamWithDsl = new ByteArrayInputStream(applicationGroovyWithDsl)
        InputStream inputSteamBuiltInVars = new ByteArrayInputStream(applicationGroovyBuiltInVars)

        MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()
        Map<String, Object> finalMap = [:]

        when:
        groovyPropertySourceLoader.processInput("test-application.groovy", inputStreamWithDsl, finalMap)
        groovyPropertySourceLoader.processInput("builtin-config.groovy", inputSteamBuiltInVars, finalMap)

        then:
        finalMap.size() == 5
        finalMap.containsKey("grailsHomeVar") & !finalMap.get("grailsHomeVar")
        finalMap.containsKey("appNameVar") & finalMap.get("appNameVar") == Metadata.DEFAULT_APPLICATION_NAME
        finalMap.containsKey("appVersionVar")  & !finalMap.get("appVersionVar")
        finalMap.get("userHomeVar")
        finalMap.get("grails.gorm.default.constraints") instanceof Closure
    }

    private byte[] getApplicationGroovyWithDsl() {
        '''
grails.gorm.default.constraints = {
    '*'(nullable: true, size: 1..20)
}
'''.bytes
    }

    private byte[] getApplicationGroovyWithUnknownVars() {
        '''
my.local.var = undefinedVar
'''.bytes
    }

    private byte[] getApplicationGroovyBuiltInVars() {
        '''
userHomeVar=userHome
grailsHomeVar=grailsHome
appNameVar=appName
appVersionVar=appVersion
'''.bytes
    }

    private byte[] getApplicationGroovyWithNesting() {
'''
micronaut{
    http {
        services{
            exampleService{
                url = "http://localhost:8080"
                path = "/example"
            }
        }
    }
}
'''.bytes
    }

    private byte[] getApplicationGroovyWithDuplicateEntries() {
'''
micronaut.http.services.exampleService.url = "http://localhost:8080"
micronaut.http.services.exampleService.url = "http://localhost:8080"
'''.bytes
    }
}
