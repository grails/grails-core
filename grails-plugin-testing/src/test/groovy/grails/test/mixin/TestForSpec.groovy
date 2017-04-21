package grails.test.mixin

import org.slf4j.Logger
import org.junit.Test

import spock.lang.Specification

class TestForSpec extends Specification{

    void "Test that imports aren't needed for junit"() {
        when:
            def test = junit4Test

        then:
            test != null
            test.getClass().getDeclaredMethod("testIndex", null).getAnnotation(Test.class) != null
            test.retrieveLog() instanceof Logger
    }

    void "Test spock test doesn't get annotation"() {
        when:
            def test = spockTest

        then:
            test != null
            test.retrieveLog() instanceof Logger
    }

    def getSpockTest() {
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
class ControllerTestForTests extends spock.lang.Specification  {

    void "Test index"() {
        when:
            controller.index()

        then:
            response.text == 'Hello'
    }

    def retrieveLog() { log }
}
''').newInstance()
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

}
