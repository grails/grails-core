package grails.test.mixin

import grails.persistence.Entity
import grails.test.runtime.FreshRuntime
import spock.lang.Issue
import spock.lang.Specification

@TestFor(SomeDomainClass)
@FreshRuntime
class DomainClassMetaClassCleanupSpec extends Specification {

    @Issue('GRAILS-11661')
    void 'test adding one method'() {
        when:
        SomeDomainClass.metaClass.static.one = {}
        
        then:
        SomeDomainClass.metaClass.hasMetaMethod('one')
        !SomeDomainClass.metaClass.hasMetaMethod('two')
    }

    @Issue('GRAILS-11661')
    void 'test adding another method'() {
        when:
        SomeDomainClass.metaClass.static.two = {}
        
        then:
        !SomeDomainClass.metaClass.hasMetaMethod('one')
        SomeDomainClass.metaClass.hasMetaMethod('two')
    }
}

@Entity
class SomeDomainClass {
    
}
