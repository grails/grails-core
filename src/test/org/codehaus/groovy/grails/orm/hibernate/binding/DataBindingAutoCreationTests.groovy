package org.codehaus.groovy.grails.orm.hibernate.binding

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class DataBindingAutoCreationTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
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


''')
    }


    void testBindToNullIfNullable() {
        def Author = ga.getDomainClass("DataBindingAutoCreationAuthor").clazz
        def Book = ga.getDomainClass("DataBindingAutoCreationBook").clazz

        def a = Author.newInstance(name:"Stephen King")
        assert a.save(flush:true) : 'should have saved author'

        session.clear()
                    
        // should bind to null
        def params = [title:"It", 'author.id':'']

        def b1 = Book.newInstance(params)

        assert b1.save(flush:true) : "should have saved book"
    }
}