package org.codehaus.groovy.grails.orm.hibernate;

class ValidatePersistentMethodTests extends AbstractGrailsHibernateTests {

    void testClearErrorsBetweenValidations() {
        def personClass = ga.getDomainClass('Person')
        def person = personClass.newInstance()
        person.age = 999
        assertFalse 'validation should have failed for invalid age', person.validate()
        assertTrue 'person should have had errors because of invalid age', person.hasErrors()
        person.clearErrors()
        assertFalse 'person should not have had errors', person.hasErrors()
        person.age = 9
        assertTrue 'validation should have succeeded', person.validate()
        assertFalse 'person should not have had errors', person.hasErrors()
        person.age = 999
        assertFalse 'validation should have failed for invalid age', person.validate()
        assertTrue 'person should have had errors because of invalid age', person.hasErrors()
    }

    void testToOneCascadingValidation() {
        def bookClass = ga.getDomainClass("Book")
        def authorClass = ga.getDomainClass("Author")
        def addressClass = ga.getDomainClass("Address")

        def book = bookClass.newInstance()

        assert !book.validate()
        assert !book.validate(deepValidate:false)

        book.title = "Foo"

        assert !book.validate()
        assert !book.validate(deepValidate:false)

        def author = authorClass.newInstance()
        book.author = author

        assert book.validate()
        assert book.validate(deepValidate:false)

        author.name = "Bar"

        assert book.validate()
        assert !author.validate()
        assert book.validate(deepValidate:false)

        def address = addressClass.newInstance()

        author.address = address

        assert book.validate()
        assert !author.validate()
        assert book.validate(deepValidate:false)

        address.location = "Foo Bar"

        assert book.validate()
        assert author.validate()
        assert book.validate(deepValidate:false)
	}

	void testToManyCascadingValidation() {
        def bookClass = ga.getDomainClass("Book")
        def authorClass = ga.getDomainClass("Author")
        def addressClass = ga.getDomainClass("Address")

        def author = authorClass.newInstance()

        assert !author.validate()
        author.name = "Foo"

        assert !author.validate()
        assert !author.validate(deepValidate:false)

        def address = addressClass.newInstance()
        author.address = address

        assert !author.validate()
        assert author.validate(deepValidate:false)

        address.location = "Foo Bar"
        assert author.validate()
        assert author.validate(deepValidate:false)

        def book = bookClass.newInstance()

        author.addToBooks(book)
        assert !author.validate()
        assert author.validate(deepValidate:false)

        book.title = "TDGTG"
        assert author.validate()
        assert author.validate(deepValidate:false)

	}

    void testFilteringValidation() {
        // Test validation on a sub-set of the available fields.
        def profileClass = ga.getDomainClass('Profile')
        def profile = profileClass.newInstance()
        profile.email = "This is not an e-mail address"
        assertFalse "Validation should have failed for invalid e-mail address", profile.validate([ "email" ])
        assertEquals 'Profile should have only one error', 1, profile.errors.errorCount
        assertTrue "'email' field should have errors associated with it", profile.errors.hasFieldErrors("email")

        // Now test the behaviour when the object has errors, but none
        // of the fields included in the filter has errors.
        profile.email = "someone@somewhere.org"
        assertTrue "Validation should not have failed for valid e-mail address", profile.validate([ "email" ])
        assertFalse 'Profile should have no errors', profile.hasErrors()

        // Finally check that without the filtering, the target would
        // have errors.
        assertFalse "Validation should have failed", profile.validate()
        assertTrue 'Profile should have errors', profile.hasErrors()
    }

    void onSetUp() {
		this.gcl.parseClass('''
class Person {
    Long id
    Long version
    Integer age
    static constraints = {
      age(max:99)
    }
}
class Profile {
    Long id
    Long version
    String firstName
    String lastName
    String email
    Date dateOfBirth

    static constraints = {
        firstName(nullable: false, blank: false)
        lastName(nullable: false, blank: false)
        email(nullable: true, blank: true, email: true)
        dateOfBirth(nullable: true)
    }
}
class Book {
    Long id
    Long version
    String title
    Author author
    static belongsTo = Author
    static constraints = {
       title(blank:false, size:1..255)
       author(nullable:false)
    }
}
class Author {
   Long id
   Long version
   String name
   Address address
   Set books = new HashSet()
   static hasMany = [books:Book]
   static constraints = {
        address(nullable:false)
        name(size:1..255, blank:false)
   }
}
class Address {
    Long id
    Long version
    Author author
    String location
    static belongsTo = Author
    static constraints = {
       author(nullable:false)
       location(blank:false)
    }
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
