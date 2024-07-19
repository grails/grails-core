package grails.test.mixin

import grails.testing.web.GrailsWebUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class ControllerMockWithMultipleControllersSpec extends Specification implements GrailsWebUnitTest {

    void "Test that both mocked controllers are valid"() {
        given:
        mockController(FirstController)
        mockController(SimpleController)

        when:"Two mock controllers are created"
            def c1 = new FirstController()
            def c2 = new SimpleController()

        then:"The request context is accessible from both"
            c1.request != null
            c2.request != null
    }
}
