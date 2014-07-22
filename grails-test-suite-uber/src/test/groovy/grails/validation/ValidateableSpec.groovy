package grails.validation

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.springframework.validation.FieldError

import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class ValidateableSpec extends Specification {

    void 'Test validate can be invoked in a unit test with no special configuration'() {
        when: 'an object is valid'
        def validateable = new MyValidateable(name: 'Kirk', age: 47, town: 'STL')

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
        validateable.errors.getFieldError('name').code == 'matches.invalid'

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
        def validateable = new MyValidateable(name: 'Kirk', age: 47, town: 'STL')

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
    @Ignore
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
    
    @Issue('GRAILS-11601')
    void 'Test that only the expected properties are constrained'() {
        when:
        def constraints = MyValidateable.constraints
        
        then:
        constraints.size() == 5
        constraints.containsKey 'name'
        constraints.containsKey 'town'
        constraints.containsKey 'age'
        constraints.containsKey 'someProperty'
        constraints.containsKey 'twiceAge'
        
        and:
        constraints.name.appliedConstraints.size() == 2
        constraints.age.appliedConstraints.size() == 2
        constraints.town.appliedConstraints.size() == 1
        constraints.someProperty.appliedConstraints.size() == 1
        constraints.twiceAge.appliedConstraints.size() == 1
        
        and:
        constraints.name.hasAppliedConstraint 'matches'
        constraints.name.hasAppliedConstraint 'nullable'
        constraints.age.hasAppliedConstraint 'range'
        constraints.age.hasAppliedConstraint 'nullable'
        constraints.town.hasAppliedConstraint 'nullable'
        constraints.someProperty.hasAppliedConstraint 'nullable'
        constraints.twiceAge.hasAppliedConstraint 'nullable'

        and:
        !constraints.town.nullable
    }

    void 'Test that constraints are nullable by default if overridden'() {
        when:
        def constraints = MyNullableValidateable.constraints

        then:
        constraints.size() == 3
        constraints.containsKey 'name'
        constraints.containsKey 'town'
        constraints.containsKey 'age'

        and:
        constraints.name.appliedConstraints.size() == 1
        constraints.age.appliedConstraints.size() == 1
        constraints.town.appliedConstraints.size() == 1

        and: "name and age are nullable by default"
        constraints.name.nullable
        constraints.age.nullable

        and:
        !constraints.town.nullable
    }
}

@Validateable
class MyValidateable {
    String name
    Integer age
    String town
    private String _someProperty = 'default value'
    
    void setSomeOtherProperty(String s) {}
    
    void setSomeProperty(String s) {
        _someProperty = s
    }
    
    String getSomeProperty() {
        _someProperty
    }
    
    int getTwiceAge() {
        age * 2
    }

    static constraints = {
        name matches: /[A-Z].*/
        age range: 1..99
    }
}

@Validateable(nullable = true)
class MyNullableValidateable {
    String name
    Integer age
    String town

    static constraints = {
        town nullable: false
    }
}
