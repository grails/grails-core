package org.grails.compiler.web

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class WithFormatSpec extends Specification implements ControllerUnitTest<MimeTypesCompiledController> {

    void "Test withFormat method injected at compile time"() {
        when:
        response.format = 'html'
        def format = controller.index()

        then:
        format == "html"
        
        when:
        response.format = 'xml'
        format = controller.index()
        
        then:
        format == 'xml'
    }
}

@Artefact('Controller')
class MimeTypesCompiledController {
    def index() {
        withFormat {
            html { "html" }
            xml { "xml" }
        }
    }
}

