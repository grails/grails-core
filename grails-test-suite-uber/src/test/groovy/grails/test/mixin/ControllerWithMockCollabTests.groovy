package grails.test.mixin

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
        def mockCallable = mockFor(MyCallable)
        mockCallable.demand.callMe { -> called = true; println "called"}
        def mockCallableInstance = mockCallable.createMock()
        controller.myCallable = mockCallableInstance

        //Call
        controller.index()

        //Check
        assert response.status == 200
        assert called == true
        mockCallable.verify()
    }
}

class ControllerWithCollabController {

    def myCallable

    def index() {
        myCallable.callMe()
    }
}

interface MyCallable {
    void callMe()
}
