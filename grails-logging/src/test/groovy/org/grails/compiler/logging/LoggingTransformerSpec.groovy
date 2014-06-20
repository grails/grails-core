package org.grails.compiler.logging

import org.apache.commons.logging.Log
import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader
import spock.lang.Specification

class LoggingTransformerSpec extends Specification {

    def "Test log field with inheritance"() {
        given:
            def gcl = new GrailsAwareClassLoader()
            def transformer = new LoggingTransformer()
            gcl.classInjectors = [transformer] as ClassInjector[]

        when:
            gcl.parseClass('''
class BaseController {}
''')
            def cls = gcl.parseClass('''

class LoggingController extends BaseController{
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
