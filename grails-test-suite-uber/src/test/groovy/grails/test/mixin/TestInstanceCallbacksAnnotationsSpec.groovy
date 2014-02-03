package grails.test.mixin

import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.runtime.FreshRuntime;

import org.junit.ClassRule
import org.junit.rules.TestRule

import spock.lang.Ignore;
import spock.lang.IgnoreRest
import spock.lang.Shared;
import spock.lang.Specification

/**
 * @author Lari Hotari
 */
@TestMixin(GrailsUnitTestMixin)
class TestInstanceCallbacksAnnotationsSpec extends Specification {
    def doWithSpring = {
        myService(MyService)
    }
    
    def doWithConfig(c) {
        c.myConfigValue = 'Hello'    
    }
    
    @FreshRuntime
    def "grailsApplication is not null"() {
        expect:
        grailsApplication != null
    }
    
    @FreshRuntime
    def "doWithSpring callback is executed"() {
        expect:
        grailsApplication.mainContext.getBean('myService') != null
    }

    @FreshRuntime
    def "doWithConfig callback is executed"(){
        expect:
        config.myConfigValue == 'Hello'
    }
}
