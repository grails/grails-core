package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.*
import grails.validation.ValidationException
import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor

class SavePersistentMethodTests extends AbstractGrailsHibernateTests {

    void testFlush() {
        def bookClass = ga.getDomainClass("grails.tests.SaveBook")
        def authorClass = ga.getDomainClass("grails.tests.SaveAuthor")
        def addressClass = ga.getDomainClass("grails.tests.SaveAddress")

        def book = bookClass.newInstance()
        book.title = "Foo"
        def author = authorClass.newInstance()
        book.author = author
        author.name = "Bar"
        def address = addressClass.newInstance()
        author.address = address
        address.location = "Foo Bar"
        assertNotNull author.save()

        assertNotNull book.save(flush:true)
        assertNotNull book.id
    }

    void testToOneCascadingValidation() {
        def bookClass = ga.getDomainClass("grails.tests.SaveBook")
        def authorClass = ga.getDomainClass("grails.tests.SaveAuthor")
        def addressClass = ga.getDomainClass("grails.tests.SaveAddress")

        def book = bookClass.newInstance()

        assertNull book.save()
        assertNull book.save(deepValidate:false)

        book.title = "Foo"

        assertNotNull book.save()

        def author = authorClass.newInstance()
        author.name = "Bar"
        author.save()

        book.author = author

        // will validate book is owned by author
        assertNotNull book.save()
        assertNotNull book.save(deepValidate:false)

        def address = addressClass.newInstance()

        author.address = address

        assertNull author.save()

        address.location = "Foo Bar"
        assertNotNull author.save()
        assertNotNull author.save(deepValidate:false)
    }

    void testToManyCascadingValidation() {
        def bookClass = ga.getDomainClass("grails.tests.SaveBook")
        def authorClass = ga.getDomainClass("grails.tests.SaveAuthor")
        def addressClass = ga.getDomainClass("grails.tests.SaveAddress")

        def author = authorClass.newInstance()

        assertNull author.save()
        author.name = "Foo"

        assertNotNull author.save()

        def address = addressClass.newInstance()
        author.address = address

        assertNull author.save()

        address.location = "Foo Bar"
        assertNotNull author.save()

        def book = bookClass.newInstance()

        author.addToBooks(book)
        assertNull author.save()

        book.title = "TDGTG"
        assertNotNull author.save()
        assertNotNull author.save(deepValidate:false)
    }

