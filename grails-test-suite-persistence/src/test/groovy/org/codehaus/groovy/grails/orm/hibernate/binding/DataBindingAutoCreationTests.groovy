package org.codehaus.groovy.grails.orm.hibernate.binding

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DataBindingAutoCreationTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class DataBindingAutoCreationAuthor {
    String name
}

@Entity
class DataBindingAutoCreationBook {
    String title
    DataBindingAutoCreationAuthor author

    static constraints = {
        author(nullable:true)
    }
}
'''
    }

    void testBindToNullIfNullable() {
        def Author = ga.getDomainClass("DataBindingAutoCreationAuthor").clazz
        def Book = ga.getDomainClass("DataBindingAutoCreationBook").clazz

        def a = Author.newInstance(name:"Stephen King")
        assertNotNull 'should have saved author', a.save(flush:true)

        session.clear()

        // should bind to null
        def params = [title:"It", 'author.id':'']
        def b1 = Book.newInstance(params)
        assertNotNull "should have saved book", b1.save(flush:true)
        assertNull "book.author is null", b1.author

        // should allow "null" for null: (see GrailsDataBinder.NULL_ASSOCIATION)
        params = [title:"It", 'author.id':'null']
        def b2 = Book.newInstance(params)
        assertNotNull "should have saved book", b2.save(flush:true)
        assertNull "book.author is null", b2.author
    }

    void testAutoCreationDuringManyToOneChildPropertyBinding() {
        def Book = ga.getDomainClass("DataBindingAutoCreationBook").clazz

        def book = Book.newInstance(title: "The Shining").save(flush: true, failOnError: true)
        session.clear()
        book = book.refresh()

        def params = ['author.name':"Stephen King"]
        book.properties['author.name', 'title'] = params

        assertEquals "The author should have been auto-created, and name should have been bound",
            "Stephen King", book.author.name
    }

    void testAutoCreationDuringManyToOneChildPropertyBindingRespectsIncludesExcludes() {
        def Book = ga.getDomainClass("DataBindingAutoCreationBook").clazz

        def book = Book.newInstance(title: "The Shining").save(flush: true, failOnError: true)
        session.clear()
        book = book.refresh()

        def params = ['author.name':"Stephen King"]
        book.properties['title','reviewers'] = params

        assertNull "The author should not have been auto-created", book.author
    }

}
