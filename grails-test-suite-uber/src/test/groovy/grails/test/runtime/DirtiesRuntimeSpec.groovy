package grails.test.runtime

import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Stepwise
import spock.util.mop.ConfineMetaClassChanges

@Stepwise
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
    
    @ConfineMetaClassChanges([String])
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
