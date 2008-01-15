package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*

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

	void onSetUp() {
		this.gcl.parseClass('''
class SaveBook {
    Long id
    Long version
    String title
    SaveAuthor author
    static belongsTo = SaveAuthor
    static constraints = {
       title(blank:false, size:1..255)
       author(nullable:true)
    }
}
class SaveAuthor {
   Long id
   Long version
   String name
   SaveAddress address
   Set books = new HashSet()
   static hasMany = [books:SaveBook]
   static constraints = {
        address(nullable:true)
        name(size:1..255, blank:false)
   }
}
class SaveAddress {
    Long id
    Long version
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
