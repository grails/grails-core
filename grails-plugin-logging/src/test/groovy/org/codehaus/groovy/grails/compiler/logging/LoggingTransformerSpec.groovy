package org.codehaus.groovy.grails.compiler.logging

import org.apache.commons.logging.Log
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import spock.lang.Specification

class LoggingTransformerSpec extends Specification {

    def "Test added log field"() {
        given:
            def gcl = new GrailsAwareClassLoader()
            def transformer = new LoggingTransformer()
            gcl.classInjectors = [transformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
class LoggingController {
    def index() {
        log.debug "message"
        return log
    }
}
''', "foo/grails-app/controllers/LoggingController.groovy")
            def controller = cls.newInstance()
            Log log = controller.index()

        then:
            log instanceof Log

    }

    def "Test adding log field via Artefact annotation"() {
        given:
            def gcl = new GrailsAwareClassLoader()
        when:
            def cls = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
class LoggingController {
    def index() {
        log.debug "message"
        return log
    }
}
''', "foo/grails-app/controllers/LoggingController.groovy")
            def controller = cls.newInstance()
            Log log = controller.index()

        then:
            log instanceof Log

    }
}
