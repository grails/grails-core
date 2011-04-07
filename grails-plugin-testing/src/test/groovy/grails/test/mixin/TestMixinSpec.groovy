package grails.test.mixin

import org.junit.After
import org.junit.Before
import spock.lang.Specification

/**
 * Tests for the mixin that adds functionality to a test case
 *
 * @author Graeme Rocher
 *
 */
class TestMixinSpec extends Specification {

    void "Test that appropriate test hooks are called for a JUnit 3 test"() {
        setup:
           MyMixin.doFirstCalled = false
           MyMixin.doLastCalled = false
        when:
            def test = new MyJunit3Test()
            test.testSomething()

        then:
            MyMixin.doFirstCalled == true
            MyMixin.doLastCalled == true
    }
}

@TestMixin(MyMixin)
class MyJunit3Test extends GroovyTestCase {

    void testSomething() {
        callMe()
    }
}
class MyMixin {
    static doFirstCalled = false
    static doLastCalled = false
    @Before
    void doFirst() {
         doFirstCalled = true
    }

    void callMe() {
        // do nothing
    }

    @After
    void doLast() {
        doLastCalled = true
    }
}
