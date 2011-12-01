package grails.test.mixin

import spock.lang.Specification

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 12/1/11
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
@Mock([FirstController, SimpleController])
class ControllerMockWithMultipleControllersSpec extends Specification{

    void "Test that both mocked controllers are valid"() {
        when:"Two mock controllers are created"
            def c1 = new FirstController()
            def c2 = new SimpleController()

        then:"The request context is accessible from both"
            c1.request != null
            c2.request != null
    }
}
