package org.codehaus.groovy.grails.orm.hibernate

import junit.framework.TestCase

class ManyToManyCompositeIdTests extends AbstractGrailsHibernateTests {

    void testManyToManyDomain() {
        def authorDomain = ga.getDomainClass("ManyToManyCompositeIdAuthor")
        def bookDomain = ga.getDomainClass("ManyToManyCompositeIdBook")

        def books = authorDomain?.getPropertyByName("books")
        def authors = bookDomain?.getPropertyByName("authors")

        assert books?.isManyToMany()
        assert authors?.isManyToMany()
        assert !books?.isOneToMany()
        assert !authors?.isOneToMany()
    }

    void testManyToManyMapping() {
        def Author = ga.getDomainClass("ManyToManyCompositeIdAuthor").clazz
        def Book = ga.getDomainClass("ManyToManyCompositeIdBook").clazz

        def a = Author.newInstance(name:"Stephen King", family:10, child:1)

        a.addToBooks(Book.newInstance(title:"The Shining", isbn:5001, edition:2))
         .addToBooks(Book.newInstance(title:"The Stand", isbn:3402, edition:4))
         .save(true)
        assertEquals 2, Book.list().size()

        def b = Book.get(Book.newInstance(isbn:5001, edition:2))
        assert b
        assertNotNull b.authors
        assertEquals 1, b.authors.size()

        a = Author.get(Author.newInstance(family:10, child:1))
        assert a
        assertNotNull a.books
        assertEquals 2, a.books.size()

        assertEquals b, a.books.find { (it.isbn == 5001) && (it.edition == 2) }
        session.flush()
        session.clear()

        a = Author.get(Author.newInstance(family:10, child:1))
        assert a
        assert a.books

        b = Book.get(Book.newInstance(isbn:5001, edition:2))
        assert b
        assert b.authors

        // now let's test the database state
        def c = session.connection()

        def ps = c.prepareStatement("select * from many_to_many_composite_id_author_books order by many_to_many_composite_id_book_isbn")
        def rs = ps.executeQuery()
        assert rs.next()

        assertEquals 10,rs.getInt("many_to_many_composite_id_author_family")
        assertEquals 1,rs.getInt("many_to_many_composite_id_author_child")
        assertEquals 3402,rs.getInt("many_to_many_composite_id_book_isbn")
        assertEquals 4,rs.getInt("many_to_many_composite_id_book_edition")

        assert rs.next()
        assertEquals 10,rs.getInt("many_to_many_composite_id_author_family")
        assertEquals 1,rs.getInt("many_to_many_composite_id_author_child")
        assertEquals 5001,rs.getInt("many_to_many_composite_id_book_isbn")
        assertEquals 2,rs.getInt("many_to_many_composite_id_book_edition")
    }

    void onSetUp() {
        this.gcl.parseClass('''
import grails.persistence.*

@Entity
class ManyToManyCompositeIdBook implements Serializable {
    Long isbn
    Long edition
    String title
    static belongsTo = ManyToManyCompositeIdAuthor
    static hasMany = [authors:ManyToManyCompositeIdAuthor]

    static mapping = {
        id composite:['isbn', 'edition'], generator:'assigned'
    }
}

@Entity
class ManyToManyCompositeIdAuthor implements Serializable {
    Long family
    Long child
    String name
    static hasMany = [books:ManyToManyCompositeIdBook]

    static mapping = {
        id composite:['family', 'child'], generator:'assigned'
    }
}
''')
    }
}
