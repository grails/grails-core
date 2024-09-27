package org.grails.plugins.databinding


import org.grails.testing.GrailsUnitTest
import org.springframework.context.ConfigurableApplicationContext
import spock.lang.Specification

class GroovyConfigPropertySourceLoaderSpec extends Specification implements GrailsUnitTest {

    void "test read config from application.groovy from parent Micronaut context"() {

        expect:
        ((ConfigurableApplicationContext) applicationContext.parent).getEnvironment().getProperty("foo", String) == "bar"
    }
}
