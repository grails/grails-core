package org.codehaus.groovy.grails.validation

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils

/**
 * Tests constraints building specific for command objects
 */
class ConstrainedPropertyBuiderForCommandsTests extends AbstractGrailsControllerTests {
    protected void onSetUp() {
        parseDomainTestClasses()
    }

    private void parseDomainTestClasses() {
        gcl.parseClass('''
            class Person {
                Long id
                Long version
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
            }''')
    }

    private void parseCommandTestClasses() {
        gcl.parseClass('''
            class PersonAllConstraintsNoNormalConstraintsCommand {
                String firstName
                String lastName
                String middleName
                String telephone
                String email

                static constraints = {
                    importFrom Person
                }
            }''')

        gcl.parseClass('''
            class PersonSomeConstraintsNoNormalConstraintsCommand {
                String firstName
                String lastName

                static constraints = {
                    importFrom Person
                }
            }''')

        gcl.parseClass('''
            class PersonAllConstraintsWithNormalConstraintsFirstCommand {
                String firstName
                String lastName
                String middleName
                String telephone
                String email

                static constraints = {
                    firstName(nullable:true, blank:true, maxSize:10)
                    lastName(nullable:true, blank:true, maxSize:20)
                    email(nullable:false, blank:true, email:true)

                    importFrom Person
                }
            }''')

        gcl.parseClass('''
            class PersonAllConstraintsWithNormalConstraintsLastCommand {
                String firstName
                String lastName
                String middleName
                String telephone
                String email

                static constraints = {
                    importFrom Person

                    firstName(nullable:true, blank:true, maxSize:10)
                    lastName(nullable:true, blank:true, maxSize:20)
                    email(nullable:false, blank:true, email:true)
                }
            }''')

        gcl.parseClass('''
            class PersonAllConstraintsNoNormalConstraintsIncludingCommand {
                String firstName
                String lastName
                String middleName
                String telephone
                String email

                static constraints = {
                    importFrom Person, include:["firstName", "lastName"]
                }
            }''')

        gcl.parseClass('''
            class PersonAllConstraintsNoNormalConstraintsExcludingCommand {
                String firstName
                String lastName
                String middleName
                String telephone
                String email

                static constraints = {
                    importFrom Person, exclude:["firstName", "lastName"]
                }
            }''')

        gcl.parseClass('''
            class PersonAllConstraintsNoNormalConstraintsIncludingByRegexpCommand {
                String firstName
                String lastName
                String middleName
                String telephone
                String email

                static constraints = {
                    importFrom Person, include:[".*Name"]
                }
            }''')

        gcl.parseClass('''
            class PersonAllConstraintsNoNormalConstraintsIncludingExcludingByRegexpCommand {
                String firstName
                String lastName
                String middleName
                String telephone
                String email

                static constraints = {
                    importFrom Person, include:[".*Name"], exclude:["m.*Name"]
                }
            }''')
    }

