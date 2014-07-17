package org.grails.compiler.web

import grails.artefact.Artefact
import grails.test.mixin.TestFor

import javax.servlet.http.HttpServletResponse

import spock.lang.Specification

@TestFor(MimeTypesCompiledController)
class WithFormatSpec extends Specification {

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

