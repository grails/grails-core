package org.codehaus.groovy.grails.web.servlet

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Mar 14, 2008
*/
class MultipleRenderCallsContentTypeTests extends AbstractGrailsControllerTests {

    public void onSetUp() {
        gcl.parseClass '''
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
'''
    }


    void testLastContentTypeWins() {
        def controller = ga.getControllerClass("MultipleRenderController").newInstance()

        controller.test()

        assertEquals "application/json;charset=utf-8", response.contentType
    }

   void testPriorSetContentTypeWins() {
        def controller = ga.getControllerClass("MultipleRenderController").newInstance()

        controller.test2()

        assertEquals "text/xml", response.contentType
    }
}