    void testValidationAfterBindingErrors() {
        def teamClass = ga.getDomainClass('grails.tests.Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        assertNull 'validation should have failed', team.save()
        assertEquals 'wrong number of errors found', 2, team.errors.errorCount
        assertEquals 'wrong number of homePage errors found', 1, team.errors.getFieldErrors('homePage')?.size()
        def homePageError = team.errors.getFieldError('homePage')
        assertTrue 'did not find typeMismatch error', 'typeMismatch' in homePageError.codes

        team.homePage = new URL('http://grails.org')
        assertNull 'validation should have failed', team.save()
        assertEquals 'wrong number of errors found', 1, team.errors.errorCount
        assertEquals 'wrong number of homePage errors found', 0, team.errors.getFieldErrors('homePage')?.size()
    }

    void testFailOnErrorTrueWithValidationErrors() {
        def teamClass = ga.getDomainClass('grails.tests.Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        def msg = shouldFail(ValidationException) {
            team.save(failOnError: true)
        }

        // test errors object
        try {
            team.save(failOnError:true)
        }
        catch (ValidationException e) {
            assertNotNull "should have a reference to the errors object", e.errors
        }
        assertEquals '''\
Validation Error(s) occurred during save():
- Field error in object 'grails.tests.Team' on field 'homePage': rejected value [null]; codes [typeMismatch.grails.tests.Team.homePage,typeMismatch.homePage,typeMismatch.java.net.URL,typeMismatch]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [grails.tests.Team.homePage,homePage]; arguments []; default message [homePage]]; default message [Failed to convert property value of type 'java.lang.String' to required type 'java.net.URL' for property 'homePage'; nested exception is java.lang.IllegalArgumentException: Could not retrieve URL for class path resource [invalidurl]: class path resource [invalidurl] cannot be resolved to URL because it does not exist]
- Field error in object 'grails.tests.Team' on field 'name': rejected value [null]; codes [grails.tests.Team.name.nullable.error.grails.tests.Team.name,grails.tests.Team.name.nullable.error.name,grails.tests.Team.name.nullable.error.java.lang.String,grails.tests.Team.name.nullable.error,team.name.nullable.error.grails.tests.Team.name,team.name.nullable.error.name,team.name.nullable.error.java.lang.String,team.name.nullable.error,grails.tests.Team.name.nullable.grails.tests.Team.name,grails.tests.Team.name.nullable.name,grails.tests.Team.name.nullable.java.lang.String,grails.tests.Team.name.nullable,team.name.nullable.grails.tests.Team.name,team.name.nullable.name,team.name.nullable.java.lang.String,team.name.nullable,nullable.grails.tests.Team.name,nullable.name,nullable.java.lang.String,nullable]; arguments [name,class grails.tests.Team]; default message [Property [{0}] of class [{1}] cannot be null]
''', msg
    }

    void testFailOnErrorFalseWithValidationErrors() {
        def teamClass = ga.getDomainClass('grails.tests.Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        assertNull 'save should have returned null', team.save(failOnError: false)
    }

    void testFailOnErrorConfigTrueWithValidationErrors() {
        def config = new ConfigSlurper().parse("grails.gorm.failOnError = true")

        ConfigurationHolder.config = config
        def teamClass = ga.getDomainClass('grails.tests.Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        def msg = shouldFail(ValidationException) {
            team.save()
        }
        assertEquals '''\
Validation Error(s) occurred during save():
- Field error in object 'grails.tests.Team' on field 'homePage': rejected value [null]; codes [typeMismatch.grails.tests.Team.homePage,typeMismatch.homePage,typeMismatch.java.net.URL,typeMismatch]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [grails.tests.Team.homePage,homePage]; arguments []; default message [homePage]]; default message [Failed to convert property value of type 'java.lang.String' to required type 'java.net.URL' for property 'homePage'; nested exception is java.lang.IllegalArgumentException: Could not retrieve URL for class path resource [invalidurl]: class path resource [invalidurl] cannot be resolved to URL because it does not exist]
- Field error in object 'grails.tests.Team' on field 'name': rejected value [null]; codes [grails.tests.Team.name.nullable.error.grails.tests.Team.name,grails.tests.Team.name.nullable.error.name,grails.tests.Team.name.nullable.error.java.lang.String,grails.tests.Team.name.nullable.error,team.name.nullable.error.grails.tests.Team.name,team.name.nullable.error.name,team.name.nullable.error.java.lang.String,team.name.nullable.error,grails.tests.Team.name.nullable.grails.tests.Team.name,grails.tests.Team.name.nullable.name,grails.tests.Team.name.nullable.java.lang.String,grails.tests.Team.name.nullable,team.name.nullable.grails.tests.Team.name,team.name.nullable.name,team.name.nullable.java.lang.String,team.name.nullable,nullable.grails.tests.Team.name,nullable.name,nullable.java.lang.String,nullable]; arguments [name,class grails.tests.Team]; default message [Property [{0}] of class [{1}] cannot be null]
''', msg
    }

    void testFailOnErrorConfigTrueWithValidationErrorsAndAutoFlush() {
        def interceptor = appCtx.getBean("eventTriggeringInterceptor")
        interceptor.failOnError=true
        def teamClass = ga.getDomainClass('grails.tests.Team').clazz
        def team = teamClass.newInstance(name:"Manchester United", homePage:new URL("http://www.manutd.com/"))
        assertNotNull "should have saved", team.save(flush:true)

        session.clear()

        team = teamClass.get(1)
        team.name = ''
        shouldFail(ValidationException) {
            session.flush()
        }
    }

    void testFailOnErrorConfigWithPackagesAndAutoFlush() {
        ClosureEventTriggeringInterceptor interceptor = appCtx.getBean("eventTriggeringInterceptor")
        interceptor.failOnError=true
        interceptor.failOnErrorPackages = ['foo.bar']
        def teamClass = ga.getDomainClass('grails.tests.Team').clazz
        def team = teamClass.newInstance(name:"Manchester United", homePage:new URL("http://www.manutd.com/"))
        assertNotNull "should have saved", team.save(flush:true)

        session.clear()

        team = teamClass.get(1)
        team.name = ''
        // should not throw exception for the grails.test package
        session.flush()

        interceptor.failOnErrorPackages = ['grails.tests']
		interceptor.eventListeners.clear()

        session.clear()
        team = teamClass.get(1)
        team.name = ''
        // should not throw exception for the grails.test package
        shouldFail(ValidationException) {
            session.flush()
        }
    }

    void testFailOnErrorConfigIncludesMatchingPackageWithValidationErrors() {
        def config = new ConfigSlurper().parse("grails.gorm.failOnError = ['com.foo', 'grails.tests', 'com.bar']")

        ConfigurationHolder.config = config
        def teamClass = ga.getDomainClass('grails.tests.Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        def msg = shouldFail(ValidationException) {
            team.save()
        }
        assertEquals '''\
Validation Error(s) occurred during save():
- Field error in object 'grails.tests.Team' on field 'homePage': rejected value [null]; codes [typeMismatch.grails.tests.Team.homePage,typeMismatch.homePage,typeMismatch.java.net.URL,typeMismatch]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [grails.tests.Team.homePage,homePage]; arguments []; default message [homePage]]; default message [Failed to convert property value of type 'java.lang.String' to required type 'java.net.URL' for property 'homePage'; nested exception is java.lang.IllegalArgumentException: Could not retrieve URL for class path resource [invalidurl]: class path resource [invalidurl] cannot be resolved to URL because it does not exist]
- Field error in object 'grails.tests.Team' on field 'name': rejected value [null]; codes [grails.tests.Team.name.nullable.error.grails.tests.Team.name,grails.tests.Team.name.nullable.error.name,grails.tests.Team.name.nullable.error.java.lang.String,grails.tests.Team.name.nullable.error,team.name.nullable.error.grails.tests.Team.name,team.name.nullable.error.name,team.name.nullable.error.java.lang.String,team.name.nullable.error,grails.tests.Team.name.nullable.grails.tests.Team.name,grails.tests.Team.name.nullable.name,grails.tests.Team.name.nullable.java.lang.String,grails.tests.Team.name.nullable,team.name.nullable.grails.tests.Team.name,team.name.nullable.name,team.name.nullable.java.lang.String,team.name.nullable,nullable.grails.tests.Team.name,nullable.name,nullable.java.lang.String,nullable]; arguments [name,class grails.tests.Team]; default message [Property [{0}] of class [{1}] cannot be null]
''', msg
    }

    void testFailOnErrorConfigDoesNotIncludeMatchingPackageWithValidationErrors() {
        def config = new ConfigSlurper().parse("grails.gorm.failOnError = ['com.foo', 'com.bar']")

        ConfigurationHolder.config = config
        def teamClass = ga.getDomainClass('grails.tests.Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        assertNull 'save should have returned null', team.save()
    }

    void testFailOnErrorConfigFalseWithValidationErrors() {
        def config = new ConfigSlurper().parse("grails.gorm.failOnError = false")

        ConfigurationHolder.config = config
        def teamClass = ga.getDomainClass('grails.tests.Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        assertNull 'save should have returned null', team.save()
    }

    void testFailOnErrorConfigTrueArgumentFalseWithValidationErrors() {
        def config = new ConfigSlurper().parse("grails.gorm.failOnError = true")

        ConfigurationHolder.config = config
        def teamClass = ga.getDomainClass('grails.tests.Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        assertNull 'save should have returned null', team.save(failOnError: false)
    }

    void testFailOnErrorConfigFalseArgumentTrueWithValidationErrors() {
        def config = new ConfigSlurper().parse("grails.gorm.failOnError = false")

        ConfigurationHolder.config = config
        def teamClass = ga.getDomainClass('grails.tests.Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        def msg = shouldFail(ValidationException) {
            team.save(failOnError: true)
        }
        assertEquals '''\
Validation Error(s) occurred during save():
- Field error in object 'grails.tests.Team' on field 'homePage': rejected value [null]; codes [typeMismatch.grails.tests.Team.homePage,typeMismatch.homePage,typeMismatch.java.net.URL,typeMismatch]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [grails.tests.Team.homePage,homePage]; arguments []; default message [homePage]]; default message [Failed to convert property value of type 'java.lang.String' to required type 'java.net.URL' for property 'homePage'; nested exception is java.lang.IllegalArgumentException: Could not retrieve URL for class path resource [invalidurl]: class path resource [invalidurl] cannot be resolved to URL because it does not exist]
- Field error in object 'grails.tests.Team' on field 'name': rejected value [null]; codes [grails.tests.Team.name.nullable.error.grails.tests.Team.name,grails.tests.Team.name.nullable.error.name,grails.tests.Team.name.nullable.error.java.lang.String,grails.tests.Team.name.nullable.error,team.name.nullable.error.grails.tests.Team.name,team.name.nullable.error.name,team.name.nullable.error.java.lang.String,team.name.nullable.error,grails.tests.Team.name.nullable.grails.tests.Team.name,grails.tests.Team.name.nullable.name,grails.tests.Team.name.nullable.java.lang.String,grails.tests.Team.name.nullable,team.name.nullable.grails.tests.Team.name,team.name.nullable.name,team.name.nullable.java.lang.String,team.name.nullable,nullable.grails.tests.Team.name,nullable.name,nullable.java.lang.String,nullable]; arguments [name,class grails.tests.Team]; default message [Property [{0}] of class [{1}] cannot be null]
''', msg
    }

    void testSaveWithoutValidation() {
        def dcClass = ga.getDomainClass('grails.tests.SaveCustomValidation')
        def dc = dcClass.newInstance()
        dc.properties = [ title: 'Test' ]

        // The custom validator for SaveCustomValidation throws an
        // exception if it's triggered, but that shouldn't happen
        // if we explicitly disable validation.
        dc.save(validate: false)
        session.flush()

        // Once its attached to the session, dirty checking applies.
        // Here we test that the validation doesn't occur even though
        // the domain instance has been modified.
        dc.title = "A different title"
        dc.save(validate: false)
        session.flush()

        // Let's check that the validation kicks in if we don't disable it...
        dc.title = "Another title"
        shouldFail(IllegalStateException) {
            dc.save()
        }

        // ...and make sure that this also happens with dirty-checking.
        dc.title = "Dirty check"
        shouldFail(IllegalStateException) {
            session.flush()
        }
    }

    void onSetUp() {
        gcl.parseClass '''
package grails.tests

import grails.persistence.*

@Entity
class Team {
    String name
    URL homePage

    static constraints = {
        name blank:false
    }
}

@Entity
class SaveBook {
    String title
    SaveAuthor author
    static belongsTo = SaveAuthor
    static constraints = {
       title(blank:false, size:1..255)
       author(nullable:true)
    }
}

@Entity
class SaveAuthor {
   String name
   SaveAddress address
   static hasMany = [books:SaveBook]
   static constraints = {
        address(nullable:true)
        name(size:1..255, blank:false)
   }
}

@Entity
class SaveAddress {
    SaveAuthor author
    String location
    static belongsTo = SaveAuthor
    static constraints = {
       author(nullable:true)
       location(blank:false)
    }
}

@Entity
class SaveCustomValidation {
    String title

    static constraints = {
        title(validator: { val, obj -> throw new IllegalStateException() })
    }
}
'''
    }

    void onTearDown() {
        ConfigurationHolder.config = null
    }
}
