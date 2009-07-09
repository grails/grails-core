package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class ManyToManyTests extends AbstractGrailsHibernateTests {

	void testManyToManyDomain() {
		def authorDomain = ga.getDomainClass("M2MAuthor")
		def bookDomain = ga.getDomainClass("M2MBook")

		
		def books = authorDomain?.getPropertyByName("books")
		def authors = bookDomain?.getPropertyByName("authors")
				
		assert books?.isManyToMany()
		assert authors?.isManyToMany()
		assert !books?.isOneToMany()
		assert !authors?.isOneToMany()
	}
	void testManyToManyMapping() {
		def Author = ga.getDomainClass("M2MAuthor").clazz
		def Book = ga.getDomainClass("M2MBook").clazz

        def a = Author.newInstance(name:"Stephen King")
		
		a.addToBooks(Book.newInstance(title:"The Shining"))
         .addToBooks(Book.newInstance(title:"The Stand"))
         .save(true)
		assertEquals 2, Book.list().size()
		
		def b = Book.get(1)
		assert b
        assertNotNull b.authors
        assertEquals 1, b.authors.size()
		
		a = Author.get(1)
		assert a
        assertNotNull a.books
        assertEquals 2, a.books.size()

		
		assertEquals b, a.books.find { it.id == 1}
		this.session.flush()
		session.clear()
		
		a = Author.get(1)
		assert a
		assert a.books
		
		b = Book.get(1)
		assert b
		assert b.authors

        // now let's test the database state
        def c = session.connection()

        def ps = c.prepareStatement("select * from m2mauthor_books")
        def rs = ps.executeQuery()
        assert rs.next()

        assertEquals 1,rs.getInt("m2mauthor_id")
        assertEquals 1,rs.getInt("m2mbook_id")

        assert rs.next()
        assertEquals 1,rs.getInt("m2mauthor_id")
        assertEquals 2,rs.getInt("m2mbook_id")

    }

    void testMappedManyToMany() {
		def Author = ga.getDomainClass("MappedM2mAuthor").clazz
		def Book = ga.getDomainClass("MappedM2mBook").clazz

        def a = Author.newInstance(name:"Stephen King")

		a.addToBooks(Book.newInstance(title:"The Shining"))
         .addToBooks(Book.newInstance(title:"The Stand"))
         .save(true)
		assertEquals 2, Book.list().size()

		def b = Book.get(1)
		assert b
        assertNotNull b.authors
        assertEquals 1, b.authors.size()

		a = Author.get(1)
		assert a
        assertNotNull a.books
        assertEquals 2, a.books.size()


		assertEquals b, a.books.find { it.id == 1}
		this.session.flush()
		session.clear()

		a = Author.get(1)
		assert a
		assert a.books

		b = Book.get(1)
		assert b
		assert b.authors

        // now let's test the database state
        def c = session.connection()

        def ps = c.prepareStatement("select * from mm_author_books")
        def rs = ps.executeQuery()
        assert rs.next()

        assertEquals 1,rs.getInt("mm_author_id")
        assertEquals 1,rs.getInt("mm_book_id")

        assert rs.next()
        assertEquals 1,rs.getInt("mm_author_id")
        assertEquals 2,rs.getInt("mm_book_id")
    }

    void onSetUp() {
		this.gcl.parseClass('''
import grails.persistence.*

@Entity
class M2MBook {
	String title
	static belongsTo = M2MAuthor
	static hasMany = [authors:M2MAuthor]
}

@Entity
class M2MAuthor {
    String name
	static hasMany = [books:M2MBook]
}

@Entity
class MappedM2mBook {

    String title
	static belongsTo = MappedM2mAuthor
	static hasMany = [authors:MappedM2mAuthor]

    static mapping = {
        authors joinTable:[name:"mm_author_books", key:'mm_book_id' ]
    }
}

@Entity
class MappedM2mAuthor {
    String name

	static hasMany = [books:MappedM2mBook]

    static mapping = {
        books joinTable:[name:"mm_author_books", key:'mm_author_id']
    }

}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
