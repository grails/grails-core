package grails.test.mixin.domain

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class SaveDomainSpec extends Specification implements DomainUnitTest<Person> {

    void 'test dateCreated and lastUpdated populated'() {
        given:
        Person person = new Person(name: 'Bobby')

        when:
        person.save(flush: true)

        then:
        person.dateCreated != null
        person.lastUpdated != null
        person.dateCreated == person.lastUpdated

        when:
        person.name = 'Bobby Updated'
        person.save(flush: true)

        then:
        person.lastUpdated > person.dateCreated
    }
}

@Entity
class Person {
    String name
    Date dateCreated
    Date lastUpdated
}