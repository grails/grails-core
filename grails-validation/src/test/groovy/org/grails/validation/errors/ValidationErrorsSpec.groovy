package org.grails.validation.errors

import grails.validation.ValidationErrors

import org.springframework.validation.FieldError

import spock.lang.Specification

/**
 * Tests for the ValidationErrors class
 */
class ValidationErrorsSpec extends Specification{

    def "Test obtain error via subscript operator"() {
        given:"An errors object with errors"
            def errors = new ValidationErrors(new Test())
            errors.rejectValue("name", "bad.name")

        when:"The error is accessed via the subscript operator"
            def error = errors['name']

        then:"A FieldError is returned"
            error instanceof FieldError
            error.code == 'bad.name'

    }

    def "Test reject field via subscript operator"() {
        given:"An errors object with errors"
            def errors = new ValidationErrors(new Test())
            errors['name'] = 'bad.name'

        when:"The error is accessed via the subscript operator"
            def error = errors['name']

        then:"A FieldError is returned"
            error instanceof FieldError
            error.code == 'bad.name'
    }
}
class Test {
    String name
}
