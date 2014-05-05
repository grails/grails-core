package org.codehaus.groovy.grails.compiler.web.converters

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Specification

class ConvertersControllersApiSpec extends Specification {

    void "Test that the render method for converters is added at compile time"() {
        given:
            def gcl = new GrailsAwareClassLoader()
            def transformer = new ConvertersControllersTransformer() {
                boolean shouldInject(URL url) { true }
            }
            gcl.classInjectors = [transformer] as ClassInjector[]
			def cls = gcl.parseClass('''

import grails.converters.*

class RenderTestController {
    def response = new org.springframework.mock.web.MockHttpServletResponse()
    def index() {
        render new XML("test")
    }
}

''')

        when:					
            def controller = cls.newInstance()
			println "controller = ${controller?.dump()}"
            def response
            try {
                GrailsWebUtil.bindMockWebRequest()
                controller.index()
                response = controller.response

            }
            finally {
                RequestContextHolder.setRequestAttributes(null)
            }

        then:
            response != null
            response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><string>test</string>'
    }

    void "Test that the render method for converters on annotated domain"() {
        given:
            def gcl = new GrailsAwareClassLoader()

        when:
            def cls = gcl.parseClass('''

import grails.converters.*
import grails.artefact.*

@Artefact("Controller")
class RenderTestController {
    def response = new org.springframework.mock.web.MockHttpServletResponse()
    def index() {
        render new XML("test")
    }
}

''')
            def controller = cls.newInstance()
            def response
            try {
                GrailsWebUtil.bindMockWebRequest()
                controller.index()
                response = controller.response

            }
            finally {
                RequestContextHolder.setRequestAttributes(null)
            }

        then:
            response != null
            response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><string>test</string>'
    }
}
