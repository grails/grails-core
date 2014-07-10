package org.grails.web.servlet

import static org.junit.Assert.assertEquals
import grails.artefact.Artefact
import grails.test.mixin.TestFor

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@TestFor(MultipleRenderController)
class MultipleRenderCallsContentTypeTests {

    @Test
    void testLastContentTypeWins() {
        controller.test()

        assertEquals "application/json;charset=utf-8", response.contentType
    }

    @Test
    void testPriorSetContentTypeWins() {
        controller.test2()

        assertEquals "text/xml", response.contentType
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
