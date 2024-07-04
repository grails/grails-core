package grails.validation

import grails.testing.gorm.DataTest
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.PendingFeature
import spock.lang.Specification
import grails.persistence.Entity

/**
 * Test is similar to CommandObjectConstraintGettersSpec but for domain classes.
 * Check more detailed description in CommandObjectConstraintGettersSpec
 *
 */
@Issue(['grails/grails-core#9749', 'grails/grails-core#9754'])
class DomainConstraintGettersSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {
        [SimplePropertiesDomain, MethodPropertiesDomain, InheritedPropertiesDomain,
         InheritedMethodPropertiesDomain, TraitPropertiesDomain, TraitMethodPropertiesDomain,
         BoolMethodPropertiesDomain, InheritedBoolMethodPropertiesDomain, TraitBoolMethodPropertiesDomain,
         DomainWithTransients, InheritedDomainWithTransients, TraitDomainWithTransients]
    }

    // STANDARD DOMAIN
    void 'ensure all public properties are by default constraint properties'() {
        SimplePropertiesDomain domain = new SimplePropertiesDomain()

        when: 'empty domain with simple properties is validated'
        domain.validate()

        then: 'only public should fail on nullable constraint'
        domain.hasErrors()
        domain.errors['string']?.code == 'nullable'
        domain.errors['pages']?.code == 'nullable'
        domain.errors.getErrorCount() == 2
    }

    void 'ensure constrained properties are only public ones'() {
        when: 'constrained properties map is get'
        Map constrainedProperties = SimplePropertiesDomain.getConstrainedProperties()

        then: 'only 4 defined public properties are there'
        constrainedProperties.size() == 2
        constrainedProperties.containsKey('string')
        constrainedProperties.containsKey('pages')
    }

    void 'ensure only public non-static properties with getter and setter are constrained properties'() {
        MethodPropertiesDomain domain = new MethodPropertiesDomain()
        when: 'empty domain with method properties is validated'
        domain.validate()

        then: 'only public with getter and setter should fail'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    void 'ensure constrained method properties are only public ones with both getter and setter'() {
        when: 'constrained properties map is get'
        Map constrainedProperties = MethodPropertiesDomain.getConstrainedProperties()

        then: 'only public property with getter and setter should fail'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // DOMAIN WITH SUPER CLASS

    @PendingFeature(reason = 'With Groovy 4, it is currently not possible to extend domain classes: https://issues.apache.org/jira/browse/GROOVY-5106')
    void 'ensure all inherited public properties are by default constraint properties'() {
        InheritedPropertiesDomain domain = new InheritedPropertiesDomain()

        when: 'empty domain with simple properties from parent class inheriteds validated'
        domain.validate()

        then: 'all public should fail on nullable constraint'
        domain.hasErrors()
        domain.errors['string']?.code == 'nullable'
        domain.errors['pages']?.code == 'nullable'
        domain.errors.getErrorCount() == 2
    }

    @PendingFeature(reason = 'With Groovy 4, it is currently not possible to extend domain classes: https://issues.apache.org/jira/browse/GROOVY-5106')
    void 'ensure inherited constrained properties are only public ones'() {
        when: 'constrained properties map is get on child class'
        Map constrainedProperties = InheritedPropertiesDomain.getConstrainedProperties()

        then: 'only 4 defined public properties are there'
        constrainedProperties.size() == 2
        constrainedProperties.containsKey('string')
        constrainedProperties.containsKey('pages')
    }

    @PendingFeature(reason = 'With Groovy 4, it is currently not possible to extend domain classes: https://issues.apache.org/jira/browse/GROOVY-5106')
    void 'ensure only public non-static inherited properties with getter and setter are constrained properties'() {
        InheritedMethodPropertiesDomain domain = new InheritedMethodPropertiesDomain()
        when: 'empty domain with method properties is validated'
        domain.validate()

        then: 'only public with getter and setter should fail'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    @PendingFeature(reason = 'With Groovy 4, it is currently not possible to extend domain classes: https://issues.apache.org/jira/browse/GROOVY-5106')
    void 'ensure constrained inherited method properties are only public ones with both getter and setter'() {
        when: 'constrained properties map is get from child class'
        Map constrainedProperties = InheritedMethodPropertiesDomain.getConstrainedProperties()

        then: 'only public with getter and setter should be there'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // DOMAIN WITH TRAIT

    void 'ensure all trait public properties are by default constraint properties'() {
        TraitPropertiesDomain domain = new TraitPropertiesDomain()

        when: 'empty domain with trait properties is validated'
        domain.validate()

        then: 'only public should fail on nullable constraint'
        domain.hasErrors()
        domain.errors['string']?.code == 'nullable'
        domain.errors['pages']?.code == 'nullable'
        domain.errors.getErrorCount() == 2
    }

    void 'ensure constrained properties are only traits public ones'() {
        when: 'constrained properties map is get'
        Map constrainedProperties = TraitPropertiesDomain.getConstrainedProperties()

        then: 'only 4 defined public properties are there'
        constrainedProperties.size() == 2
        constrainedProperties.containsKey('string')
        constrainedProperties.containsKey('pages')
    }

    void 'ensure only public non-static properties from trait with getter and setter are constrained properties'() {
        TraitMethodPropertiesDomain domain = new TraitMethodPropertiesDomain()
        when: 'empty domain with simple properties is validated'
        domain.validate()

        then: 'all should fail on nullable constraint'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    void 'ensure constrained method properties from trait are only public ones with both getter and setter'() {
        when: 'constrained properties map is get'
        Map constrainedProperties = TraitMethodPropertiesDomain.getConstrainedProperties()

        then: 'only 4 defined public properties are there'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // BOOL METHODS DOMAIN OBJECT

    @Ignore
    void 'ensure only public non-static bool properties with getter and setter are constrained properties'() {
        BoolMethodPropertiesDomain domain = new BoolMethodPropertiesDomain()
        when: 'empty domain with method properties is validated'
        domain.validate()

        then: 'only public with getter and setter should fail'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    @Ignore
    void 'ensure constrained bool method properties are only public ones with both getter and setter'() {
        when: 'constrained properties map is get'
        Map constrainedProperties = BoolMethodPropertiesDomain.getConstrainedProperties()

        then: 'only public property with getter and setter should fail'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // BOOL DOMAIN OBJECT WITH SUPER CLASS

    @Ignore
    void 'ensure only public non-static inherited bool properties with getter and setter are constrained properties'() {
        InheritedBoolMethodPropertiesDomain domain = new InheritedBoolMethodPropertiesDomain()
        when: 'empty domain with method properties is validated'
        domain.validate()

        then: 'only public with getter and setter should fail'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    @Ignore
    void 'ensure constrained inherited bool method properties are only public ones with both getter and setter'() {
        when: 'constrained properties map is get from child class'
        Map constrainedProperties = InheritedBoolMethodPropertiesDomain.getConstrainedProperties()

        then: 'only public with getter and setter should be there'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // BOOL DOMAIN OBJECT WITH TRAIT

    @Ignore
    void 'ensure only public non-static bool properties from trait with getter and setter are constrained properties'() {
        TraitBoolMethodPropertiesDomain domain = new TraitBoolMethodPropertiesDomain()
        when: 'empty domain with simple properties is validated'
        domain.validate()

        then: 'all should fail on nullable constraint'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    @Ignore
    void 'ensure constrained bool method properties from trait are only public ones with both getter and setter'() {
        when: 'constrained properties map is get'
        Map constrainedProperties = TraitBoolMethodPropertiesDomain.getConstrainedProperties()

        then: 'only 4 defined public properties are there'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // DOMAIN WITH TRANSIENTS
    @PendingFeature(reason = 'With Groovy 4, transient properties and methods are currently not excluded from validation')
    void 'ensure transient properties and methods are not validated'() {
        DomainWithTransients domain = new DomainWithTransients()
        when: 'domain with transient methods and properties is validated'
        domain.validate()

        then: 'nothing should fail'
        domain.errors.getErrorCount() == 0
    }

    @PendingFeature(reason = 'With Groovy 4, transient methods and properties are currently constrained')
    void 'ensure transient methods and properties are not constrained'() {
        when: 'constrained properties map is get'
        Map constrainedProperties = DomainWithTransients.getConstrainedProperties()

        then: 'nothing is constrained'
        constrainedProperties.size() == 0
    }

    // DOMAIN WITH SUPER CLASS WITH TRANSIENTS
    @Ignore('''
        With Groovy 4, it is currently not possible to extend domain classes: https://issues.apache.org/jira/browse/GROOVY-5106
        Can't use @PendingFeature as the test currently passes as the domain class cannot currently be extended.
    ''')
    void 'ensure inherited transient properties and methods are not validated'() {
        def domain = new InheritedDomainWithTransients()
        when: 'domain with superclass properties and methods is validated'
        domain.validate()

        then: 'nothing should fail'
        domain.errors.getErrorCount() == 0
    }

    @Ignore('''
        With Groovy 4, it is currently not possible to extend domain classes: https://issues.apache.org/jira/browse/GROOVY-5106
        Can't use @PendingFeature as the test currently passes as the domain class cannot currently be extended.
    ''')
    void 'ensure inherited transient methods and properties are not constrained'() {
        when: 'constrained properties map is get'
        Map constrainedProperties = InheritedDomainWithTransients.getConstrainedProperties()

        then: 'nothing is constrained'
        constrainedProperties.size() == 0
    }

    // DOMAIN WITH TRAIT WITH TRANSIENTS

    @Ignore
    void 'ensure trait transient properties and methods are not validated'() {
        TraitDomainWithTransients domain = new TraitDomainWithTransients()
        when: 'domain with trait transient properties and methods'
        domain.validate()

        then: 'nothing should fail'
        domain.errors.getErrorCount() == 0
    }

    @Ignore
    void 'ensure trait transient methods and properties are not constrained'() {
        when: 'constrained properties map is get'
        Map constrainedProperties = TraitDomainWithTransients.getConstrainedProperties()

        then: 'nothing is constrained'
        constrainedProperties.size() == 0
    }
}

/**
 * Domain with properties only
 */
@Entity
class SimplePropertiesDomain {
    String string
    Integer pages

    private String firstName
    protected String secondName
    static String finalName
    private static String foo
    protected static String bar
}

/**
 * Domain with properties from super class only
 */
// With Groovy 4, it is currently not possible to extend domain classes: https://issues.apache.org/jira/browse/GROOVY-5106
@Entity
class InheritedPropertiesDomain // extends SimplePropertiesDomain
{}

/**
 * Domain with getter/setter methods
 */
@Entity
class MethodPropertiesDomain {

    /**
     * publicProperty should be constrained because those getter and setter
     */
    String getPublicProperty() {}

    void setPublicProperty(String value) {}

    protected String getProtectedProperty() {}

    protected void setProtectedProperty(String value) {}

    private String getPrivateProperty() {}

    private void setPrivateProperty(String value) {}

    static String getStaticPublicProperty() {}

    static void setStaticPublicProperty(String value) {}

    static protected String getStaticProtectedProperty() {}

    static protected void setStaticProtectedProperty(String value) {}

    static private String getStaticPrivateProperty() {}

    static private void setStaticPrivateProperty(String value) {}

    String getGetterOnly() {}

    protected String getProtectedGetterOnly() {}

    private String getPrivateGetterOnly() {}

    static String getStaticGetterOnly() {}

    static protected String getStaticProtectedGetterOnly() {}

    static private String getStaticPrivateGetterOnly() {}

    void setSetterOnly(String value) {}

    protected void setProtectedSetterOnly(String value) {}

    private void setPrivateSetterOnly(String value) {}

    static void setStaticSetterOnly(String value) {}

    static protected void setStaticProtectedSetterOnly(String value) {}

    static private void setStaticPrivateSetterOnly(String value) {}
}

/**
 * Domain with method properties from super class
 */
// With Groovy 4, it is currently not possible to extend domain classes: https://issues.apache.org/jira/browse/GROOVY-5106
@Entity
class InheritedMethodPropertiesDomain // extends MethodPropertiesDomain
{}

/**
 * Trait with properties only
 */
trait SimpleDomainPropertiesTrait {
    String string
    Integer pages

    private String firstName
    static String finalName
    private static String foo
}

/**
 * Trait with getter/setter methods
 */
trait MethodPropertiesDomainTrait {

    /**
     * publicProperty should be constrained because those getter and setter
     */
    String getPublicProperty() {}

    void setPublicProperty(String value) {}

    private String getPrivateProperty() {}

    private void setPrivateProperty(String value) {}

    static String getStaticPublicProperty() {}

    static void setStaticPublicProperty(String value) {}

    static private String getStaticPrivateProperty() {}

    static private void setStaticPrivateProperty(String value) {}

    String getGetterOnly() {}

    private String getPrivateGetterOnly() {}

    static String getStaticGetterOnly() {}

    static private String getStaticPrivateGetterOnly() {}

    void setSetterOnly(String value) {}

    private void setPrivateSetterOnly(String value) {}

    static void setStaticSetterOnly(String value) {}

    static private void setStaticPrivateSetterOnly(String value) {}
}

/**
 * Domain with properties from trait
 */
@Entity
class TraitPropertiesDomain implements SimpleDomainPropertiesTrait {}

/**
 * Domain with method properties from trait
 */
@Entity
class TraitMethodPropertiesDomain implements MethodPropertiesDomainTrait {}

/**
 * Domain with bool methods - `is` instead of `get`
 */
@Entity
class BoolMethodPropertiesDomain {

    /**
     * publicProperty should be constrained because those getter and setter
     */
    Boolean isPublicProperty() {}

    void setPublicProperty(Boolean value) {}

    protected Boolean isProtectedProperty() {}

    protected void setProtectedProperty(Boolean value) {}

    private Boolean isPrivateProperty() {}

    private void setPrivateProperty(Boolean value) {}

    static Boolean isStaticPublicProperty() {}

    static void setStaticPublicProperty(Boolean value) {}

    static protected Boolean isStaticProtectedProperty() {}

    static protected void setStaticProtectedProperty(Boolean value) {}

    static private Boolean isStaticPrivateProperty() {}

    static private void setStaticPrivateProperty(Boolean value) {}

    Boolean isGetterOnly() {}

    protected Boolean isProtectedGetterOnly() {}

    private Boolean isPrivateGetterOnly() {}

    static Boolean isStaticGetterOnly() {}

    static protected Boolean isStaticProtectedGetterOnly() {}

    static private Boolean isStaticPrivateGetterOnly() {}

    void setSetterOnly(Boolean value) {}

    protected void setProtectedSetterOnly(Boolean value) {}

    private void setPrivateSetterOnly(Boolean value) {}

    static void setStaticSetterOnly(Boolean value) {}

    static protected void setStaticProtectedSetterOnly(Boolean value) {}

    static private void setStaticPrivateSetterOnly(Boolean value) {}
}

/**
 * Trait with getter/setter methods
 */
trait BoolMethodPropertiesDomainTrait {

    /**
     * publicProperty should be constrained because those getter and setter
     */
    Boolean isPublicProperty() {}

    void setPublicProperty(Boolean value) {}

    private Boolean isPrivateProperty() {}

    private void setPrivateProperty(Boolean value) {}

    static Boolean isStaticPublicProperty() {}

    static void setStaticPublicProperty(Boolean value) {}

    static private Boolean isStaticPrivateProperty() {}

    static private void setStaticPrivateProperty(Boolean value) {}

    Boolean isGetterOnly() {}

    private Boolean isPrivateGetterOnly() {}

    static Boolean isStaticGetterOnly() {}

    static private Boolean isStaticPrivateGetterOnly() {}

    void setSetterOnly(Boolean value) {}

    private void setPrivateSetterOnly(Boolean value) {}

    static void setStaticSetterOnly(Boolean value) {}

    static private void setStaticPrivateSetterOnly(Boolean value) {}
}

/**
 * Domain with inherited bool method properties from super class
 */
//With Groovy 4, it is currently not possible to extend domain classes: https://issues.apache.org/jira/browse/GROOVY-5106
@Entity
class InheritedBoolMethodPropertiesDomain // extends BoolMethodPropertiesDomain
{}

/**
 * Domain with inherited bool method properties from trait
 */
@Entity
class TraitBoolMethodPropertiesDomain implements BoolMethodPropertiesDomainTrait {}

@Entity
class DomainWithTransients {
    static transients = ['simpleProperty', 'methodProperty', 'boolMethodProperty']

    String simpleProperty

    String getMethodProperty() {}

    void setMethodProperty(String value) {}

    String getTransientMethodProperty() {}

    void setTransientMethodProperty(String value) {}

    Boolean isBoolMethodProperty() {}

    void setBoolMethodProperty(Boolean value) {}

    Boolean isTransientBoolMethodProperty() {}

    void setTransientBoolMethodProperty(Boolean value) {}
}

trait TraitWithTransients {
    static transients = ['simpleProperty', 'methodProperty', 'boolMethodProperty']

    String simpleProperty

    String getMethodProperty() {}

    void setMethodProperty(String value) {}

    transient String getTransientMethodProperty() {}

    transient void setTransientMethodProperty(String value) {}

    Boolean isBoolMethodProperty() {}

    void setBoolMethodProperty(Boolean value) {}

    transient Boolean isTransientBoolMethodProperty() {}

    transient void setTransientBoolMethodProperty(Boolean value) {}
}

// With Groovy 4, it is currently not possible to extend domain classes: https://issues.apache.org/jira/browse/GROOVY-5106
@Entity
class InheritedDomainWithTransients // extends DomainWithTransients
{}

@Entity
class TraitDomainWithTransients implements TraitWithTransients {}