package org.grails.validation

import grails.persistence.Entity
import grails.test.mixin.TestFor
import grails.validation.Validateable

import static org.junit.Assert.*

/**
 * Tests constraints building specific for command objects
 */
@TestFor(ConstraintsPerson)
class ConstrainedPropertyBuilderForCommandsTests {

    void testImportFrom_AllConstraints_ConstraintsExist() {
        def personCommandConstraints = PersonAllConstraintsNoNormalConstraintsCommand.constraintsMap
        assertNotNull personCommandConstraints
        assertEquals 5, personCommandConstraints.size()
        assertNull personCommandConstraints.get("importFrom")
        assertNotNull personCommandConstraints.get("email")
    }

    void testImportFrom_AllConstraints_Validation() {
        def personCommand = new PersonAllConstraintsNoNormalConstraintsCommand()

        personCommand.firstName = "firstName"
        personCommand.lastName = "lastName"
        personCommand.validate()

        assertFalse personCommand.hasErrors()

        personCommand.clearErrors()
        personCommand.firstName = null
        personCommand.validate()

        assertTrue personCommand.hasErrors()
        assertEquals 1, personCommand.getErrors().getErrorCount()
        assertEquals 1, personCommand.getErrors().getFieldErrors("firstName").size()
        assertNull personCommand.getErrors().getFieldErrors("firstName")[0].getRejectedValue()
    }

    void testImportFrom_SomeConstraints_ConstraintsExist() {
        def personCommandConstraints = PersonSomeConstraintsNoNormalConstraintsCommand.constraintsMap

        assertNotNull personCommandConstraints
        assertEquals 2, personCommandConstraints.size()
        assertNull personCommandConstraints.get("importFrom")
        assertNotNull personCommandConstraints.get("firstName")
    }

    void testImportFrom_SomeConstraints_Validation() {
        def personCommand = new PersonSomeConstraintsNoNormalConstraintsCommand()

        personCommand.firstName = "firstName"
        personCommand.lastName = "lastName"
        personCommand.validate()

        assertFalse personCommand.hasErrors()

        personCommand.clearErrors()
        personCommand.firstName = null
        personCommand.validate()

        assertTrue personCommand.hasErrors()
        assertEquals(1, personCommand.getErrors().getErrorCount())
        assertEquals(1, personCommand.getErrors().getFieldErrors("firstName").size())
        assertNull personCommand.getErrors().getFieldErrors("firstName")[0].getRejectedValue()

        // Now check that everything is ok with domain class
        def person = new ConstraintsPerson()

        person.firstName = "firstName"
        person.lastName = "lastName"
        person.email = "someemail@some.net"
        person.validate()

        assertFalse(person.hasErrors())

        person.clearErrors()
        person.email = "wrongEmail"
        person.validate()

        assertTrue(person.hasErrors())
        assertEquals(1, person.getErrors().getErrorCount())
        assertEquals(1, person.getErrors().getFieldErrors("email").size())
        assertEquals("wrongEmail", person.getErrors().getFieldErrors("email")[0].getRejectedValue())
    }

    void testImportFrom_AllConstraints_ConstraintsExist_NormalConstraintsFirst() {
        def personCommandConstraints = PersonAllConstraintsWithNormalConstraintsFirstCommand.constraintsMap

        assertNotNull personCommandConstraints
        assertEquals 5, personCommandConstraints.size()
        assertNull personCommandConstraints.get("importFrom")
        assertNotNull personCommandConstraints.get("telephone")

        assertEquals(30, personCommandConstraints.get("firstName").getAppliedConstraint("maxSize").getParameter())
        assertEquals(50, personCommandConstraints.get("lastName").getAppliedConstraint("maxSize").getParameter())
        assertEquals(
                "123123",
                personCommandConstraints.get("telephone").getAppliedConstraint("matches").getParameter())
    }

    void testImportFrom_AllConstraints_Validation_NormalConstraintsFirst() {
        def personCommand = new PersonAllConstraintsWithNormalConstraintsFirstCommand()

        personCommand.firstName = "firstName"
        personCommand.lastName = "lastName"
        personCommand.validate()

        assertFalse personCommand.hasErrors()

        personCommand.clearErrors()
        personCommand.firstName = null
        personCommand.lastName = null
        personCommand.validate()

        assertTrue personCommand.hasErrors()
        assertEquals 2, personCommand.getErrors().getErrorCount()

        // Now check that everything is ok with domain class
        def person = new ConstraintsPerson()

        person.firstName = "firstName"
        person.lastName = "lastName"
        person.email = "someemail@some.net"
        person.validate()

        assertFalse(person.hasErrors())

        person.clearErrors()
        person.firstName  = null
        person.email = "wrongEmail"
        person.validate()

        assertTrue(person.hasErrors())
        assertEquals 2, person.getErrors().getErrorCount()
        assertEquals 1, person.getErrors().getFieldErrors("firstName").size()
        assertNull person.getErrors().getFieldErrors("firstName")[0].getRejectedValue()
        assertEquals 1, person.getErrors().getFieldErrors("email").size()
        assertEquals "wrongEmail", person.getErrors().getFieldErrors("email")[0].getRejectedValue()
    }

