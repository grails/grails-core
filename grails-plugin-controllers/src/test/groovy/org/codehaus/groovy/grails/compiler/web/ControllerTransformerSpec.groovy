package org.codehaus.groovy.grails.compiler.web

import grails.util.GrailsWebUtil

import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Specification

class ControllerTransformerSpec extends Specification {

    void "Test that transformation does not generate methods that conflict with getters and setters for properties defined in the class"() {
        given:
            def gcl = new GrailsAwareClassLoader()
            def transformer = new ControllerTransformer() {
                @Override
                boolean shouldInject(URL url) { true }
            }
            gcl.classInjectors = [transformer] as ClassInjector[]

            def grailsApplicationControllerClass = gcl.parseClass('''
            class GrailsApplicationController {
                List grailsApplication
            }
            ''')

            def noGrailsApplicationControllerClass = gcl.parseClass('''
            class NoGrailsApplicationController {
            }
            ''')

        when:
            def getterMethods = noGrailsApplicationControllerClass.metaClass.methods.findAll { 'getGrailsApplication' == it.name }
            def setterMethods = noGrailsApplicationControllerClass.metaClass.methods.findAll { 'setGrailsApplication' == it.name }

        then:
            1 == getterMethods?.size()
            GrailsApplication == getterMethods[0].returnType
            0 == setterMethods?.size()

        when:
            getterMethods = grailsApplicationControllerClass.metaClass.methods.findAll { 'getGrailsApplication' == it.name }
            setterMethods = grailsApplicationControllerClass.metaClass.methods.findAll { 'setGrailsApplication' == it.name }

        then:
            1 == getterMethods?.size()
            List == getterMethods[0].returnType
            1 == setterMethods?.size()
            1 == setterMethods[0].paramsCount
            List == setterMethods[0].parameterTypes[0].theClass
    }

    void "Test get artefact type"() {
        when:
            def transformer = new ControllerTransformer()

        then:
            transformer.artefactType == 'Controller'
    }

    void "Test that the API is injected via AST"() {

        given:
            def gcl = new GrailsAwareClassLoader()
            def transformer = new ControllerTransformer() {
                @Override
                boolean shouldInject(URL url) { true }
            }
            gcl.classInjectors = [transformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
class TestTransformedController {}
''')
            def controller = cls.newInstance()

        then:

            controller != null
            controller.instanceControllersApi != null

        when:
            GrailsWebUtil.bindMockWebRequest()

        then:
            controller.getRequest() instanceof HttpServletRequest
            controller.request instanceof HttpServletRequest

        when:
            controller.render(view:"foo")

        then:
            controller.modelAndView != null
    }

    void "Test annotated artefact"() {
         when:
            def gcl = new GrailsAwareClassLoader()
            def cls = gcl.parseClass('''
@grails.artefact.Artefact("Controller")
class AnnotatedControllerTransformerController {

}
''')
            def controller = cls.newInstance()

        then:

            controller != null
            controller.instanceControllersApi != null
    }

    def cleanup() {
        RequestContextHolder.setRequestAttributes(null)
    }
}

