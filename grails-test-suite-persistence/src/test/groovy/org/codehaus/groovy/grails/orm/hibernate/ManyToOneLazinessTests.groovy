package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil

 /**
* @author Graeme Rocher
* @since 1.0
*
* Created: Jan 17, 2008
*/
class ManyToOneLazinessTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class ManyToOneLazinessTestsBook {
    String title
    ManyToOneLazinessTestsAuthor author
    static belongsTo =  ManyToOneLazinessTestsAuthor

    static mapping = {
        author lazy:true
    }
}

@Entity
class ManyToOneLazinessTestsAuthor {
    String name
    static hasMany = [books:ManyToOneLazinessTestsBook]
}
'''
    }

    void testManyToOneLaziness() {
        def bookClass = ga.getDomainClass("ManyToOneLazinessTestsBook").clazz
        def authorClass= ga.getDomainClass("ManyToOneLazinessTestsAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King")
        assertNotNull author.save()

        author.addToBooks(title:"The Stand")
              .addToBooks(title:"The Shining")
              .save(flush:true)

        session.clear()

        def book = bookClass.get(1)
        assertFalse "many-to-one association should have been lazy loaded", GrailsHibernateUtil.isInitialized(book, "author")
        assertEquals "Stephen King", book.author.name
        assertTrue "lazy many-to-one association should have been initialized",GrailsHibernateUtil.isInitialized(book, "author")
    }
}
