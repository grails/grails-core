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
}
class SimpleController {
    def index = {
        render "Hello"
    }

    def total = {
        render "Total = ${Simple.count()}"
    }
}
class Simple {
    Long id
    Long version
    String name
}