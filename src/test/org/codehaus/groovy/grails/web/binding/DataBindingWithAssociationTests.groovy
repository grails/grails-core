package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.mock.web.MockHttpServletRequest

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DataBindingWithAssociationTests extends AbstractGrailsHibernateTests{

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

        def b= Book.newInstance(params)

        assert b.save(flush:true) : "should have saved book"

    }
}
