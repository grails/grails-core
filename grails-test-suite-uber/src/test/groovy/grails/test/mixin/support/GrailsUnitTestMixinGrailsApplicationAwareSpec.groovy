package grails.test.mixin.support

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import grails.test.mixin.TestMixin
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class GrailsUnitTestMixinGrailsApplicationAwareSpec extends Specification {

    static doWithSpring = {
        someBean SomeBean
    }
    
    void 'test that the GrailsApplicationAware post processor is effective for beans registered by a unit test'() {
        when: 'when a test registers a bean which implements GrailsApplicationAware'
        def someBean = applicationContext.someBean
        
        then: 'the grailsApplication property is properly initialized'
        someBean.grailsApplication
    }
}

class SomeBean implements GrailsApplicationAware {
    GrailsApplication grailsApplication
}