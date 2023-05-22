package org.grails.validation

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class TestingValidationSpec extends Specification implements DomainUnitTest<Person> {

    void 'Test validating a domain object which has binding errors associated with it'() {
        given:
            def person = new Person(name: 'Jeff', age: 42, email: 'jeff.brown@springsource.com')

        when:
            person.properties = [age: 'some string', name: 'Jeff Scott Brown']
            person.validate()
            def errorCount = person.errors.errorCount
            def ageError = person.errors.getFieldError('age')

        then:
            errorCount == 1
            'typeMismatch' in ageError.codes
    }

    void 'Test fixing a validation error'() {
        given:
            def person = new Person(name: 'Jeff', age: 42, email: 'bademail')

        when:
            person.validate()
            def errorCount = person.errors.errorCount
            def emailError = person.errors.getFieldError('email')

        then:
            errorCount == 1
            'person.email.email.error' in emailError.codes

        when:
            person.email = 'jeff.brown@springsource.com'
            person.validate()

        then:
            !person.hasErrors()
    }

    void 'Test multiple validation errors on the same property'() {
        given:
            def person = new Person(name: 'Jeff', age: 42, email: 'bade')

        when:
            person.validate()
            def errorCount = person.errors.errorCount
            def emailErrors = person.errors.getFieldErrors('email')
            def codes = emailErrors*.codes.flatten()

        then:
            errorCount == 2
            emailErrors?.size() == 2
            'person.email.email.error' in codes
            'person.email.size.error' in codes

        when:
            person.clearErrors()

        then:
            0 == person.errors.errorCount

        when:
            person.validate()
            person.validate()
            errorCount = person.errors.errorCount
            emailErrors = person.errors.getFieldErrors('email')
            codes = emailErrors*.codes.flatten()

        then:
            errorCount == 2
            emailErrors?.size() == 2
            'person.email.email.error' in codes
            'person.email.size.error' in codes
    }

    void 'Test that binding errors are retained during validation'() {
        given:
        def person = new Person(name: 'Jeff', age: 42, email: 'jeff.brown@springsource.com')

        when:
            person.properties = [age: 'some string', name: 'Jeff Scott Brown', email: 'abcdefgh']

        then:
            1 == person.errors.errorCount

        when:
            def ageError = person.errors.getFieldError('age')

        then:
            'some string' == ageError.rejectedValue

        when:
            person.validate()
            def errorCount = person.errors.errorCount
            ageError = person.errors.getFieldError('age')
            def emailError = person.errors.getFieldError('email')

        then:
            errorCount == 2
            'typeMismatch' in ageError.codes
            'person.email.email.error' in emailError.codes
            'some string' == ageError.rejectedValue
    }
}

@Entity
class Person {
    String name
    Integer age
    String email

    static transients = ['images']

    Collection getImages() {
        throw new UnsupportedOperationException()
    }

    static constraints = {
        email email: true, size: 5..35
    }
}
