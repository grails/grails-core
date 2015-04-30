package grails.test

import spock.lang.Issue
import spock.lang.Specification

class GrailsMockSpec extends Specification {

    @Issue('GRAILS-11075')
    void 'Test that verify() can be called in the then block of a Spock spec'() {
        given:
            def mock = new GrailsMock(String)
            mock.demand.trim(1..1) { ->
                'trimmed string'
            }
            def str = mock.createMock()
        
        when:
            def result = str.trim()
        
        then: 
            result == 'trimmed string'
            mock.verify()
    }

    void 'Test that ExplicitDemand correctly detects missing methods (fail case)'() {
        given:
            def mock = new GrailsMock(String)
        when:
            mock.demandExplicit.thisMethodDoesNotExist(1..1) { ->
                'will never be called'
            }
        then: 
            thrown(ExplicitDemandException)
    }

    void 'Test that ExplicitDemand correctly detects missing methods (success case)'() {
        given:
            def mock = new GrailsMock(String)
        when:
            mock.demandExplicit.trim(1..1) { ->
                'trimmed string'
            }
            def str = mock.createMock()
            def result = str.trim()
        then: 
            result == 'trimmed string'
            mock.verify()
    }

    class ClassWithStaticMethod {
        static String staticStringThing() {
            return 'Here is a string'
        }
    }

    void 'Test that ExplicitDemand correctly detects missing static methods (fail case)'() {
        given:
            def mock = new GrailsMock(ClassWithStaticMethod)
        when:
            mock.demandExplicit.static.thisStaticMethodDoesNotExist(1..1) { ->
                'will never be called'
            }
        then: 
            thrown(ExplicitDemandException)
    }

    void 'Test that ExplicitDemand correctly detects missing static methods (success case)'() {
        given:
            def mock = new GrailsMock(ClassWithStaticMethod)
            mock.demandExplicit.static.staticStringThing(1..1) { ->
                return 'different string'
            }
        when:
            def result = ClassWithStaticMethod.staticStringThing()
        then: 
            result == 'different string'
            mock.verify()
    }
}
