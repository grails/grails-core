package grails.test.web

import spock.lang.Specification
import grails.artefact.Artefact
import grails.test.mixin.TestFor

/**
 */
@TestFor(FooController)
class AsyncControllerTestSpec extends Specification{
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
