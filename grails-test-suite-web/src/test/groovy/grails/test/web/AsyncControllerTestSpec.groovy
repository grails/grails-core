package grails.test.web

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification
import grails.artefact.Artefact

/**
 */
class AsyncControllerTestSpec extends Specification implements ControllerUnitTest<FooController> {
    void "Test that it is possible to test interaction with the Servlet 3.0 async API"() {
        when:"A controller that uses the async API is called"
            controller.index()
        then:"The async response is testable"
            response.text == 'Hello World'
    }
}
@Artefact("Controller")
class FooController {

    def index() {
        def ctx = startAsync()
        ctx.start {
            render "Hello World"
            ctx.complete()
        }
    }
}
