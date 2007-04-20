package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*

class SavePersistentMethodTests extends AbstractGrailsHibernateTests {

    void testFlush() {
        def bookClass = ga.getDomainClass("Book")
        def authorClass = ga.getDomainClass("Author")
        def addressClass = ga.getDomainClass("Address")

        def book = bookClass.newInstance()
        book.title = "Foo"
        def author = authorClass.newInstance()
        book.author = author
        author.name = "Bar"
        def address = addressClass.newInstance()
        author.address = address
        address.location = "Foo Bar"

        assert book.save(flush:true)
        assert book.id
    }
	void testToOneCascadingValidation() {
        def bookClass = ga.getDomainClass("Book")
        def authorClass = ga.getDomainClass("Author")
        def addressClass = ga.getDomainClass("Address")

        def book = bookClass.newInstance()

        assert !book.save()
        assert !book.save(deepValidate:false)

        book.title = "Foo"

        assert !book.save()
        assert !book.save(deepValidate:false)

        def author = authorClass.newInstance()
        book.author = author

        // will validate book is owned by author
        assert book.save()
        assert book.save(deepValidate:false)

        author.name = "Bar"

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
        def bookClass = ga.getDomainClass("Book")
        def authorClass = ga.getDomainClass("Author")
        def addressClass = ga.getDomainClass("Address")

        def author = authorClass.newInstance()

        assert !author.save()
        author.name = "Foo"

        assert !author.save()
        assert !author.save(deepValidate:false)

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

	void onSetUp() {
		this.gcl.parseClass('''
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
