package grails.test.mixin

import grails.test.mixin.web.ControllerUnitTestMixin

 /**
 * Specification for the behavior of the ControllerUnitTestMixin
 *
 * @author Graeme Rocher
 *
 */
@TestMixin(ControllerUnitTestMixin)
class ControllerUnitTestMixinTests extends GroovyTestCase {


    void testRenderText() {
        def controller = mockController(TestController)

        controller.renderText()
        assert response.contentAsString == "good"
    }


    void testSimpleControllerRedirect() {

        def controller = mockController(TestController)

        controller.redirectToController()

        assert response.redirectedUrl == '/bar'
    }

    void testRenderView() {
        def controller = mockController(TestController)

        controller.renderView()

        assert "/test/foo" == controller.modelAndView.viewName
    }

    void testRenderXml() {
        def controller = mockController(TestController)

        controller.renderXml()

        assert "<book title='Great'/>" == controller.response.contentAsString
        assert "Great" == controller.response.xml.@title.text()
    }

    void testRenderJson() {

        def controller = mockController(TestController)

        controller.renderJson()

        assert '{"book":"Great"}' == controller.response.contentAsString
        assert "Great" == controller.response.json.book

    }

    void testRenderState() {
        request.addParameter("foo", "bar")
        request.setAttribute("bar", "foo")
        def controller = mockController(TestController)

        controller.renderState()

        def xml = response.xml

        assert xml.parameter.find { it.@name == 'foo' }.@value.text() == 'bar'
        assert xml.attribute.find { it.@name == 'bar' }.@value.text() == 'foo'
    }

    void testInjectedProperties() {
        assert request != null
        assert response != null
        assert servletContext != null
        assert params != null
        assert grailsApplication != null
        assert applicationContext != null
        assert webRequest != null
    }
}

class TestController {
    def renderText = {
        render "good"
    }

    def redirectToController = {
        redirect(controller:"bar")
    }

    def renderView = {
        render(view:"foo")
    }

    def renderXml = {
        render(contentType:"text/xml") {
            book(title:"Great")
        }
    }

    def renderJson = {
        render(contentType:"text/json") {
            book = "Great"
        }
    }

    def renderState = {
        render(contentType:"text/xml") {
            println params.foo
            println request.bar
            requestInfo {
                for(p in params) {
                    parameter(name:p.key, value:p.value)
                }
                request.each {
                    attribute(name:it.key, value:it.value)
                }
            }

        }
    }
}
