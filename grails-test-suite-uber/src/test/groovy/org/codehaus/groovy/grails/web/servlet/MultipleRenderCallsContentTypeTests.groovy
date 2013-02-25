package org.codehaus.groovy.grails.web.servlet

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class MultipleRenderCallsContentTypeTests extends AbstractGrailsControllerTests {

    @Override
    protected Collection<Class> getControllerClasses() {
        [MultipleRenderController]
    }

    void testLastContentTypeWins() {
        def controller = new MultipleRenderController()

        controller.test()

        assertEquals "application/json;charset=utf-8", response.contentType
    }

    void testPriorSetContentTypeWins() {
        def controller = new MultipleRenderController()

        controller.test2()

        assertEquals "text/xml", response.contentType
    }
}

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
