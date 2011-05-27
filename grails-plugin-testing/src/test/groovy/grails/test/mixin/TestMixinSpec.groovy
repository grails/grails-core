package grails.test.mixin

import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.After
import org.junit.Before
import org.junit.runner.Result
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.spockframework.runtime.*
import spock.lang.*

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
        when: "A Junit 3 test is run"
            def test = junit3Test
            test.setUp()

        then:
            GrailsUnitTestMixin.grailsApplication != null
            GrailsUnitTestMixin.applicationContext != null
        when:
            test.testSomething()
            test.tearDown()

        then: "Check that @Before and @After hooks are called"
            MyMixin.doFirstCalled == true
            MyMixin.doLastCalled == true
            GrailsUnitTestMixin.grailsApplication == null
            GrailsUnitTestMixin.applicationContext == null

    }

    void "Test that appropriate test hooks are called for a JUnit 4 test"() {
        setup:
           MyMixin.doFirstCalled = false
           MyMixin.doLastCalled = false
        when: "A Junit 4 test is run"
            def test = junit4Test
            def runner = new BlockJUnit4ClassRunner(test.getClass())
            final notifier = new RunNotifier()
            def result = new Result()
            notifier.addListener(result.createListener())
            runner.run(notifier)


        then: "Check that @Before and @After hooks are called and the test was run"
            result.runCount == 1
            result.failureCount == 0
            MyMixin.doFirstCalled == true
            MyMixin.doLastCalled == true
    }

    @FailsWith(
      value = ConditionNotSatisfiedError,
      reason = "Spock 0.5 does not support @Before etc., Spock 0.6 (not yet released) does")
    void "Test that appropriate test hooks are called for a Spock test"() {
        setup:
           MyMixin.doFirstCalled = false
           MyMixin.doLastCalled = false
        when: "A Spock test is run"
            def test = spockTest
            def adapter = new Sputnik(test.getClass())
            final notifier = new RunNotifier()
            def result = new Result()
            notifier.addListener(result.createListener())
            adapter.run(notifier)


        then: "Check that the test is run and @Before and @After hooks are called"
            result.runCount == 1
            result.failureCount == 0

            MyMixin.doFirstCalled == true
            MyMixin.doLastCalled == true
    }

    def getJunit3Test() {
        new GroovyClassLoader().parseClass('''
@grails.test.mixin.TestMixin([grails.test.mixin.MyMixin,grails.test.mixin.SecondMixin])
class MyJunit3Test extends GroovyTestCase {

    void testSomething() {
        callMe()
        secondCall()
    }
}
''').newInstance()
    }


    def getJunit4Test() {
        new GroovyClassLoader().parseClass('''

@grails.test.mixin.TestMixin(grails.test.mixin.MyMixin)
class MyJunit4Test {

    @org.junit.Test
    void testSomething() {
        grailsApplication != null
        applicationContext != null
        callMe()
    }
}
''').newInstance()
    }

    def getSpockTest() {
        new GroovyClassLoader().parseClass('''
@grails.test.mixin.TestMixin(grails.test.mixin.MyMixin)
class MyJunitSpockTest extends spock.lang.Specification {

    void "Test something"() {
        when:
            callMe()
        then:
            true == true
    }
}
''').newInstance()
    }
}


class MyMixin extends GrailsUnitTestMixin{
    static doFirstCalled = false
    static doLastCalled = false
    @Before
    void doFirst() {
         assert grailsApplication != null
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
class SecondMixin extends GrailsUnitTestMixin{

    void secondCall() {
        // do nothing
    }

}

