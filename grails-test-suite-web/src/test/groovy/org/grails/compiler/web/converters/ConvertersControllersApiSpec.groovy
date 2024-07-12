package org.grails.compiler.web.converters

import grails.artefact.Artefact
import grails.converters.XML
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class ConvertersControllersApiSpec extends Specification implements ControllerUnitTest<RenderTestController> {

    void "Test that the render method for converters is added at compile time"() {
        when:
            controller.index()

        then:
            response.contentAsString == '<?xml version="1.0" encoding="UTF-8"?><string>test</string>'
    }
}

@Artefact('Controller')
class RenderTestController {
    def index() {
        render new XML("test")
    }
}
