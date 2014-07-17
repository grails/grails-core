package org.grails.compiler.web.converters

import grails.artefact.Artefact
import grails.converters.XML
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(RenderTestController)
class ConvertersControllersApiSpec extends Specification {

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
