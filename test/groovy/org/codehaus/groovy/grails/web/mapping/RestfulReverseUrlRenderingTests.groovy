package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Jan 31, 2008
*/
class RestfulReverseUrlRenderingTests extends AbstractGrailsTagTests {

    public void onSetUp() {
        gcl.parseClass '''
class UrlMappings {
    static mappings = {
    "/book" (controller: "book") { action = [GET: "create", POST: "save"] }
    }
}'''

        gcl.parseClass '''
class BookController {
    def create = {}
    def save = {}
}
'''
    }


    void testLinkTagRendering() {
        def template = '<g:link controller="book">create</g:link>'

        assertOutputEquals '<a href="/book">create</a>', template
    }

    void testFormTagRendering() {
        def template = '<g:form controller="book" name="myForm" method="POST">save</g:form>'

        assertOutputEquals '<form action="/book" method="POST" name="myForm" id="myForm" >save</form>', template

    }

    void testFormTagRenderingWithControllerAndAction() {
        def template = '<g:form controller="book" action="save" name="myForm" method="POST">save</g:form>'

        assertOutputEquals '<form action="/book/save" method="POST" name="myForm" id="myForm" >save</form>', template

    }

    void testFormTagRenderGETRequest() {
        def template = '<g:form controller="book" name="myForm" method="GET">create</g:form>'

        assertOutputEquals '<form action="/book" method="GET" name="myForm" id="myForm" >create</form>', template

    }


}
