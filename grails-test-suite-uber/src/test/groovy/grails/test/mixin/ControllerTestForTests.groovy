package grails.test.mixin

import org.junit.Test

@TestFor(SimpleController)
@Mock(Simple)
class ControllerTestForTests {

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
class SimpleController {
    def index = {
        render "Hello"
    }

    def total = {
        render "Total = ${Simple.count()}"
    }

    def simpleService
    def hello = {
        render simpleService.sayHello()
    }
}
class Simple {
    Long id
    Long version
    String name
}
class SimpleService {
    String sayHello() {
        "hello"
    }
}