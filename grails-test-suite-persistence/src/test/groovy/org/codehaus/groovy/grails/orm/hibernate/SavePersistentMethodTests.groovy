package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import grails.validation.ValidationException

import org.codehaus.groovy.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor

class SavePersistentMethodTests extends AbstractGrailsHibernateTests {

    void testFlush() {
        def bookClass = ga.getDomainClass(SavePersistentMethodBook.name)
        def authorClass = ga.getDomainClass(SavePersistentMethodAuthor.name)
        def addressClass = ga.getDomainClass(SavePersistentMethodAddress.name)

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
        def bookClass = ga.getDomainClass(SavePersistentMethodBook.name)
        def authorClass = ga.getDomainClass(SavePersistentMethodAuthor.name)
        def addressClass = ga.getDomainClass(SavePersistentMethodAddress.name)

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
        def bookClass = ga.getDomainClass(SavePersistentMethodBook.name)
        def authorClass = ga.getDomainClass(SavePersistentMethodAuthor.name)
        def addressClass = ga.getDomainClass(SavePersistentMethodAddress.name)

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
        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name)
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
        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name)
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
- Field error in object 'org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam' on field 'homePage': rejected value [null]; codes [typeMismatch.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.homePage,typeMismatch.homePage,typeMismatch.java.net.URL,typeMismatch]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.homePage,homePage]; arguments []; default message [homePage]]; default message [Failed to convert property value of type 'java.lang.String' to required type 'java.net.URL' for property 'homePage'; nested exception is java.lang.IllegalArgumentException: Could not retrieve URL for class path resource [invalidurl]: class path resource [invalidurl] cannot be resolved to URL because it does not exist]
- Field error in object 'org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam' on field 'name': rejected value [null]; codes [org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.java.lang.String,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error,savePersistentMethodTeam.name.nullable.error.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,savePersistentMethodTeam.name.nullable.error.name,savePersistentMethodTeam.name.nullable.error.java.lang.String,savePersistentMethodTeam.name.nullable.error,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.java.lang.String,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable,savePersistentMethodTeam.name.nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,savePersistentMethodTeam.name.nullable.name,savePersistentMethodTeam.name.nullable.java.lang.String,savePersistentMethodTeam.name.nullable,nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,nullable.name,nullable.java.lang.String,nullable]; arguments [name,class org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam]; default message [Property [{0}] of class [{1}] cannot be null]
''', msg
    }

    void testFailOnErrorFalseWithValidationErrors() {
        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name)
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        assertNull 'save should have returned null', team.save(failOnError: false)
    }

    void testFailOnErrorConfigTrueWithValidationErrors() {
        ga.config.grails.gorm.failOnError = true
		ga.configChanged()

        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name)
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        def msg = shouldFail(ValidationException) {
            team.save()
        }
        assertEquals '''\
Validation Error(s) occurred during save():
- Field error in object 'org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam' on field 'homePage': rejected value [null]; codes [typeMismatch.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.homePage,typeMismatch.homePage,typeMismatch.java.net.URL,typeMismatch]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.homePage,homePage]; arguments []; default message [homePage]]; default message [Failed to convert property value of type 'java.lang.String' to required type 'java.net.URL' for property 'homePage'; nested exception is java.lang.IllegalArgumentException: Could not retrieve URL for class path resource [invalidurl]: class path resource [invalidurl] cannot be resolved to URL because it does not exist]
- Field error in object 'org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam' on field 'name': rejected value [null]; codes [org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.java.lang.String,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error,savePersistentMethodTeam.name.nullable.error.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,savePersistentMethodTeam.name.nullable.error.name,savePersistentMethodTeam.name.nullable.error.java.lang.String,savePersistentMethodTeam.name.nullable.error,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.java.lang.String,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable,savePersistentMethodTeam.name.nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,savePersistentMethodTeam.name.nullable.name,savePersistentMethodTeam.name.nullable.java.lang.String,savePersistentMethodTeam.name.nullable,nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,nullable.name,nullable.java.lang.String,nullable]; arguments [name,class org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam]; default message [Property [{0}] of class [{1}] cannot be null]
''', msg
    }

    void testFailOnErrorConfigTrueWithValidationErrorsAndAutoFlush() {
        def interceptor = appCtx.getBean("eventTriggeringInterceptor")
        interceptor.failOnError=true
        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name).clazz
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
        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name).clazz
        def team = teamClass.newInstance(name:"Manchester United", homePage:new URL("http://www.manutd.com/"))
        assertNotNull "should have saved", team.save(flush:true)

        session.clear()

        team = teamClass.get(1)
        team.name = ''
        // should not throw exception for the org.codehaus.groovy.grails.orm.hibernate package
        session.flush()

        interceptor.failOnErrorPackages = ['org.codehaus.groovy.grails.orm.hibernate']
        interceptor.eventListeners.clear()

        session.clear()
        team = teamClass.get(1)
        team.name = ''
        // should not throw exception for the org.codehaus.groovy.grails.orm.hibernate package
        shouldFail(ValidationException) {
            session.flush()
        }
    }

    void testFailOnErrorConfigIncludesMatchingPackageWithValidationErrors() {
        ga.config.grails.gorm.failOnError = ['com.foo', 'org.codehaus.groovy.grails.orm.hibernate', 'com.bar']
		ga.configChanged()
        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name)
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        def msg = shouldFail(ValidationException) {
            team.save()
        }
        assertEquals '''\
