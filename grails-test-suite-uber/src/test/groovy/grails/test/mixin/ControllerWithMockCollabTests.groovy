package grails.test.mixin

import grails.artefact.Artefact
import org.junit.Test

/**
 * Tests a controller with a mock collaborator
 */
@TestFor(ControllerWithCollabController)
class ControllerWithMockCollabTests {

    @Test
    void testFirstCall() {
        executeCallTest()
    }

    @Test
    void testSecondCall() {
        executeCallTest()
    }

    private void executeCallTest() {
        //Prepare
        boolean called = false
        controller.myCallable = [callMe: { called = true }]

        //Call
        controller.index()

        //Check
        assert response.status == 200
        assert called == true
    }
}

@Artefact("Controller")
class ControllerWithCollabController {

    def myCallable

    def index() {
        myCallable.callMe()
    }
}
