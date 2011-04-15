package grails.test.mixin

import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 15/04/2011
 * Time: 12:02
 * To change this template use File | Settings | File Templates.
 */
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