    void testImportFrom_AllConstraints_ConstraintsExist_NormalConstraintsLast() {
        def personCommandConstraints = PersonAllConstraintsWithNormalConstraintsLastCommand.constraintsMap

        assertNotNull personCommandConstraints
        assertEquals 5, personCommandConstraints.size()
        assertNull personCommandConstraints.get("importFrom")
        assertNotNull personCommandConstraints.get("telephone")

        assertEquals 10, personCommandConstraints.get("firstName").getAppliedConstraint("maxSize").getParameter()
        assertEquals 20, personCommandConstraints.get("lastName").getAppliedConstraint("maxSize").getParameter()
        assertEquals "123123",
                     personCommandConstraints.get("telephone").getAppliedConstraint("matches").getParameter()
    }

    void testImportFrom_AllConstraints_Validation_NormalConstraintsLast() {
        def personCommand = new PersonAllConstraintsWithNormalConstraintsLastCommand()

        personCommand.firstName = null
        personCommand.lastName = null
        personCommand.email = "someemail@some.net"
        personCommand.validate()

        assertFalse personCommand.hasErrors()

        personCommand.clearErrors()
        personCommand.firstName = null
        personCommand.lastName = null
        personCommand.email = "wrongEmail"
        personCommand.validate()

        assertTrue personCommand.hasErrors()
        assertEquals 1, personCommand.getErrors().getErrorCount()

        // Now check that everything is ok with domain class
        def person = new ConstraintsPerson()

        person.firstName = "firstName"
        person.lastName = "lastName"
        person.email = "someemail@some.net"
        person.validate()

        assertFalse(person.hasErrors())

        person.clearErrors()
        person.firstName  = null
        person.email = "wrongEmail"
        person.validate()

        assertTrue(person.hasErrors())
        assertEquals 2, person.getErrors().getErrorCount()
        assertEquals 1, person.getErrors().getFieldErrors("firstName").size()
        assertNull person.getErrors().getFieldErrors("firstName")[0].getRejectedValue()
        assertEquals 1, person.getErrors().getFieldErrors("email").size()
        assertEquals "wrongEmail", person.getErrors().getFieldErrors("email")[0].getRejectedValue()
    }

    void testImportFrom_AllConstraints_ConstraintsExist_Including() {
        def personCommandConstraints = PersonAllConstraintsNoNormalConstraintsIncludingCommand.constraintsMap

        assertNotNull personCommandConstraints
        assertEquals 5, personCommandConstraints.size()
        assertNull personCommandConstraints.get("importFrom")
        assertNotNull personCommandConstraints.get("firstName")

        def emailConstraint = personCommandConstraints.get('email')
        assertNotNull emailConstraint
        assertFalse emailConstraint.hasAppliedConstraint('email')
        assertFalse emailConstraint.hasAppliedConstraint('blank')
        assertTrue emailConstraint.hasAppliedConstraint('nullable')
    }

    void testImportFrom_AllConstraints_ConstraintsExist_Excluding() {
        def personCommandConstraints = PersonAllConstraintsNoNormalConstraintsExcludingCommand.constraintsMap

        assertNotNull personCommandConstraints
        assertNull personCommandConstraints.get("importFrom")
        assertEquals 5, personCommandConstraints.size()

        def firstNameConstraint = personCommandConstraints.get("firstName")
        assertNotNull firstNameConstraint
        assertTrue firstNameConstraint.hasAppliedConstraint('nullable')
        assertFalse firstNameConstraint.hasAppliedConstraint('maxSize')

        def lastNameConstraint = personCommandConstraints.get("lastName")
        assertNotNull lastNameConstraint
        assertTrue lastNameConstraint.hasAppliedConstraint('nullable')
        assertFalse lastNameConstraint.hasAppliedConstraint('maxSize')

        def emailConstraint = personCommandConstraints.get("email")
        assertNotNull emailConstraint
        assertTrue emailConstraint.hasAppliedConstraint('email')
    }

