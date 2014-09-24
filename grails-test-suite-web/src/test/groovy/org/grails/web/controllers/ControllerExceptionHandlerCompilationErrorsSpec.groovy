package org.grails.web.controllers

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader
import org.grails.compiler.web.ControllerActionTransformer

import spock.lang.Specification

class ControllerExceptionHandlerCompilationErrorsSpec extends Specification {

    static gcl

    void setupSpec() {
        gcl = new GrailsAwareClassLoader()
        def transformer = new ControllerActionTransformer() {
            @Override
            boolean shouldInject(URL url) { true }
        }
        gcl.classInjectors = [transformer]as ClassInjector[]
    }

    void 'Test multiple exception handlers for the same exception type'() {
        when: 'Two handlers exist for the same exception type'
            gcl.parseClass('''
            class TestController {
                def methodOne(NumberFormatException e){}
                def methodTwo(NumberFormatException e){}
            }
''')
        then: 'compilation fails'
            MultipleCompilationErrorsException e = thrown()
            e.message.contains 'A controller may not define more than 1 exception handler for a particular exception type.  [TestController] defines the [methodOne] and [methodTwo] exception handlers which each accept a [java.lang.NumberFormatException] which is not allowed.'
    }
}
