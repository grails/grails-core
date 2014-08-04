package grails.test.mixin

import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.runtime.FreshRuntime

import org.grails.spring.beans.factory.InstanceFactoryBean
import spock.lang.Specification

/**
 * @author Lari Hotari
 */
@FreshRuntime
@TestMixin(GrailsUnitTestMixin)
class MockedBeanSpec extends Specification {
    def myService=Mock(MyService)
    
    def doWithSpring = {
        myService(InstanceFactoryBean, myService, MyService)
    }
    
    def "doWithSpring callback is executed"() {
        when:
        def myServiceBean=grailsApplication.mainContext.getBean('myService')
        myServiceBean.identity() == System.identityHashCode(this)
        then:
        1 * myService.identity() >> { System.identityHashCode(this) }
        where:
        counter << [0..10].flatten() 
    }
}
