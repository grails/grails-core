package grails.test.mixin

import grails.artefact.Artefact
import grails.test.runtime.DirtiesRuntime

/**
 *
 */
@TestFor(MyController)
class ControllerAndMockForTests {

    void testIndexWithoutUsingMockFor() {
        controller.index()
    }

    /**
     * Leave this test in, and testIndexWithoutUsingMockForAgain will fail.
     * Comment this test out, and the other 2 tests will work
     */
    @DirtiesRuntime
    void testIndexUsingMockFor() {
        def mockForUtilsControl = mockFor(MockForUtils)
        mockForUtilsControl.demand.static.isMockForWorking(1..1) {->
            return true
        }

        controller.index()

        // run validation on mocks
        mockForUtilsControl.verify()
    }

    void testIndexWithoutUsingMockForAgain() {
        controller.index()
    }

}

@Artefact("Controller")
class MyController {

    def index() {
        render "mockFor is working: ${MockForUtils.isMockForWorking()}"
    }
}

class MockForUtils {

    static boolean isMockForWorking() {
        return false
    }
}
