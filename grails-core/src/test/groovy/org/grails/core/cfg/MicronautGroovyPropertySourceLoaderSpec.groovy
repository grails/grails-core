package org.grails.core.cfg

import grails.util.Metadata
import org.grails.config.NavigableMap
import spock.lang.Specification

@SuppressWarnings("GrMethodMayBeStatic")
class MicronautGroovyPropertySourceLoaderSpec extends Specification {

    void "test parsing configuration file with DSL"() {

        setup:
        InputStream inputStream = new ByteArrayInputStream(applicationGroovyWithDsl)
        MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()
        Map<String, Object> finalMap = [:]

        when:
        groovyPropertySourceLoader.processInput("test-application.groovy", inputStream, finalMap)

        then:
        noExceptionThrown()
        finalMap.get("grails") instanceof NavigableMap
        finalMap.get("grails.gorm.default.constraints")
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
        noExceptionThrown()
        finalMap.containsKey("undefinedVar")
        !finalMap.get("undefinedVar")
        finalMap.containsKey("my.local.var")
        !finalMap.get("my.local.var")
        finalMap.get("my") instanceof NavigableMap
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
        noExceptionThrown()
        finalMap.get("userHomeVar")
        finalMap.get("appNameVar")
        finalMap.get("appVersionVar")

        cleanup:
        Metadata.reset()
    }

    void "test parsing configuration file with camel cased keys"() {
        setup:
        InputStream inputStream = new ByteArrayInputStream(applicationGroovyWithCamelCaseVars)
        MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()
        Map<String, Object> finalMap = [:]

        when:
        groovyPropertySourceLoader.processInput("test-application.groovy", inputStream, finalMap)

        then:
        noExceptionThrown()
        finalMap.containsKey("micronaut.http.services.example-service.url")
        finalMap.containsKey("micronaut.http.services.example-service.path")
        finalMap.get("micronaut.http.services.example-service.url")
        finalMap.get("micronaut.http.services.example-service.path")
    }

    void "test parsing configuration file with flattened camel cased keys"() {
        setup:
        InputStream inputStream = new ByteArrayInputStream(applicationGroovyWithFlatCamelCaseVars)
        MicronautGroovyPropertySourceLoader groovyPropertySourceLoader = new MicronautGroovyPropertySourceLoader()
        Map<String, Object> finalMap = [:]

        when:
        groovyPropertySourceLoader.processInput("test-application.groovy", inputStream, finalMap)

        then:
        noExceptionThrown()
        finalMap.containsKey("micronaut.http.services.example-service.url")
        finalMap.containsKey("micronaut.http.services.example-service.path")
        finalMap.get("micronaut.http.services.example-service.url")
        finalMap.get("micronaut.http.services.example-service.path")
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

    private byte[] getApplicationGroovyWithCamelCaseVars() {
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

    private byte[] getApplicationGroovyWithFlatCamelCaseVars() {
'''
micronaut.http.services.exampleService.url = "http://localhost:8080"
micronaut.http.services.exampleService.path = "/example"
'''.bytes
    }
}
