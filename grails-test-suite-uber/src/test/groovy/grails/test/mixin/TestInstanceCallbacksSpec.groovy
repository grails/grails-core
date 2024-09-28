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
    
    @PendingFeature(reason = 'org.springframework.beans.factory.NoSuchBeanDefinitionException: No bean named \'myService\' available')
    def "doWithSpring callback is executed"() {
        expect:
        grailsApplication.mainContext.getBean('myService') != null
    }

    @PendingFeature(reason = 'myConfigValue is null')
    def "doWithConfig callback is executed"(){
        expect:
        config.myConfigValue == 'Hello'
    }
}
