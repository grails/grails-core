package grails.test.mixin

import grails.test.mixin.support.GrailsUnitTestMixin

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
class StaticCallbacksSpec extends Specification {
    static doWithSpring = {
        myService(MyService)
    }
    
    static doWithConfig(c) {
        c.myConfigValue = 'Hello'    
    }
    
    def "grailsApplication is not null"() {
        expect:
        grailsApplication != null
    }
    
    def "doWithSpring callback is executed"() {
        expect:
        grailsApplication.mainContext.getBean('myService') != null
    }

    def "doWithConfig callback is executed"(){
        expect:
        config.myConfigValue == 'Hello'
    }
}
