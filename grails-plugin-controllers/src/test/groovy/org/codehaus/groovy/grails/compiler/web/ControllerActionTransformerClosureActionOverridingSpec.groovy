package org.codehaus.groovy.grails.compiler.web

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Specification

class ControllerActionTransformerClosureActionOverridingSpec extends Specification {

    static subclassControllerClass

    void setupSpec() {
        def gcl = new GrailsAwareClassLoader()
        def transformer = new ControllerActionTransformer() {
            @Override
            boolean shouldInject(URL url) { true }
        }
        def transformer2 = new ControllerTransformer() {
            @Override
            boolean shouldInject(URL url) { true }
        }
        gcl.classInjectors = [transformer, transformer2] as ClassInjector[]

        // Make sure this parent controller is compiled before the subclass.  This is relevant to GRAILS-8268
        gcl.parseClass('''
        abstract class MyAbstractController {
            def index = {
                [name: 'Abstract Parent Controller']
            }
        }
''')
        subclassControllerClass = gcl.parseClass('''
        class SubClassController extends MyAbstractController {
            def index = {
                [name: 'Subclass Controller']
            }
        }
''')
    }

    void 'Test overriding closure actions in subclass'() {
        given:
            GrailsWebUtil.bindMockWebRequest()
            def subclassController = subclassControllerClass.newInstance()
            subclassController.instanceControllersApi = new ControllersApi()

        when:
            def model = subclassController.index()

        then:
            'Subclass Controller' == model.name
    }

    def cleanupSpec() {
        RequestContextHolder.setRequestAttributes(null)
    }
}
