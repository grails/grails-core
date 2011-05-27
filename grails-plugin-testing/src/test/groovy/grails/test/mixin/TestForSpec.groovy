package grails.test.mixin

import org.junit.Test
import spock.lang.Specification
import org.apache.commons.logging.Log

class TestForSpec extends Specification{

    void "Test that imports aren't needed for junit"() {
        when:
            def test = junit4Test

        then:
            test != null
            test.getClass().getDeclaredMethod("testIndex", null).getAnnotation(Test.class) != null
            test.retrieveLog() instanceof Log
    }

    void "Test junit 3 test doesn't get annotation"() {
        when:
            def test = junit3Test

        then:
            test != null
            test.getClass().getDeclaredMethod("testIndex", null).getAnnotation(Test.class) == null
            test.retrieveLog() instanceof Log
    }

    def getJunit4Test() {
        final gcl = new GroovyClassLoader()
        gcl.parseClass('''
class SimpleController {
    def index = {
        render "Hello"
    }
}
''')
        gcl.parseClass('''
import grails.test.mixin.*

@TestFor(SimpleController)
class ControllerTestForTests {

    void testIndex() {
        controller.index()
        assert response.text == 'Hello'
    }

    def retrieveLog() { log }
}


''').newInstance()
    }

    def getJunit3Test() {
        final gcl = new GroovyClassLoader()
        gcl.parseClass('''
class SimpleController {
    def index = {
        render "Hello"
    }
}
''')
        gcl.parseClass('''
import grails.test.mixin.*

@TestFor(SimpleController)
class ControllerTestForTests extends GroovyTestCase {

    void testIndex() {
        controller.index()
        assert response.text == 'Hello'
    }

    def retrieveLog() { log }
}


''').newInstance()
    }
}
