package grails.test.mixin

import grails.test.mixin.*
import org.junit.*

@TestFor(MyService)
class ServiceAndMockForTests {

    @Test
    void testProvaUsingMockFor() {
        def mockForUtilsControl = mockFor(MockForUtils2)
        mockForUtilsControl.demand.isMockForWorking(1..2) {->
            return true
        }
        service.utils = mockForUtilsControl.createMock()
        service.prova()

        assert service.prova()

        // run validation on mocks
        mockForUtilsControl.verify()
    }

    @Test
    void testProvaUsingMockForAgain() {
        def mockForUtilsControl = mockFor(MockForUtils2)
        mockForUtilsControl.demand.isMockForWorking(1..1) {->
            return true
        }
        service.utils = mockForUtilsControl.createMock()
        assert service.prova()

        // run validation on mocks
        mockForUtilsControl.verify()
    }

    @Test
    void testIndexWithoutUsingMockForAgain() {
        service.prova()
    }

}

class MyService {
    def utils
    boolean prova() {
        utils?.isMockForWorking()
    }
}

class MockForUtils2 {
    boolean isMockForWorking() {
        return false
    }
}