    void testImportFrom_AllConstraints_ConstraintsExist_IncludingByRegexp() {
        def personCommandConstraints = PersonAllConstraintsNoNormalConstraintsIncludingByRegexpCommand.constraintsMap

        assertNotNull personCommandConstraints
        assertEquals 5, personCommandConstraints.size()
        assertNull personCommandConstraints.get("importFrom")
        assertNotNull personCommandConstraints.get("firstName")
        assertNotNull personCommandConstraints.get("lastName")
        assertNotNull personCommandConstraints.get("middleName")

        def emailConstraint = personCommandConstraints.get('email')
        assertNotNull emailConstraint
        assertFalse emailConstraint.hasAppliedConstraint('email')
        assertFalse emailConstraint.hasAppliedConstraint('blank')
        assertTrue emailConstraint.hasAppliedConstraint('nullable')
    }

    void testImportFrom_AllConstraints_ConstraintsExist_IncludingExcludingByRegexp() {
        def personCommandConstraints = PersonAllConstraintsNoNormalConstraintsIncludingExcludingByRegexpCommand.constraintsMap

        assertNotNull personCommandConstraints
        assertEquals 5, personCommandConstraints.size()
        assertNull personCommandConstraints.get("importFrom")
        assertNotNull personCommandConstraints.get("firstName")
        assertNotNull personCommandConstraints.get("lastName")

        def emailConstraint = personCommandConstraints.get('email')
        assertNotNull emailConstraint
        assertFalse emailConstraint.hasAppliedConstraint('email')
        assertFalse emailConstraint.hasAppliedConstraint('blank')
        assertTrue emailConstraint.hasAppliedConstraint('nullable')
    }
}

@Entity
class ConstraintsPerson {
    String firstName
    String lastName
    String middleName
    String telephone
    String email

    static constraints = {
        firstName(nullable:false, blank:false, maxSize:30)
        lastName(nullable:false, blank:false, maxSize:50)
        middleName(nullable:true, blank:false, notEqual:"myMiddleName")
        telephone(nullable:true, blank:false, matches:"123123")
        email(nullable:true, blank:false, email:true)
    }
}

class PersonAllConstraintsNoNormalConstraintsCommand implements Validateable {
    String firstName
    String lastName
    String middleName
    String telephone
    String email

    static constraints = {
        importFrom ConstraintsPerson
    }
}

class PersonSomeConstraintsNoNormalConstraintsCommand implements Validateable {
    String firstName
    String lastName

    static constraints = {
        importFrom ConstraintsPerson
    }
}

class PersonAllConstraintsWithNormalConstraintsFirstCommand implements Validateable {
    String firstName
    String lastName
    String middleName
    String telephone
    String email

    static constraints = {
        firstName(nullable:true, blank:true, maxSize:10)
        lastName(nullable:true, blank:true, maxSize:20)
        email(nullable:false, blank:true, email:true)

        importFrom ConstraintsPerson
    }
}

class PersonAllConstraintsWithNormalConstraintsLastCommand implements Validateable {
    String firstName
    String lastName
    String middleName
    String telephone
    String email

    static constraints = {
        importFrom ConstraintsPerson

        firstName(nullable:true, blank:true, maxSize:10)
        lastName(nullable:true, blank:true, maxSize:20)
        email(nullable:false, blank:true, email:true)
    }
}

class PersonAllConstraintsNoNormalConstraintsIncludingCommand implements Validateable {
    String firstName
    String lastName
    String middleName
    String telephone
    String email

    static constraints = {
        importFrom ConstraintsPerson, include:["firstName", "lastName"]
    }
}

class PersonAllConstraintsNoNormalConstraintsExcludingCommand implements Validateable {
    String firstName
    String lastName
    String middleName
    String telephone
    String email

    static constraints = {
        importFrom ConstraintsPerson, exclude:["firstName", "lastName"]
    }
}

class PersonAllConstraintsNoNormalConstraintsIncludingByRegexpCommand implements Validateable {
    String firstName
    String lastName
    String middleName
    String telephone
    String email

    static constraints = {
        importFrom ConstraintsPerson, include:[".*Name"]
    }
}

class PersonAllConstraintsNoNormalConstraintsIncludingExcludingByRegexpCommand implements Validateable {
    String firstName
    String lastName
    String middleName
    String telephone
    String email

    static constraints = {
        importFrom ConstraintsPerson, include:[".*Name"], exclude:["m.*Name"]
    }
}
