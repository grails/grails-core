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
}

class TestController {
    def renderText = {
        render "good"
    }

    def redirectToController = {
        redirect(controller:"bar")
    }
}
