package grails.test.mixin

import spock.lang.Specification

@Mock([FirstController, SimpleController])
class ControllerMockWithMultipleControllersSpec extends Specification {

    void "Test that both mocked controllers are valid"() {
        when:"Two mock controllers are created"
            def c1 = new FirstController()
            def c2 = new SimpleController()

        then:"The request context is accessible from both"
            c1.request != null
            c2.request != null
    }
}
