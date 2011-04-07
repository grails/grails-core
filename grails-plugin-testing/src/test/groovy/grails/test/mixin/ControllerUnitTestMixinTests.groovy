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
}
