package grails.validation

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.springframework.validation.FieldError
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class ValidateableTraitSpec extends Specification {
    
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
        def constraints = MyValidateable.getConstraintsMap()
        
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

    void 'Test that constraints are nullable by default if overridden and ensure nullable:true constraint is not applied when no other constraints were defined by user'() {
        when:
        def constraints = MyNullableValidateable.getConstraintsMap()

        then:
        constraints.size() == 3
        constraints.containsKey 'town'
        constraints.containsKey 'age'
        constraints.containsKey 'country'

        and:
        constraints.town.appliedConstraints.size() == 1
        constraints.country.appliedConstraints.size() == 1

        and: 'if property has any constraint defined, nullable is added too'
        constraints.age.appliedConstraints.size() == 2

        and: 'explicit nullable constraints are correctly applied'
        constraints.town.nullable == false
        constraints.country.nullable == true
    }
    
    @Issue('GRAILS-11625')
    void 'test that properties defined in a class marked with @Validateable(nullable=true) which are not explicitly constrained are not accessed during validation'() {
        given: 'an instance of a class marked with @Validateable(nullable=true)'
        println MyNullableValidateable.constraintsMap.keySet()
        def obj = new MyNullableValidateable(town: 'St. Louis', age:18)
        
        expect: 'property accessors are not invoked for properties which are not explicitly constrained (getName() would throw an exception)'
        obj.validate()
    }

    void 'ensure class without any constraints can be validated'(){
        NoConstraintsValidateable obj = new NoConstraintsValidateable()

        expect:
        obj.validate() == true
        !obj.hasErrors()
        obj.errors != null
    }

    void 'ensure only getters are considered as validateable properties'(){

        expect:
        propertyGetter == ValidatableGetters.isPropertyGetter(ValidatableGetters.getDeclaredMethod(methodName))

        where:
        propertyGetter  |   methodName
        true            |   'getTown'
        true            |   'getName'
        false           |   'getSurname'
        false           |   'getEmail'
        false           |   'getDay'
        false           |   'getEaster'
        false           |   'getChristmas'
        false           |   'getNewYear'
    }

    @Issue('9513')
    void 'ensure validation may be done for classes with private and protected getters'(){
        ValidatableGetters obj = new ValidatableGetters()

        expect: 'validation is executed and public properties/getters are marked nullable in errors'
        obj.validate() == false
        obj.errors['name'].code == 'nullable'
        !obj.errors['surname']
        !obj.errors['email']
    }

    void 'ensure private and protected getter is not handled as not-nullable property by default'(){
        when:
        Map constraints = new ValidatableGetters().getConstraintsMap()

        then: 'only public properties and public getters should be considered validateable properties by default'
        constraints
        constraints.size() == 2
        constraints.name
        constraints.town
    }
}

class MyValidateable implements Validateable {
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

class MyNullableValidateable implements Validateable {
    Integer age
    String town
    String country
    
    String getName() {
        throw new UnsupportedOperationException('getName() should not have been called during validation')
    }

    static constraints = {
        town nullable: false
        age validator:{val -> val > 0}
        country nullable: true
    }
    
    static boolean defaultNullable() {
        true
    }
}

class NoConstraintsValidateable implements Validateable {
    static constraints = {

    }
}

class ValidatableGetters implements Validateable {
    //standard properties - private/protected will not have getter
    String town

    //properties from getters
    String getName(){}
    protected String getSurname(){}
    private String getEmail(){}

    //static properties
    static Integer day

    //static properties from getters
    static Date getEaster() {}
    protected static Date getChristmas() {}
    private static Date getNewYear() {}
}