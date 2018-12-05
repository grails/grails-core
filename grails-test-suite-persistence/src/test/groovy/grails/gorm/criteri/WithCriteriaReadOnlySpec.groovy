package grails.gorm.criteri

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class WithCriteriaReadOnlySpec extends Specification implements DomainUnitTest<Person> {

    void 'test that the readOnly criteria method is available in a unit test'() {
        given:
        ['Jeff', 'Betsy', 'Jake', 'Zack'].each { name ->
            new Person(name: name).save()
        }
        
        when:
        def results = Person.withCriteria {
            readOnly true
            like 'name', 'J%'
        }
        
        then:
        results.size() == 2
    }
}

@Entity
class Person {
    String name
}