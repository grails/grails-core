package grails.validation

import grails.core.GrailsDomainClass
import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.util.ClosureToMapPopulator
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.validation.ConstraintsEvaluatorFactoryBean
import org.hibernate.Hibernate
import org.springframework.validation.FieldError
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

/**
 * Ensure validation logic for domain class and whether or not compatible with command object with {@code Validateable}.
 *
 * @see grails.validation.ValidateableTraitSpec
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([MyDomainClass, MyNullableDomainClass, NoConstraintsDomainClass, DomainClassGetters, SharedConstraintsDomainClass, SuperClassDomainClass, SubClassDomainClass])
class DomainClassValidationSpec extends Specification {

    @Issue('grails/grails-core#9749')
    void 'test that transient properties are not constrained by default but can be explicitly constrained'() {
        when:
        def props = getAssociatedDomainClassFromApplication(new DomainClassGetters()).getConstrainedProperties()

        then:
        !props.foo
        !props.baz
        !props.transientString1
        props.bar
        props.qux
        props.transientString2
    }

    void 'Test validate can be invoked in a unit test with no special configuration'() {
        when: 'an object is valid'
        def domainClass = new MyDomainClass(name: 'Kirk', age: 47, town: 'STL')

        then: 'validate() returns true and there are no errors'
        domainClass.validate()
        !domainClass.hasErrors()
        domainClass.errors.errorCount == 0

        when: 'an object is invalid'
        domainClass.name = 'kirk'

        then: 'validate() returns false and the appropriate error is created'
        !domainClass.validate()
        domainClass.hasErrors()
        domainClass.errors.errorCount == 1
        domainClass.errors.getFieldError('name').code == 'matches.invalid'

        when: 'the clearErrors() is called'
        domainClass.clearErrors()

        then: 'the errors are gone'
        !domainClass.hasErrors()
        domainClass.errors.errorCount == 0

        when: 'the object is put back in a valid state'
        domainClass.name = 'Kirk'

        then: 'validate() returns true and there are no errors'
        domainClass.validate()
        !domainClass.hasErrors()
        domainClass.errors.errorCount == 0
    }

    void 'Test that binding failures are retained during validation and that the corresponding property is not validated'() {
        given:
        def domainClass = new MyDomainClass()

        when:
        def fieldError = new FieldError(MyDomainClass.name, 'age', 'type mismatched', true, null, null, null)
        domainClass.errors.addError fieldError

        then:
        domainClass.hasErrors()
        domainClass.errors.errorCount == 1
        domainClass.errors.getFieldError('age').rejectedValue == 'type mismatched'

        when:
        domainClass.name = 'lower case'
        domainClass.age = -1  // invalid value
        domainClass.town = ''

        then:
        !domainClass.validate()
        domainClass.hasErrors()
        domainClass.errors.errorCount == 2
        domainClass.errors.getFieldError('age').rejectedValue == 'type mismatched'
        domainClass.errors.getFieldError('name').rejectedValue == 'lower case'
    }

    void 'Test that validation failures are not retained during validation'() {
        given:
        def domainClass = new MyDomainClass()

        when:
        def fieldError = new FieldError(MyValidateable.name, 'age', 'any validation failure', false, null, null, null)
        domainClass.errors.addError fieldError

        then:
        domainClass.hasErrors()
        domainClass.errors.errorCount == 1
        domainClass.errors.getFieldError('age').rejectedValue == 'any validation failure'

        when:
        domainClass.name = 'lower case'
        domainClass.age = -1  // invalid value
        domainClass.town = ''

        then:
        !domainClass.validate()
        domainClass.hasErrors()
        domainClass.errors.errorCount == 2
        domainClass.errors.getFieldError('age')?.rejectedValue == -1
        domainClass.errors.getFieldError('name').rejectedValue == 'lower case'

        when:
        domainClass.age = 1  // valid value

        then:
        !domainClass.validate()
        domainClass.hasErrors()
        domainClass.errors.errorCount == 1
        domainClass.errors.getFieldError('name').rejectedValue == 'lower case'
    }

    void 'Test that only the expected properties are constrained'() {
        when:
        def constraints = getAssociatedDomainClassFromApplication(new MyDomainClass()).getConstrainedProperties()

        then:
        constraints.size() == 4
        constraints.containsKey 'name'
        constraints.containsKey 'town'
        constraints.containsKey 'age'
        constraints.containsKey 'someProperty'

        and:
        constraints.name.appliedConstraints.size() == 2
        constraints.age.appliedConstraints.size() == 2
        constraints.town.appliedConstraints.size() == 1
        constraints.someProperty.appliedConstraints.size() == 1

        and:
        constraints.name.hasAppliedConstraint 'matches'
        constraints.name.hasAppliedConstraint 'nullable'
        constraints.age.hasAppliedConstraint 'range'
        constraints.age.hasAppliedConstraint 'nullable'
        constraints.town.hasAppliedConstraint 'nullable'
        constraints.someProperty.hasAppliedConstraint 'nullable'

        and: 'implicit defaultNullable is nullable:false'
        !constraints.name.nullable
        !constraints.age.nullable
        !constraints.town.nullable
        !constraints.someProperty.nullable
    }

    @Ignore('defaultNullable is not supported yet')
    void 'Test that constraints are nullable by default if overridden and ensure nullable:true constraint is not applied when no other constraints were defined by user'() {
        when:
        def constraints = getAssociatedDomainClassFromApplication(new MyNullableDomainClass()).getConstrainedProperties()

        then: 'not including "name" because there is no constraints by user'
        constraints.size() == 3
        constraints.containsKey 'town'
        constraints.containsKey 'age'
        constraints.containsKey 'country'

        and: 'explicit nullable constraints are correctly applied'
        constraints.town.appliedConstraints.size() == 1
        !constraints.town.nullable
        constraints.country.appliedConstraints.size() == 1
        constraints.country.nullable

        and: 'if property has any constraint defined, nullable is added too'
        constraints.age.appliedConstraints.size() == 2
        constraints.age.nullable
    }

    @Ignore('defaultNullable is not supported yet')
    void 'Test that properties defined in a class with overridden defaultNullable which are not explicitly constrained are not accessed during validation'() {
        given: 'an instance of a class with overridden defaultNullable returning true'
        def obj = new MyNullableDomainClass(town: 'St. Louis', age: 18)

        expect: 'property accessors are not invoked for properties which are not explicitly constrained (getName() would throw an exception)'
        obj.validate()
    }

    void 'Ensure class without any constraints can be validated'() {
        given:
        NoConstraintsDomainClass obj = new NoConstraintsDomainClass()

        expect:
        obj.validate()
        !obj.hasErrors()
        obj.errors != null
    }

    void 'Ensure validation may be done for classes with private and protected getters'() {
        given:
        DomainClassGetters obj = new DomainClassGetters()

        expect: 'validation is executed and public properties/getters are marked nullable in errors'
        !obj.validate()
        obj.hasErrors()
        obj.errors.errorCount == 1
        obj.errors['town']?.code == 'nullable' // public property
        !obj.errors['surname'] // only protected getter method
        !obj.errors['email'] // only private getter method
    }

    void 'Ensure private and protected getter is not handled as not-nullable property by default'() {
        when:
        Map constraints = getAssociatedDomainClassFromApplication(new DomainClassGetters()).getConstrainedProperties()

        then: 'only public properties and public getters should be considered domainClass properties by default'
        constraints.size() == 4
        constraints.town
        constraints.bar
        constraints.qux
        constraints.transientString2
    }

    void "Test that default and shared constraints can be applied from configuration"() {
        given:
        defineBeans {
            "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
                defaultConstraints = new ClosureToMapPopulator().populate {
                    '*' blank: false
                    myShared matches: /MY_SHARED/, maxSize: 10
                }
            }
        }

        and:
        def constraints = getAssociatedDomainClassFromApplication(new SharedConstraintsDomainClass()).getConstrainedProperties()

        expect:
        constraints.size() == 2
        constraints.name.hasAppliedConstraint 'matches'
        constraints.town.hasAppliedConstraint 'inList'

        and: 'default nullable'
        constraints.name.hasAppliedConstraint 'nullable'
        constraints.town.hasAppliedConstraint 'nullable'

        and: 'default constraints "*"'
        constraints.name.hasAppliedConstraint 'blank'
        constraints.town.hasAppliedConstraint 'blank'

        and: 'shared constraints'
        constraints.town.hasAppliedConstraint 'matches'
        constraints.town.hasAppliedConstraint 'maxSize'

        cleanup:
        defineBeans {
            "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
                defaultConstraints = Collections.emptyMap()
            }
        }
    }

    void 'Ensure properties of super class is inherited'() {
        when:
        def constraints = getAssociatedDomainClassFromApplication(new SubClassDomainClass()).getConstrainedProperties()

        then:
        constraints.size() == 2
        constraints.superName.appliedConstraints.size() == 2
        constraints.superName.hasAppliedConstraint 'matches'
        constraints.superName.hasAppliedConstraint 'nullable'
        constraints.subName.appliedConstraints.size() == 2
        constraints.subName.hasAppliedConstraint 'matches'
        constraints.subName.hasAppliedConstraint 'nullable'
    }

    void 'Ensure properties of super class can be validated'() {
        given:
        def obj = new SubClassDomainClass()

        expect:
        !obj.validate()
        obj.hasErrors()
        obj.errors.errorCount == 2
        obj.errors['superName']?.code == 'nullable'
        obj.errors['subName']?.code == 'nullable'
    }

    // FIXME domainClass.getConstrainedProperty() cannot be used. this is workaround.
    private GrailsDomainClass getAssociatedDomainClassFromApplication(Object associatedObject) {
        String associatedObjectType = Hibernate.getClass(associatedObject).getName();
        return (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, associatedObjectType);
    }
}

@Entity
class MyDomainClass {
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

@Entity
class MyNullableDomainClass {
    Integer age
    String town
    String country

    String getName() {
        throw new UnsupportedOperationException('getName() should not have been called during validation')
    }

    static constraints = {
        town nullable: false
        age validator: { val -> val > 0 }
        country nullable: true
    }

    static boolean defaultNullable() { // TODO not supported yet
        true
    }
}

@Entity
class NoConstraintsDomainClass {
    static constraints = {

    }
}

@Entity
class DomainClassGetters {
    //standard properties - private/protected will not have getter
    String town

    //properties from getters
    String getName() {}

    protected String getSurname() {}

    private String getEmail() {}

    //static properties
    static Integer day

    //static properties from getters
    static Date getEaster() {}

    protected static Date getChristmas() {}

    private static Date getNewYear() {}

    static transients = ['foo', 'bar']

    String getFoo() {}

    String getBar() {}

    transient String getBaz() {}

    transient String getQux() {}

    transient String transientString1;
    transient String transientString2;

    static constraints = {
        bar size: 3..10
        qux size: 3..10
        transientString2 size: 3..10
    }
}

@Entity
class SharedConstraintsDomainClass {
    String name
    String town

    static constraints = {
        name matches: /[A-Z].*/
        town shared: "myShared", inList: ['St. Louis']
    }
}

@Entity
class SuperClassDomainClass {
    String superName

    static constraints = {
        superName matches: /SUPER/
    }
}

@Entity
class SubClassDomainClass extends SuperClassDomainClass {
    String subName

    static constraints = {
        subName matches: /SUB/
    }
}
