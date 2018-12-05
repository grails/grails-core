package org.grails.web.binding

import grails.persistence.Entity
import spock.lang.Issue
import spock.lang.Specification

class DefaultASTDatabindingHelperDomainClassSpecialPropertiesSpec extends
        Specification {

    @Issue('GRAILS-11173')
    void 'Test binding to special properties in a domain class'() {
        when:
        Date now = new Date()
        SomeDomainClass obj = new SomeDomainClass(dateCreated: now, lastUpdated: now)
        
        then:
        obj.dateCreated == null
        obj.lastUpdated == null
    }
    
    @Issue('GRAILS-11173')
    void 'Test binding to special properties in a domain class with explicit bindable rules'() {
        when:
        def now = new Date()
        def obj = new SomeDomainClassWithExplicitBindableRules(dateCreated: now, lastUpdated: now)
        
        then:
        obj.dateCreated == now
        obj.lastUpdated == now
    }
}
        
@Entity
class SomeDomainClass {
    Date dateCreated
    Date lastUpdated
}

@Entity
class SomeDomainClassWithExplicitBindableRules {
    Date dateCreated
    Date lastUpdated

    static constraints = {
        dateCreated bindable: true
        lastUpdated bindable: true
    }
}
