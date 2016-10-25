package org.grails.compiler.web

import grails.compiler.ast.ClassInjector
import grails.compiler.traits.ControllerTraitInjector
import grails.compiler.traits.TraitInjector
import grails.util.GrailsWebMockUtil

import org.grails.compiler.injection.GrailsAwareClassLoader
import org.grails.compiler.web.ControllerActionTransformer
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
        gcl.classInjectors = [transformer] as ClassInjector[]

        // Make sure this parent controller is compiled before the subclass.  This is relevant to GRAILS-8268
        gcl.parseClass('''
        @grails.artefact.Artefact('Controller')
        abstract class MyAbstractController {
            def index = {
                [name: 'Abstract Parent Controller']
            }
        }
''')
        subclassControllerClass = gcl.parseClass('''
        @grails.artefact.Artefact('Controller')
        class SubClassController extends MyAbstractController {
            def index = {
                [name: 'Subclass Controller']
            }
        }
''')
    }

    void 'Test overriding closure actions in subclass'() {
        given:
            GrailsWebMockUtil.bindMockWebRequest()
            def subclassController = subclassControllerClass.newInstance()

        when:
            def model = subclassController.index()

        then:
            'Subclass Controller' == model.name
    }

    def cleanupSpec() {
        RequestContextHolder.resetRequestAttributes()
    }
}
