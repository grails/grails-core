package org.codehaus.groovy.grails.orm.hibernate.validation

import grails.persistence.Entity
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import spock.lang.Specification

@TestMixin(DomainClassUnitTestMixin)
class ValidateEmbeddedSpec extends Specification {
    def "Embedded properties are validated"() {
        given:
        mockDomain(Person)
        mockForConstraintsTests(Person)

        when:
        def person = new Person(name: 'Bart', pet: new Pet(name: ''))

        then:
        !person.validate()
        person.errors.getFieldError('pet.name').code == 'blank'
    }
}

@Entity
class Person {
    String name
    Pet pet

    static embedded = ['pet']

    static constraints = {
        name blank: false
    }
}

class Pet {
    String name

    static constraints = {
        name blank: false
    }
}
