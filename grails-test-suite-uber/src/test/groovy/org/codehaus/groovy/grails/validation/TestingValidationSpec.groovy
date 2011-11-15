package org.codehaus.groovy.grails.validation

import grails.persistence.Entity
import grails.test.mixin.Mock
import spock.lang.Specification

@Mock(Person)
class TestingValidationSpec extends Specification {

    void 'Test validating a domain object which has binding errors associated with it'() {
        given:
            def person = new Person(name: 'Jeff', age: 42)
            
        when:
            person.properties = [age: 'some string', name: 'Jeff Scott Brown']
            person.validate()
            def errorCount = person.errors.errorCount
            def ageError = person.errors.getFieldError('age')
            
        then:
            errorCount == 1
            'typeMismatch' in ageError.codes
    }
}

@Entity
class Person {
    String name
    Integer age
}
