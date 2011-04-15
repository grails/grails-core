package grails.test.mixin

import spock.lang.Specification

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 15/04/2011
 * Time: 12:11
 * To change this template use File | Settings | File Templates.
 */
class TestForSpec extends Specification{

    void "Test that imports aren't needed for junit"() {
        // this currently is not working, can't add imports via local transforms
        when:
            def test = junit4Test

        then:
            test != null
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
import org.junit.*

@TestFor(SimpleController)
class ControllerTestForTests {

    @Test
    void testIndex() {
        controller.index()
        assert response.text == 'Hello'
    }
}


''').newInstance()
    }
}
