package grails.test.mixin.support

import grails.test.mixin.TestMixin

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware

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
