package grails.validation

import grails.testing.gorm.DataTest
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
        [
            BoolMethodPropertiesDomain, DomainWithTransients, InheritedBoolMethodPropertiesDomain,
            InheritedDomainWithTransients, InheritedMethodPropertiesDomain, InheritedPropertiesDomain,
            MethodPropertiesDomain, SimplePropertiesDomain, TraitBoolMethodPropertiesDomain, TraitDomainWithTransients,
            TraitMethodPropertiesDomain, TraitPropertiesDomain
        ]
    }

    // STANDARD DOMAIN

    void 'ensure all public properties are by default constraint properties'() {

        given:
        def domain = new SimplePropertiesDomain()

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

        given:
        def domain = new MethodPropertiesDomain()

        when: 'empty domain with method properties is validated'
        domain.validate()

        then: 'only public with getter and setter should fail'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    void 'ensure constrained method properties are only public ones with both getter and setter'() {

        when: 'constrained properties map is get'
        def constrainedProperties = MethodPropertiesDomain.getConstrainedProperties()

        then: 'only public property with getter and setter should fail'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // DOMAIN WITH SUPER CLASS

    void 'ensure all inherited public properties are by default constraint properties'() {

        given:
        def domain = new InheritedPropertiesDomain()

        when: 'empty domain with simple properties from parent class inheriteds validated'
        domain.validate()

        then: 'all public should fail on nullable constraint'
        domain.hasErrors()
        domain.errors['string']?.code == 'nullable'
        domain.errors['pages']?.code == 'nullable'
        domain.errors.getErrorCount() == 2
    }

    void 'ensure inherited constrained properties are only public ones'() {

        when: 'constrained properties map is get on child class'
        def constrainedProperties = InheritedPropertiesDomain.getConstrainedProperties()

        then: 'only 4 defined public properties are there'
        constrainedProperties.size() == 2
        constrainedProperties.containsKey('string')
        constrainedProperties.containsKey('pages')
    }

    void 'ensure only public non-static inherited properties with getter and setter are constrained properties'() {

        given:
        def domain = new InheritedMethodPropertiesDomain()

        when: 'empty domain with method properties is validated'
        domain.validate()

        then: 'only public with getter and setter should fail'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    void 'ensure constrained inherited method properties are only public ones with both getter and setter'() {

        when: 'constrained properties map is get from child class'
        def constrainedProperties = InheritedMethodPropertiesDomain.getConstrainedProperties()

        then: 'only public with getter and setter should be there'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // DOMAIN WITH TRAIT

    void 'ensure all trait public properties are by default constraint properties'() {

        given:
        def domain = new TraitPropertiesDomain()

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
        def constrainedProperties = TraitPropertiesDomain.getConstrainedProperties()

        then: 'only 4 defined public properties are there'
        constrainedProperties.size() == 2
        constrainedProperties.containsKey('string')
        constrainedProperties.containsKey('pages')
    }

    void 'ensure only public non-static properties from trait with getter and setter are constrained properties'() {

        given:
        def domain = new TraitMethodPropertiesDomain()

        when: 'empty domain with simple properties is validated'
        domain.validate()

        then: 'all should fail on nullable constraint'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    void 'ensure constrained method properties from trait are only public ones with both getter and setter'() {

        when: 'constrained properties map is get'
        def constrainedProperties = TraitMethodPropertiesDomain.getConstrainedProperties()

        then: 'only 4 defined public properties are there'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // BOOL METHODS DOMAIN OBJECT

    @PendingFeature
    void 'ensure only public non-static bool properties with getter and setter are constrained properties'() {

        given:
        def domain = new BoolMethodPropertiesDomain()

        when: 'empty domain with method properties is validated'
        domain.validate()

        then: 'only public with getter and setter should fail'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    @PendingFeature
    void 'ensure constrained bool method properties are only public ones with both getter and setter'() {

        when: 'constrained properties map is get'
        def constrainedProperties = BoolMethodPropertiesDomain.getConstrainedProperties()

        then: 'only public property with getter and setter should fail'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // BOOL DOMAIN OBJECT WITH SUPER CLASS

    @PendingFeature
    void 'ensure only public non-static inherited bool properties with getter and setter are constrained properties'() {

        given:
        def domain = new InheritedBoolMethodPropertiesDomain()

        when: 'empty domain with method properties is validated'
        domain.validate()

        then: 'only public with getter and setter should fail'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    @PendingFeature
    void 'ensure constrained inherited bool method properties are only public ones with both getter and setter'() {

        when: 'constrained properties map is get from child class'
        def constrainedProperties = InheritedBoolMethodPropertiesDomain.getConstrainedProperties()

        then: 'only public with getter and setter should be there'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // BOOL DOMAIN OBJECT WITH TRAIT

    @PendingFeature
    void 'ensure only public non-static bool properties from trait with getter and setter are constrained properties'() {

        given:
        def domain = new TraitBoolMethodPropertiesDomain()

        when: 'empty domain with simple properties is validated'
        domain.validate()

        then: 'all should fail on nullable constraint'
        domain.hasErrors()
        domain.errors['publicProperty']?.code == 'nullable'
        domain.errors.getErrorCount() == 1
    }

    @PendingFeature
    void 'ensure constrained bool method properties from trait are only public ones with both getter and setter'() {

        when: 'constrained properties map is get'
        def constrainedProperties = TraitBoolMethodPropertiesDomain.getConstrainedProperties()

        then: 'only 4 defined public properties are there'
        constrainedProperties.size() == 1
        constrainedProperties.containsKey('publicProperty')
    }

    // DOMAIN WITH TRANSIENTS
    @PendingFeature(reason = 'With Groovy 4, transient properties and methods are currently not excluded from validation')
    void 'ensure transient properties and methods are not validated'() {

        given:
        def domain = new DomainWithTransients()

        when: 'domain with transient methods and properties is validated'
        domain.validate()

        then: 'nothing should fail'
        domain.errors.getErrorCount() == 0
    }

    @PendingFeature(reason = 'With Groovy 4, transient methods and properties are currently constrained')
    void 'ensure transient methods and properties are not constrained'() {

        when: 'constrained properties map is get'
        def constrainedProperties = DomainWithTransients.getConstrainedProperties()

        then: 'nothing is constrained'
        constrainedProperties.size() == 0
    }

    // DOMAIN WITH SUPER CLASS WITH TRANSIENTS

    @PendingFeature(reason = 'With Groovy 4, transient properties and methods are currently not excluded from validation')
    void 'ensure inherited transient properties and methods are not validated'() {

        given:
        def domain = new InheritedDomainWithTransients()

        when: 'domain with superclass properties and methods is validated'
        domain.validate()

        then: 'nothing should fail'
        domain.errors.getErrorCount() == 0
    }

    @PendingFeature(reason = 'With Groovy 4, transient properties and methods are currently not excluded from validation')
    void 'ensure inherited transient methods and properties are not constrained'() {

        when: 'constrained properties map is get'
        def constrainedProperties = InheritedDomainWithTransients.getConstrainedProperties()

        then: 'nothing is constrained'
        constrainedProperties.size() == 0
    }

    // DOMAIN WITH TRAIT WITH TRANSIENTS

    @PendingFeature
    void 'ensure trait transient properties and methods are not validated'() {

        given:
        def domain = new TraitDomainWithTransients()

        when: 'domain with trait transient properties and methods'
        domain.validate()

        then: 'nothing should fail'
        domain.errors.getErrorCount() == 0
    }

    @PendingFeature
    void 'ensure trait transient methods and properties are not constrained'() {

        when: 'constrained properties map is get'
        def constrainedProperties = TraitDomainWithTransients.getConstrainedProperties()

        then: 'nothing is constrained'
        constrainedProperties.size() == 0
    }
}

/**
 * Domain with properties only
 */
@Entity
@SuppressWarnings('unused')
class SimplePropertiesDomain {

    String string
    Integer pages

    private String firstName
    protected String secondName
    static String finalName
    private static String foo
    protected static String bar
}

// Since Groovy 4, parent domain classes cannot be annotated with @Entity (https://issues.apache.org/jira/browse/GROOVY-5106)
@SuppressWarnings('unused')
class ParentSimplePropertiesDomain {

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
@Entity
class InheritedPropertiesDomain extends ParentSimplePropertiesDomain {}

/**
 * Domain with getter/setter methods
 */
@Entity
@SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
class MethodPropertiesDomain {

    /**
     * publicProperty should be constrained because those getter and setter
     */
    String getPublicProperty() { null }
    void setPublicProperty(String value) {}

    protected String getProtectedProperty() { null }
    protected void setProtectedProperty(String value) {}

    private String getPrivateProperty() { null }
    private void setPrivateProperty(String value) {}

    static String getStaticPublicProperty() { null }
    static void setStaticPublicProperty(String value) {}

    static protected String getStaticProtectedProperty() { null }
    static protected void setStaticProtectedProperty(String value) {}

    static private String getStaticPrivateProperty() { null }
    static private void setStaticPrivateProperty(String value) {}

    String getGetterOnly() { null }

    protected String getProtectedGetterOnly() { null }

    private String getPrivateGetterOnly() { null }

    static String getStaticGetterOnly() { null }

    static protected String getStaticProtectedGetterOnly() { null }

    static private String getStaticPrivateGetterOnly() { null }

    void setSetterOnly(String value) {}

    protected void setProtectedSetterOnly(String value) {}

    private void setPrivateSetterOnly(String value) {}

    static void setStaticSetterOnly(String value) {}

    static protected void setStaticProtectedSetterOnly(String value) {}

    static private void setStaticPrivateSetterOnly(String value) {}
}

// Since Groovy 4, parent domain classes cannot be annotated with @Entity (https://issues.apache.org/jira/browse/GROOVY-5106)
@SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
class ParentMethodPropertiesDomain {

    /**
     * publicProperty should be constrained because those getter and setter
     */
    String getPublicProperty() { null }
    void setPublicProperty(String value) {}

    protected String getProtectedProperty() { null }
    protected void setProtectedProperty(String value) {}

    private String getPrivateProperty() { null }
    private void setPrivateProperty(String value) {}

    static String getStaticPublicProperty() { null }
    static void setStaticPublicProperty(String value) {}

    static protected String getStaticProtectedProperty() { null }
    static protected void setStaticProtectedProperty(String value) {}

    static private String getStaticPrivateProperty() { null }
    static private void setStaticPrivateProperty(String value) {}

    String getGetterOnly() { null }

    protected String getProtectedGetterOnly() { null }

    private String getPrivateGetterOnly() { null }

    static String getStaticGetterOnly() { null }

    static protected String getStaticProtectedGetterOnly() { null }

    static private String getStaticPrivateGetterOnly() { null }

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
@Entity
class InheritedMethodPropertiesDomain extends ParentMethodPropertiesDomain {}

/**
 * Trait with properties only
 */
@SuppressWarnings('unused')
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
@SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
trait MethodPropertiesDomainTrait {

    /**
     * publicProperty should be constrained because those getter and setter
     */
    String getPublicProperty() { null }
    void setPublicProperty(String value) {}

    private String getPrivateProperty() { null }
    private void setPrivateProperty(String value) {}

    static String getStaticPublicProperty() { null }
    static void setStaticPublicProperty(String value) {}

    static private String getStaticPrivateProperty() { null }
    static private void setStaticPrivateProperty(String value) {}

    String getGetterOnly() { null }

    private String getPrivateGetterOnly() { null }

    static String getStaticGetterOnly() { null }

    static private String getStaticPrivateGetterOnly() { null }

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
@SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
class BoolMethodPropertiesDomain {

    /**
     * publicProperty should be constrained because those getter and setter
     */
    Boolean isPublicProperty() { null }
    void setPublicProperty(Boolean value) {}

    protected Boolean isProtectedProperty() { null }
    protected void setProtectedProperty(Boolean value) {}

    private Boolean isPrivateProperty() { null }
    private void setPrivateProperty(Boolean value) {}

    static Boolean isStaticPublicProperty() { null }
    static void setStaticPublicProperty(Boolean value) {}

    static protected Boolean isStaticProtectedProperty() { null }
    static protected void setStaticProtectedProperty(Boolean value) {}

    static private Boolean isStaticPrivateProperty() { null }
    static private void setStaticPrivateProperty(Boolean value) {}

    Boolean isGetterOnly() { null }

    protected Boolean isProtectedGetterOnly() { null }

    private Boolean isPrivateGetterOnly() { null }

    static Boolean isStaticGetterOnly() { null }

    static protected Boolean isStaticProtectedGetterOnly() { null }

    static private Boolean isStaticPrivateGetterOnly() { null }

    void setSetterOnly(Boolean value) {}

    protected void setProtectedSetterOnly(Boolean value) {}

    private void setPrivateSetterOnly(Boolean value) {}

    static void setStaticSetterOnly(Boolean value) {}

    static protected void setStaticProtectedSetterOnly(Boolean value) {}

    static private void setStaticPrivateSetterOnly(Boolean value) {}
}

// Since Groovy 4, parent domain classes cannot be annotated with @Entity (https://issues.apache.org/jira/browse/GROOVY-5106)
@SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
class ParentBoolMethodPropertiesDomain {

    /**
     * publicProperty should be constrained because those getter and setter
     */
    Boolean isPublicProperty() { null }
    void setPublicProperty(Boolean value) {}

    protected Boolean isProtectedProperty() { null }
    protected void setProtectedProperty(Boolean value) {}

    private Boolean isPrivateProperty() { null }
    private void setPrivateProperty(Boolean value) {}

    static Boolean isStaticPublicProperty() { null }
    static void setStaticPublicProperty(Boolean value) {}

    static protected Boolean isStaticProtectedProperty() { null }
    static protected void setStaticProtectedProperty(Boolean value) {}

    static private Boolean isStaticPrivateProperty() { null }
    static private void setStaticPrivateProperty(Boolean value) {}

    Boolean isGetterOnly() { null }

    protected Boolean isProtectedGetterOnly() { null }

    private Boolean isPrivateGetterOnly() { null }

    static Boolean isStaticGetterOnly() { null }

    static protected Boolean isStaticProtectedGetterOnly() { null }

    static private Boolean isStaticPrivateGetterOnly() { null }

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
@SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
trait BoolMethodPropertiesDomainTrait {

    /**
     * publicProperty should be constrained because those getter and setter
     */
    Boolean isPublicProperty() { null }
    void setPublicProperty(Boolean value) {}

    private Boolean isPrivateProperty() { null }
    private void setPrivateProperty(Boolean value) {}

    static Boolean isStaticPublicProperty() { null }
    static void setStaticPublicProperty(Boolean value) {}

    static private Boolean isStaticPrivateProperty() { null }
    static private void setStaticPrivateProperty(Boolean value) {}

    Boolean isGetterOnly() { null }

    private Boolean isPrivateGetterOnly() { null }

    static Boolean isStaticGetterOnly() { null }

    static private Boolean isStaticPrivateGetterOnly() { null }

    void setSetterOnly(Boolean value) {}

    private void setPrivateSetterOnly(Boolean value) {}

    static void setStaticSetterOnly(Boolean value) {}

    static private void setStaticPrivateSetterOnly(Boolean value) {}
}

/**
 * Domain with inherited bool method properties from super class
 */
@Entity
class InheritedBoolMethodPropertiesDomain extends ParentBoolMethodPropertiesDomain {}

/**
 * Domain with inherited bool method properties from trait
 */
@Entity
class TraitBoolMethodPropertiesDomain implements BoolMethodPropertiesDomainTrait {}

@Entity
@SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
class DomainWithTransients {

    static transients = ['simpleProperty', 'methodProperty', 'boolMethodProperty']

    String simpleProperty

    String getMethodProperty() { null }
    void setMethodProperty(String value) {}

    String getTransientMethodProperty() { null }
    void setTransientMethodProperty(String value) {}

    Boolean isBoolMethodProperty() { null }
    void setBoolMethodProperty(Boolean value) {}

    Boolean isTransientBoolMethodProperty() { null }
    void setTransientBoolMethodProperty(Boolean value) {}
}

// Since Groovy 4, parent domain classes cannot be annotated with @Entity (https://issues.apache.org/jira/browse/GROOVY-5106)
@SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
class ParentDomainWithTransients {

    static transients = ['simpleProperty', 'methodProperty', 'boolMethodProperty']

    String simpleProperty

    String getMethodProperty() { null }
    void setMethodProperty(String value) {}

    String getTransientMethodProperty() { null }
    void setTransientMethodProperty(String value) {}

    Boolean isBoolMethodProperty() { null }
    void setBoolMethodProperty(Boolean value) {}

    Boolean isTransientBoolMethodProperty() { null }
    void setTransientBoolMethodProperty(Boolean value) {}
}

@SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
trait TraitWithTransients {

    static transients = ['simpleProperty', 'methodProperty', 'boolMethodProperty']

    String simpleProperty

    String getMethodProperty() { null }
    void setMethodProperty(String value) {}

    transient String getTransientMethodProperty() { null }
    transient void setTransientMethodProperty(String value) {}

    Boolean isBoolMethodProperty() { null }
    void setBoolMethodProperty(Boolean value) {}

    transient Boolean isTransientBoolMethodProperty() { null }
    transient void setTransientBoolMethodProperty(Boolean value) {}
}


@Entity
class InheritedDomainWithTransients extends ParentDomainWithTransients {}

@Entity
class TraitDomainWithTransients implements TraitWithTransients {}