package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.validation.exceptions.ValidationException

class SavePersistentMethodTests extends AbstractGrailsHibernateTests {

    void testFlush() {
        def bookClass = ga.getDomainClass("SaveBook")
        def authorClass = ga.getDomainClass("SaveAuthor")
        def addressClass = ga.getDomainClass("SaveAddress")

        def book = bookClass.newInstance()
        book.title = "Foo"
        def author = authorClass.newInstance()
        book.author = author
        author.name = "Bar"
        def address = addressClass.newInstance()
        author.address = address
        address.location = "Foo Bar"
        assert author.save()

        assert book.save(flush:true)
        assert book.id
    }
	void testToOneCascadingValidation() {
        def bookClass = ga.getDomainClass("SaveBook")
        def authorClass = ga.getDomainClass("SaveAuthor")
        def addressClass = ga.getDomainClass("SaveAddress")

        def book = bookClass.newInstance()

        assert !book.save()
        assert !book.save(deepValidate:false)

        book.title = "Foo"

        assert book.save()

        def author = authorClass.newInstance()
        author.name = "Bar"
        author.save()

        book.author = author

        // will validate book is owned by author
        assert book.save()
        assert book.save(deepValidate:false)


        assert book.save()
        assert book.save(deepValidate:false)

        def address = addressClass.newInstance()

        author.address = address

        assert !author.save()


        address.location = "Foo Bar"

        assert author.save()
        assert author.save(deepValidate:false)
	}

	void testToManyCascadingValidation() {
        def bookClass = ga.getDomainClass("SaveBook")
        def authorClass = ga.getDomainClass("SaveAuthor")
        def addressClass = ga.getDomainClass("SaveAddress")

        def author = authorClass.newInstance()

        assert !author.save()
        author.name = "Foo"

        assert author.save()


        def address = addressClass.newInstance()
        author.address = address

        assert !author.save()


        address.location = "Foo Bar"
        assert author.save()


        def book = bookClass.newInstance()

        author.addToBooks(book)
        assert !author.save()


        book.title = "TDGTG"
        assert author.save()
        assert author.save(deepValidate:false)
	}

    void testValidationAfterBindingErrors() {
        def teamClass = ga.getDomainClass('Team')
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
        def teamClass = ga.getDomainClass('Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        def msg = shouldFail(ValidationException) {
            team.save(failOnError: true)
        }
        assertEquals 'Validation Error(s) Occurred During Save', msg
    }

    void testFailOnErrorFalseWithValidationErrors() {
        def teamClass = ga.getDomainClass('Team')
        def team = teamClass.newInstance()
        team.properties = [homePage: 'invalidurl']
        assertNull 'save should have returned null', team.save(failOnError: false)
    }

    void onSetUp() {
		this.gcl.parseClass('''
import grails.persistence.*

@Entity
class Team {
    String name
    URL homePage
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
'''
		)
	}

	void onTearDown() {

	}
}