    public void testImportFrom_AllConstraints_ConstraintsExist() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonAllConstraintsNoNormalConstraintsCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        assertNotNull(personCommand.getConstraints())
        assertEquals(5, personCommand.getConstraints().size())
        assertNull(personCommand.getConstraints().get("importFrom"))
        assertNotNull(personCommand.getConstraints().get("email"))

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()
        assertNotNull(person.getConstraints())
        assertEquals(5, person.getConstraints().size())
        assertNull(person.getConstraints().get("importFrom"))
        assertNotNull(person.getConstraints().get("email"))
    }

    public void testImportFrom_AllConstraints_Validation() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonAllConstraintsNoNormalConstraintsCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        personCommand.firstName = "firstName"
        personCommand.lastName = "lastName"
        personCommand.validate()

        assertFalse(personCommand.hasErrors())

        personCommand.clearErrors()
        personCommand.firstName = null
        personCommand.validate()

        assertTrue(personCommand.hasErrors())
        assertEquals(1, personCommand.getErrors().getErrorCount())
        assertEquals(1, personCommand.getErrors().getFieldErrors("firstName").size())
        assertNull(personCommand.getErrors().getFieldErrors("firstName")[0].getRejectedValue())

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()

        person.firstName = "firstName"
        person.lastName = "lastName"
        person.validate()

        assertFalse(person.hasErrors())

        person.clearErrors()
        person.firstName = null
        person.validate()

        assertTrue(person.hasErrors())
        assertEquals(1, person.getErrors().getErrorCount())
        assertEquals(1, person.getErrors().getFieldErrors("firstName").size())
        assertNull(person.getErrors().getFieldErrors("firstName")[0].getRejectedValue())
    }

    public void testImportFrom_SomeConstraints_ConstraintsExist() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonSomeConstraintsNoNormalConstraintsCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        assertNotNull(personCommand.getConstraints())
        assertEquals(2, personCommand.getConstraints().size())
        assertNull(personCommand.getConstraints().get("importFrom"))
        assertNotNull(personCommand.getConstraints().get("firstName"))

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()
        assertNotNull(person.getConstraints())
        assertEquals(5, person.getConstraints().size())
        assertNull(person.getConstraints().get("importFrom"))
        assertNotNull(person.getConstraints().get("firstName"))
        assertNotNull(person.getConstraints().get("email"))
    }

    public void testImportFrom_SomeConstraints_Validation() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonSomeConstraintsNoNormalConstraintsCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        personCommand.firstName = "firstName"
        personCommand.lastName = "lastName"
        personCommand.validate()

        assertFalse(personCommand.hasErrors())

        personCommand.clearErrors()
        personCommand.firstName = null
        personCommand.validate()

        assertTrue(personCommand.hasErrors())
        assertEquals(1, personCommand.getErrors().getErrorCount())
        assertEquals(1, personCommand.getErrors().getFieldErrors("firstName").size())
        assertNull(personCommand.getErrors().getFieldErrors("firstName")[0].getRejectedValue())

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()

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

    public void testImportFrom_AllConstraints_ConstraintsExist_NormalConstraintsFirst() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonAllConstraintsWithNormalConstraintsFirstCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        assertNotNull(personCommand.getConstraints())
        assertEquals(5, personCommand.getConstraints().size())
        assertNull(personCommand.getConstraints().get("importFrom"))
        assertNotNull(personCommand.getConstraints().get("telephone"))

        assertEquals(30, personCommand.getConstraints().get("firstName").getAppliedConstraint("maxSize").getParameter())
        assertEquals(50, personCommand.getConstraints().get("lastName").getAppliedConstraint("maxSize").getParameter())
        assertEquals(
                "123123",
                personCommand.getConstraints().get("telephone").getAppliedConstraint("matches").getParameter())

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()
        assertNotNull(person.getConstraints())
        assertEquals(5, person.getConstraints().size())
        assertNull(person.getConstraints().get("importFrom"))
        assertNotNull(person.getConstraints().get("telephone"))

        assertEquals(30, person.getConstraints().get("firstName").getAppliedConstraint("maxSize").getParameter())
        assertEquals(50, person.getConstraints().get("lastName").getAppliedConstraint("maxSize").getParameter())
        assertEquals("123123", person.getConstraints().get("telephone").getAppliedConstraint("matches").getParameter())        
    }

    public void testImportFrom_AllConstraints_Validation_NormalConstraintsFirst() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonAllConstraintsWithNormalConstraintsFirstCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        personCommand.firstName = "firstName"
        personCommand.lastName = "lastName"
        personCommand.validate()

        assertFalse(personCommand.hasErrors())

        personCommand.clearErrors()
        personCommand.firstName = null
        personCommand.lastName = null
        personCommand.validate()

        assertTrue(personCommand.hasErrors())
        assertEquals(2, personCommand.getErrors().getErrorCount())

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()

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
        assertEquals(2, person.getErrors().getErrorCount())
        assertEquals(1, person.getErrors().getFieldErrors("firstName").size())
        assertNull(person.getErrors().getFieldErrors("firstName")[0].getRejectedValue())        
        assertEquals(1, person.getErrors().getFieldErrors("email").size())
        assertEquals("wrongEmail", person.getErrors().getFieldErrors("email")[0].getRejectedValue())
    }

    public void testImportFrom_AllConstraints_ConstraintsExist_NormalConstraintsLast() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonAllConstraintsWithNormalConstraintsLastCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        assertNotNull(personCommand.getConstraints())
        assertEquals(5, personCommand.getConstraints().size())
        assertNull(personCommand.getConstraints().get("importFrom"))
        assertNotNull(personCommand.getConstraints().get("telephone"))

        assertEquals(10, personCommand.getConstraints().get("firstName").getAppliedConstraint("maxSize").getParameter())
        assertEquals(20, personCommand.getConstraints().get("lastName").getAppliedConstraint("maxSize").getParameter())
        assertEquals(
                "123123",
                personCommand.getConstraints().get("telephone").getAppliedConstraint("matches").getParameter())

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()
        assertNotNull(person.getConstraints())
        assertEquals(5, person.getConstraints().size())
        assertNull(person.getConstraints().get("importFrom"))
        assertNotNull(person.getConstraints().get("telephone"))

        assertEquals(30, person.getConstraints().get("firstName").getAppliedConstraint("maxSize").getParameter())
        assertEquals(50, person.getConstraints().get("lastName").getAppliedConstraint("maxSize").getParameter())
        assertEquals("123123", person.getConstraints().get("telephone").getAppliedConstraint("matches").getParameter())
    }

    public void testImportFrom_AllConstraints_Validation_NormalConstraintsLast() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonAllConstraintsWithNormalConstraintsLastCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        personCommand.firstName = null
        personCommand.lastName = null
        personCommand.email = "someemail@some.net"
        personCommand.validate()
        
        assertFalse(personCommand.hasErrors())

        personCommand.clearErrors()
        personCommand.firstName = null
        personCommand.lastName = null
        personCommand.email = "wrongEmail"
        personCommand.validate()

        assertTrue(personCommand.hasErrors())
        assertEquals(1, personCommand.getErrors().getErrorCount())

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()

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
        assertEquals(2, person.getErrors().getErrorCount())
        assertEquals(1, person.getErrors().getFieldErrors("firstName").size())
        assertNull(person.getErrors().getFieldErrors("firstName")[0].getRejectedValue())
        assertEquals(1, person.getErrors().getFieldErrors("email").size())
        assertEquals("wrongEmail", person.getErrors().getFieldErrors("email")[0].getRejectedValue())
    }

    public void testImportFrom_AllConstraints_ConstraintsExist_Including() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonAllConstraintsNoNormalConstraintsIncludingCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        assertNotNull(personCommand.getConstraints())
        assertEquals(2, personCommand.getConstraints().size())
        assertNull(personCommand.getConstraints().get("importFrom"))
        assertNotNull(personCommand.getConstraints().get("firstName"))
        assertNull(personCommand.getConstraints().get("email"))

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()
        assertNotNull(person.getConstraints())
        assertEquals(5, person.getConstraints().size())
        assertNull(person.getConstraints().get("importFrom"))
        assertNotNull(person.getConstraints().get("firstName"))
        assertNotNull(person.getConstraints().get("email"))
    }

    public void testImportFrom_AllConstraints_ConstraintsExist_Excluding() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonAllConstraintsNoNormalConstraintsExcludingCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        assertNotNull(personCommand.getConstraints())
        assertEquals(3, personCommand.getConstraints().size())
        assertNull(personCommand.getConstraints().get("importFrom"))
        assertNull(personCommand.getConstraints().get("firstName"))
        assertNull(personCommand.getConstraints().get("lastName"))
        assertNotNull(personCommand.getConstraints().get("email"))

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()
        assertNotNull(person.getConstraints())
        assertEquals(5, person.getConstraints().size())
        assertNull(person.getConstraints().get("importFrom"))
        assertNotNull(person.getConstraints().get("firstName"))
        assertNotNull(person.getConstraints().get("email"))
    }

    public void testImportFrom_AllConstraints_ConstraintsExist_IncludingByRegexp() {
        parseCommandTestClasses()
        def personCommandClazz = gcl.loadClass("PersonAllConstraintsNoNormalConstraintsIncludingByRegexpCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        assertNotNull(personCommand.getConstraints())
        assertEquals(3, personCommand.getConstraints().size())
        assertNull(personCommand.getConstraints().get("importFrom"))
        assertNotNull(personCommand.getConstraints().get("firstName"))
        assertNotNull(personCommand.getConstraints().get("lastName"))
        assertNotNull(personCommand.getConstraints().get("middleName"))
        assertNull(personCommand.getConstraints().get("email"))

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()
        assertNotNull(person.getConstraints())
        assertEquals(5, person.getConstraints().size())
        assertNull(person.getConstraints().get("importFrom"))
        assertNotNull(person.getConstraints().get("firstName"))
        assertNotNull(person.getConstraints().get("email"))
    }

    public void testImportFrom_AllConstraints_ConstraintsExist_IncludingExcludingByRegexp() {
        parseCommandTestClasses()
        def personCommandClazz =
            gcl.loadClass("PersonAllConstraintsNoNormalConstraintsIncludingExcludingByRegexpCommand")

        WebMetaUtils.enhanceCommandObject(appCtx, personCommandClazz)
        def personCommand = personCommandClazz.newInstance()

        assertNotNull(personCommand.getConstraints())
        assertEquals(2, personCommand.getConstraints().size())
        assertNull(personCommand.getConstraints().get("importFrom"))
        assertNotNull(personCommand.getConstraints().get("firstName"))
        assertNotNull(personCommand.getConstraints().get("lastName"))
        assertNull(personCommand.getConstraints().get("middleName"))
        assertNull(personCommand.getConstraints().get("email"))

        // Now check that everything is ok with domain class
        def person = gcl.loadClass("Person").newInstance()
        assertNotNull(person.getConstraints())
        assertEquals(5, person.getConstraints().size())
        assertNull(person.getConstraints().get("importFrom"))
        assertNotNull(person.getConstraints().get("firstName"))
        assertNotNull(person.getConstraints().get("email"))
    }
}
