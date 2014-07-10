package org.grails.web.taglib
/**
 * @author Graeme Rocher
 * @since 1.0
 */
class TagLibraryDynamicPropertyTests extends AbstractGrailsTagTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.gsp.*

@TagLib
class FooTagLib {
    Closure showAction = { attrs, body ->
        out << "action: ${actionName}"
    }
    Closure showController = { attrs, body ->
        out << "controller: ${controllerName}"
    }

    Closure showSession = { attrs, body ->
        out << "test: ${session.foo}"
    }
    Closure showParam = { attrs, body ->
        out << "test: ${params.foo}"
    }
}
'''
    }

    void testActionName() {
        webRequest.actionName = "test"
        def template = '<g:showAction />'
        assertOutputEquals("action: test", template)
    }

    void testControllerName() {
        webRequest.controllerName = "foo"
        def template = '<g:showController />'
        assertOutputEquals("controller: foo", template)
    }

    void testSession() {
        request.session.foo = "bar"
        def template = '<g:showSession />'
        assertOutputEquals("test: bar", template)
    }

    void testParams() {
        webRequest.params.foo = "bar"
        def template = '<g:showParam />'
        assertOutputEquals("test: bar", template)
    }
}
