package grails.test.mixin

import grails.test.mixin.support.GrailsUnitTestMixin
import grails.util.Mixin
import grails.util.MixinTargetAware
import org.junit.AfterClass
import org.junit.Assert

import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class MetaClassCleanupSpec extends Specification {

    def "Test that meta classes are restored to prior state after test run"() {
        when:"A meta class is modified in the test"
            Author.metaClass.testMe = {-> "test"}
            Author.metaClass.testToo = {-> "second"}
            def a = new Author()
        then:"The methods are available"
            a.testMe() == "test"
            a.testToo() == "second"
    }

    def instance = new Author()
    def "Test that changes made to an instance are cleaned up - step 1"() {
        when:"a change is made to an instance"
            instance.metaClass.doWork = {->"done"}

        then:"The method is callable"
            instance.doWork() == "done"
    }

    def "Test that changes made to an instance are cleaned up - step 2"() {
        when:"when the method is called again"
            instance.doWork()

        then:"The method was cleaned by the registry cleaner"
            thrown MissingMethodException
    }

    def "Test that meta class cleanup doesn't break XmlSlurper - step 1"() {
        given:"A service that uses XmlSlurper"
            def service = new HelloService()
        when:"A method is invoked that uses XmlSlurper"
            def greeting = service.greet('hello')
        then:"The correct result is returned"
            greeting == 'hello'
    }

    def "Test that meta class cleanup doesn't break XmlSlurper - step 2"() {
        given:"A service that uses XmlSlurper"
        def service = new HelloService()
        when:"A method is invoked that uses XmlSlurper"
        def greeting = service.greet('goodbye')
        then:"The correct result is returned"
        greeting == 'goodbye'
    }

    def "Test that mixins are re-applied after cleanup - step 1"() {
        given:"A mixin class"
            def a = new A()

        when:"A method is called that uses a mixin"
            def rs = a.doStuff()

        then:"The the mixin method works"
            rs == "A with mixin: mixMe from AMixin good mix static"
    }

    def "Test that mixins are re-applied after cleanup - step 2"() {
        given:"A mixin class"
            def a = new A()
        when:"A method is called that uses a mixin"
            def rs = a.doStuff()

        then:"The the mixin method works"
            rs == "A with mixin: mixMe from AMixin good mix static"
    }
    @AfterClass
    static void checkCleanup() {
        def a = new Author()

        try {
            a.testMe()
            Assert.fail("Should have cleaned up meta class changes")
        } catch (MissingMethodException) {
        }

        try {
            a.testToo()
            Assert.fail("Should have cleaned up meta class changes")
        } catch (MissingMethodException) {
        }
    }
}

class Author {
    String name
}

class HelloService {

    def greet(message) {
        def xml = "<greeting message='${message}'/>"
        new XmlParser().parseText(xml).@message
    }
}

class AMixin implements MixinTargetAware {
    Object target
    String prop = "foo"
    String mixMe() {"mixMe from AMixin $target.me"}
    static mixStatic() { "mix static"}
}

@Mixin(AMixin)
class A {
    String me = "good"
    String doStuff() {
        "A with mixin: ${mixMe()} ${mixStatic()}"
    }
}
