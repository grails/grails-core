package grails.test.runtime

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.runtime.DirtiesRuntime;
import spock.lang.Issue
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class DirtiesRuntimeSpec extends Specification {

    @Issue('GRAILS-11671')
    void 'test method 1'() {
        expect:
        !String.metaClass.hasMetaMethod('someNewMethod')
    }
    
    @Issue('GRAILS-11671')
    void 'test method 2'() {
        expect:
        !String.metaClass.hasMetaMethod('someNewMethod')
    }
    
    @DirtiesRuntime
    @Issue('GRAILS-11671')
    void 'test method 3'() {
        when:
        String.metaClass.someNewMethod = {}
        
        then:
        String.metaClass.hasMetaMethod('someNewMethod')
    }
    
    @Issue('GRAILS-11671')
    void 'test method 4'() {
        expect:
        !String.metaClass.hasMetaMethod('someNewMethod')
    }
    
    @Issue('GRAILS-11671')
    void 'test method 5'() {
        expect:
        !String.metaClass.hasMetaMethod('someNewMethod')
    }
}
