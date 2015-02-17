package grails.test.mixin.integration.compiler

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import spock.lang.Issue
import spock.lang.Specification

class IntegrationTestMixinCompilationErrorsSpec extends Specification {

    @Issue('GROOVY-7305')
    void 'test applicationClass is an invalid type'() {
        given:
        def gcl = new GroovyClassLoader()

        when: 'a class marked with @Integration specifies an invalid applicationClass'
        gcl.parseClass('''
package grails.compiler

@grails.test.mixin.integration.Integration(applicationClass=String)
class SomeTestClass {}
''')
        then: 'an error is thrown'
        MultipleCompilationErrorsException e = thrown()
        e.message.contains 'Invalid applicationClass attribute value [java.lang.String].  The applicationClass attribute must specify a class which extends grails.boot.config.GrailsAutoConfiguration.'
    }
}
