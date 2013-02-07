package grails.test.mixin

/**
 * @author Graeme Rocher
 */
import org.junit.Test

@TestFor(SimpleController)
@Mock(Simple)
class ControllerTestForTest {

    @Test
    void testIndex() {
        controller.index()
        assert response.text == 'Hello'
    }

    @Test
    void testTotal() {
        controller.total()
        assert response.text == "Total = 0"
    }

    @Test
    void testMockCollaborator() {
        def mockService = mockFor(SimpleService)
        mockService.demand.sayHello(1) {-> "goodbye" }

        controller.simpleService = mockService.createMock()
        controller.hello()

        mockService.verify()
        assert response.text == 'goodbye'
    }
}