package org.grails.web.servlet

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification
import grails.artefact.Artefact

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class MultipleRenderCallsContentTypeTests extends Specification implements ControllerUnitTest<MultipleRenderController> {

    void testLastContentTypeWins() {
        when:
        controller.test()

        then:
        "application/json;charset=utf-8" == response.contentType
    }

    void testPriorSetContentTypeWins() {
        when:
        controller.test2()

        then:
        "text/xml" == response.contentType
    }
}

@Artefact('Controller')
class MultipleRenderController {

    def test = {
        render(text:"foo",contentType:"text/xml")
        render(text:"bar",contentType:"application/json")
    }

    def test2 = {
        response.contentType = "text/xml"

        render(text:"bar")
    }
}
