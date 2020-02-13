package grails.boot

import org.springframework.core.env.ConfigurableEnvironment
import spock.lang.Specification

class StaticCompilePropSpec extends Specification {

    void "test that compileStatic system property is set to avoid static compilation exception with application.groovy"() {

        when:
        GrailsApp app = new GrailsApp(GrailsTestConfigurationClass.class)
        app.run()
        ConfigurableEnvironment environment = app.getConfiguredEnvironment()

        then:
        environment
        environment.systemProperties['micronaut.groovy.config.compileStatic'] == "false"
    }
}
