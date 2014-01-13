package grails.validation

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.springframework.validation.FieldError

import spock.lang.Issue
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
@spock.lang.Ignore
class ValidateableSpec extends Specification {

    void 'Test validate can be invoked in a unit test with no special configuration'() {
        when: 'an object is valid'
        def validateable = new MyValidateable(name: 'Kirk', age: 47)

        then: 'validate() returns true and there are no errors'
        validateable.validate()
        !validateable.hasErrors()
        validateable.errors.errorCount == 0

        when: 'an object is invalid'
        validateable.name = 'kirk'

        then: 'validate() returns false and the appropriate error is created'
        !validateable.validate()
        validateable.hasErrors()
        validateable.errors.errorCount == 1
        validateable.errors['name'].code == 'matches.invalid'

        when: 'the clearErrors() is called'
        validateable.clearErrors()

        then: 'the errors are gone'
        !validateable.hasErrors()
        validateable.errors.errorCount == 0

        when: 'the object is put back in a valid state'
        validateable.name = 'Kirk'

        then: 'validate() returns true and there are no errors'
        validateable.validate()
        !validateable.hasErrors()
        validateable.errors.errorCount == 0
    }

    void 'Test mockForConstraintstests'() {
        given:
        mockForConstraintsTests MyValidateable

        when: 'an object is valid'
        def validateable = new MyValidateable(name: 'Kirk', age: 47)

        then: 'validate() returns true and there are no errors'
        validateable.validate()
        !validateable.hasErrors()
        validateable.errors.errorCount == 0

        when: 'an object is invalid'
        validateable.name = 'kirk'

        then: 'validate() returns false and the appropriate error is created'
        !validateable.validate()
        validateable.hasErrors()
        validateable.errors.errorCount == 1
        validateable.errors['name'] == 'matches'

        when: 'the clearErrors() is called'
        validateable.clearErrors()

        then: 'the errors are gone'
        !validateable.hasErrors()
        validateable.errors.errorCount == 0

        when: 'the object is put back in a valid state'
        validateable.name = 'Kirk'

        then: 'validate() returns true and there are no errors'
        validateable.validate()
        !validateable.hasErrors()
        validateable.errors.errorCount == 0
    }
    
    @Issue('GRAILS-10871')
    void 'Test that binding failures are retained during validation and that the corresponding property is not validated'() {
        given:
        def validateable = new MyValidateable()
        
        when:
        def fieldError = new FieldError(MyValidateable.name, 'age', 'forty two', true, null, null, null)
        validateable.errors.addError fieldError
       
        then:
        validateable.hasErrors()
        validateable.errors.errorCount == 1
        validateable.errors.getFieldError('age').rejectedValue == 'forty two'
        
        when:
        validateable.name = 'lower case'
        
        then:
        !validateable.validate()
        validateable.hasErrors()
        validateable.errors.errorCount == 2
        validateable.errors.getFieldError('age').rejectedValue == 'forty two'
        validateable.errors.getFieldError('name').rejectedValue == 'lower case'
    }
}

@Validateable
class MyValidateable {
    String name
    Integer age

    static constraints = {
        name matches: /[A-Z].*/
        age range: 1..99
    }
}