Validation Error(s) occurred during save():
- Field error in object 'org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam' on field 'homePage': rejected value [null]; codes [typeMismatch.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.homePage,typeMismatch.homePage,typeMismatch.java.net.URL,typeMismatch]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.homePage,homePage]; arguments []; default message [homePage]]; default message [Failed to convert property value of type 'java.lang.String' to required type 'java.net.URL' for property 'homePage'; nested exception is java.lang.IllegalArgumentException: Could not retrieve URL for class path resource [invalidurl]: class path resource [invalidurl] cannot be resolved to URL because it does not exist]
- Field error in object 'org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam' on field 'name': rejected value [null]; codes [org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.java.lang.String,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error,savePersistentMethodTeam.name.nullable.error.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,savePersistentMethodTeam.name.nullable.error.name,savePersistentMethodTeam.name.nullable.error.java.lang.String,savePersistentMethodTeam.name.nullable.error,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.java.lang.String,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable,savePersistentMethodTeam.name.nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,savePersistentMethodTeam.name.nullable.name,savePersistentMethodTeam.name.nullable.java.lang.String,savePersistentMethodTeam.name.nullable,nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,nullable.name,nullable.java.lang.String,nullable]; arguments [name,class org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam]; default message [Property [{0}] of class [{1}] cannot be null]
''', msg
    }

    void testFailOnErrorConfigDoesNotIncludeMatchingPackageWithValidationErrors() {
        ga.config.grails.gorm.failOnError = ['com.foo', 'com.bar']
		ga.configChanged()
        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name)
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        assertNull 'save should have returned null', team.save()
    }

    void testFailOnErrorConfigFalseWithValidationErrors() {
        ga.config.grails.gorm.failOnError = false
		ga.configChanged()

        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name)
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        assertNull 'save should have returned null', team.save()
    }

    void testFailOnErrorConfigTrueArgumentFalseWithValidationErrors() {
        ga.config.grails.gorm.failOnError = true
		ga.configChanged()
        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name)
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        assertNull 'save should have returned null', team.save(failOnError: false)
    }

    void testFailOnErrorConfigFalseArgumentTrueWithValidationErrors() {
        ga.config.grails.gorm.failOnError = true
		ga.configChanged()
        def teamClass = ga.getDomainClass(SavePersistentMethodTeam.name)
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        def msg = shouldFail(ValidationException) {
            team.save(failOnError: true)
        }
        assertEquals '''\
Validation Error(s) occurred during save():
- Field error in object 'org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam' on field 'homePage': rejected value [null]; codes [typeMismatch.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.homePage,typeMismatch.homePage,typeMismatch.java.net.URL,typeMismatch]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.homePage,homePage]; arguments []; default message [homePage]]; default message [Failed to convert property value of type 'java.lang.String' to required type 'java.net.URL' for property 'homePage'; nested exception is java.lang.IllegalArgumentException: Could not retrieve URL for class path resource [invalidurl]: class path resource [invalidurl] cannot be resolved to URL because it does not exist]
- Field error in object 'org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam' on field 'name': rejected value [null]; codes [org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error.java.lang.String,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.error,savePersistentMethodTeam.name.nullable.error.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,savePersistentMethodTeam.name.nullable.error.name,savePersistentMethodTeam.name.nullable.error.java.lang.String,savePersistentMethodTeam.name.nullable.error,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.name,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable.java.lang.String,org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name.nullable,savePersistentMethodTeam.name.nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,savePersistentMethodTeam.name.nullable.name,savePersistentMethodTeam.name.nullable.java.lang.String,savePersistentMethodTeam.name.nullable,nullable.org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam.name,nullable.name,nullable.java.lang.String,nullable]; arguments [name,class org.codehaus.groovy.grails.orm.hibernate.SavePersistentMethodTeam]; default message [Property [{0}] of class [{1}] cannot be null]
''', msg
    }

    void testSaveWithoutValidation() {
        def dcClass = ga.getDomainClass(SavePersistentMethodCustomValidation.name)
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

    protected getDomainClasses() {
        [SavePersistentMethodTeam, SavePersistentMethodBook, SavePersistentMethodAuthor, SavePersistentMethodAddress, SavePersistentMethodCustomValidation]
    }
}


@Entity
class SavePersistentMethodTeam {
    String name
    URL homePage

    static constraints = {
        name blank:false
    }
}

@Entity
class SavePersistentMethodBook {
    String title
    SavePersistentMethodAuthor author
    static belongsTo = SavePersistentMethodAuthor
    static constraints = {
       title(blank:false, size:1..255)
       author(nullable:true)
    }
}

@Entity
class SavePersistentMethodAuthor {
   String name
   SavePersistentMethodAddress address
   static hasMany = [books:SavePersistentMethodBook]
   static constraints = {
        address(nullable:true)
        name(size:1..255, blank:false)
   }
}

@Entity
class SavePersistentMethodAddress {
    SavePersistentMethodAuthor author
    String location
    static belongsTo = SavePersistentMethodAuthor
    static constraints = {
       author(nullable:true)
       location(blank:false)
    }
}

@Entity
class SavePersistentMethodCustomValidation {
    String title

    static constraints = {
        title(validator: { val, obj -> throw new IllegalStateException() })
    }
}

