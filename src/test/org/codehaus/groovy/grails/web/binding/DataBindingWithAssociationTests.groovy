package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.mock.web.MockHttpServletRequest

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DataBindingWithAssociationTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Book {
    static belongsTo = [author: Author]
    String isbn
    String title
    static constraints = {
        isbn(unique: true, size: 1..10)
        title(size: 1..40)
    }
}

@Entity
class Author {
    static hasMany = [books: Book]
    String name
    static constraints = {
        name(size: 1..40)
        books sort:"title"
    }
}
''')
    }

    void testDataBindingWithAssociation() {
        def Author = ga.getDomainClass("Author").clazz
        def Book = ga.getDomainClass("Book").clazz

        def a = Author.newInstance(name:"Stephen").save(flush:true)

        assert a != null : "should have saved the Author"

        def request = new MockHttpServletRequest()
        request.addParameter("title","Book 1")
        request.addParameter("isbn", "938739")
        request.addParameter("author.id", "1")
        def params = new GrailsParameterMap(request)

        def b2 = Book.newInstance()
        b2.properties['title', 'isbn', 'author'] = params

        assert b2.save(flush:true) : "should have saved book"

        // test traditional binding

        request = new MockHttpServletRequest()
        request.addParameter("title","Book 2")
        request.addParameter("isbn", "5645674")
        request.addParameter("author.id", "1")
        params = new GrailsParameterMap(request)

        def b = Book.newInstance(params)

        assert b.save(flush:true) : "should have saved book"
    }
	
	void testBindToSetCollection() {
		def Author = ga.getDomainClass("Author").clazz
		def Book = ga.getDomainClass("Book").clazz

		def a = Author.newInstance(name:"Stephen King")
					.addToBooks(title:"The Stand", isbn:"983479")
					.addToBooks(title:"The Shining", isbn:"232309")
					.save(flush:true)
		
	
		assert a != null : "should have saved the Author"
		
		Author.withSession { session -> session.clear() }
		
		a = Author.findByName("Stephen King")
		
		def request = new MockHttpServletRequest()
		request.addParameter("books[0].isbn","12345")
		request.addParameter("books[1].isbn","54321")
		def params = new GrailsParameterMap(request)
		
		a.properties = params
		
		assert a.books.find { it.isbn == "12345" } != null
		assert a.books.find { it.isbn == "54321" } != null
	}
	
	void testBindToNewInstance() {
		def Author = ga.getDomainClass("Author").clazz
		def Book = ga.getDomainClass("Book").clazz

		def a = Author.newInstance(name:"Stephen King")
		
	
		
		def request = new MockHttpServletRequest()
		request.addParameter("books[0].isbn","12345")
		request.addParameter("books[0].title","The Shining")
		
		request.addParameter("books[1].isbn","54321")
		request.addParameter("books[1].title","The Stand")
		
		def params = new GrailsParameterMap(request)
		
		a.properties = params
		
		assert a.books.find { it.isbn == "12345" }?.title == "The Shining"
		assert a.books.find { it.isbn == "54321" }?.title == "The Stand"
	}
	

}
