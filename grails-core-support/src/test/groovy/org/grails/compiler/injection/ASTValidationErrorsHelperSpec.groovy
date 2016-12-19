package org.grails.compiler.injection

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.SourceUnit
import org.grails.compiler.injection.ASTValidationErrorsHelper
import grails.compiler.ast.ClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader
import org.springframework.validation.Errors

import spock.lang.Specification

class ASTValidationErrorsHelperSpec extends Specification {

    static gcl

    void setupSpec() {
        gcl = new GrailsAwareClassLoader()
        def transformer = new ClassInjector() {
            @Override
            void performInjection(SourceUnit source, ClassNode classNode) {
                performInject(source, null, classNode)
            }

            @Override
            void performInjectionOnAnnotatedClass(SourceUnit source, ClassNode classNode) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
                new ASTValidationErrorsHelper().injectErrorsCode(classNode)
            }
            @Override
            boolean shouldInject(URL url) { true }
        }
        gcl.classInjectors = [transformer] as ClassInjector[]
    }

    void 'Test injected errors property'() {
        given:
            def widgetClass = gcl.parseClass('class MyWidget{}')

        when:
            def widget = widgetClass.newInstance()

        then:
            !widget.hasErrors()

        when:
            widget.setErrors([hasErrors: { false }] as Errors)

        then:
            !widget.hasErrors()

        when:
            widget.setErrors([hasErrors: { true }] as Errors)

        then:
            widget.hasErrors()

        when:
            widget.clearErrors()

        then:
            !widget.hasErrors()

        when:
            def localErrors = [:] as Errors
            widget.setErrors(localErrors)

        then:
            localErrors.is(widget.getErrors())
    }
}
