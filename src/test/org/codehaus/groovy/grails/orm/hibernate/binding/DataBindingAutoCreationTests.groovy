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
    }
}
