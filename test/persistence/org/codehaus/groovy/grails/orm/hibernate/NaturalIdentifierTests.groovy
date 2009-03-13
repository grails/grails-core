package org.codehaus.groovy.grails.orm.hibernate

import org.springframework.orm.hibernate3.HibernateSystemException
import org.springframework.dao.DataIntegrityViolationException

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class NaturalIdentifierTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class NaturalAuthor {
    String name
}

@Entity
class NaturalBook {
    String title
    NaturalAuthor author

    static mapping = {
        id natural:['title', 'author']
    }
}

@Entity
class NaturalBook2 {
    String title
    NaturalAuthor author

    static mapping = {
        id natural:[properties:['title', 'author'], mutable:true]
    }
}
''')
    }


    void testNaturalIdentifier() {
        def Book = ga.getDomainClass("NaturalBook").clazz
        def Author = ga.getDomainClass("NaturalAuthor").clazz

        def a = Author.newInstance(name:"Stephen King").save(flush:true)

        def b = Book.newInstance(author:a, title:"The Stand").save(flush:true)

        assertNotNull b

        b.title = "Changed"

        // should fail with an attempt to alter an immutable natural identifier
        shouldFail(HibernateSystemException) {
            b.save(flush:true)
        }

        // should fail with a unique constraint violation exception
        shouldFail(DataIntegrityViolationException) {
            Book.newInstance(author:a, title:"The Stand").save(flush:true)
        }

    }

    void testMutalbeNaturalIdentifier() {
       def Book = ga.getDomainClass("NaturalBook2").clazz
        def Author = ga.getDomainClass("NaturalAuthor").clazz

        def a = Author.newInstance(name:"Stephen King").save(flush:true)

        def b = Book.newInstance(author:a, title:"The Stand").save(flush:true)

        assertNotNull b

        b.title = "Changed"
        // mutable identifier so no problem
        b.save(flush:true)

        // should fail with a unique constraint violation exception
        shouldFail(DataIntegrityViolationException) {
            Book.newInstance(author:a, title:"Changed").save(flush:true)
        }
    }

}