package org.grails.compiler.logging

import org.slf4j.Logger
import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader
import spock.lang.Specification

class LoggingTransformerSpec extends Specification {
    def "Test log field with inheritance and base class with log property"() {
        given:
        def gcl = new GrailsAwareClassLoader()
        def transformer = new LoggingTransformer()
        gcl.classInjectors = [transformer] as ClassInjector[]

        when:
        gcl.parseClass('''
import org.slf4j.Logger
import org.slf4j.LoggerFactory
class BaseController {
    protected Logger log = LoggerFactory.getLogger(getClass())

}
''')
        def cls = gcl.parseClass('''

class LoggingController extends BaseController{
    def index() {
        log.debug "message"
        return log
    }
}
''', "foo/grails-app/controllers/LoggingController.groovy")
        def controller = cls.getDeclaredConstructor().newInstance()
        Logger log = controller.index()

        then:
        log instanceof Logger
    }

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
            def controller = cls.getDeclaredConstructor().newInstance()
            Logger log = controller.index()

        then:
            log instanceof Logger
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
            def controller = cls.getDeclaredConstructor().newInstance()
            Logger log = controller.index()

        then:
            log instanceof Logger

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
            def controller = cls.getDeclaredConstructor().newInstance()
            Logger log = controller.index()

        then:
            log instanceof Logger

    }

    def "Test log field is not added to Application classes"() {
        given:
        def gcl = new GrailsAwareClassLoader()
        when:
        def cls = gcl.parseClass('''
class LoggingController extends grails.boot.config.GrailsAutoConfiguration {
    def index() {
        return log
    }
}
''', "foo/src/main/groovy/LoggingController.groovy")
        def controller = cls.getDeclaredConstructor().newInstance()
        controller.index()

        then:
        thrown(MissingPropertyException)

    }
}
