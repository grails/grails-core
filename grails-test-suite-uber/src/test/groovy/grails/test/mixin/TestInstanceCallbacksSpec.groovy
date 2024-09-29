package grails.test.mixin

import org.grails.testing.GrailsUnitTest
import spock.lang.PendingFeature
import spock.lang.Specification

/**
 * @author Lari Hotari
 */
class TestInstanceCallbacksSpec extends Specification implements GrailsUnitTest {

    Closure doWithSpring() {{ ->
        myService(MyService)
    }}

    Closure doWithConfig() {{ c ->
        c.myConfigValue = 'Hello'
    }}
    
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
