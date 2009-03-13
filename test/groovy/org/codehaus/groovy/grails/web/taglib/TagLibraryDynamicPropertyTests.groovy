/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 28, 2007
 */
package org.codehaus.groovy.grails.web.taglib
class TagLibraryDynamicPropertyTests extends AbstractGrailsTagTests {

    public void onSetUp() {
        gcl.parseClass '''
class FooTagLib {
	def showAction = { attrs, body ->
		out << "action: ${actionName}"
	}
    def showController = { attrs, body ->
        out << "controller: ${controllerName}"
    }

	def showSession = { attrs, body ->
		out << "test: ${session.foo}"
	}
    def showParam = { attrs, body ->
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