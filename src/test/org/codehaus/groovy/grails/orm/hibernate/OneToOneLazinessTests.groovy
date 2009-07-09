package org.codehaus.groovy.grails.orm.hibernate


/**
 * @author Graeme Rocher
 * @since 1.1
 */

import org.hibernate.Hibernate
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil

public class OneToOneLazinessTests extends AbstractGrailsHibernateTests{

   protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class OneToOneLazinessTestsBook {
    String title

    static belongsTo = [author:OneToOneLazinessTestsAuthor]
}

@Entity
class OneToOneLazinessTestsAuthor {
    String name
    OneToOneLazinessTestsBook book
}
'''
    }

    void testManyToOneLaziness() {
        def bookClass = ga.getDomainClass("OneToOneLazinessTestsBook").clazz
        def authorClass= ga.getDomainClass("OneToOneLazinessTestsAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King", book:bookClass.newInstance(title:"The Stand") )
        assert author.save()


        session.clear()

        author = authorClass.get(1)

        assertFalse "one-to-one association should have been lazy loaded", GrailsHibernateUtil.isInitialized(author, "book")


        assertEquals "The Stand", author.book.title

        assertTrue "lazy one-to-one association should have been initialized",GrailsHibernateUtil.isInitialized(author, "book")
    }

    void testDynamicFinderWithLazyProxy() {
       def bookClass = ga.getDomainClass("OneToOneLazinessTestsBook").clazz
        def authorClass= ga.getDomainClass("OneToOneLazinessTestsAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King", book:bookClass.newInstance(title:"The Stand") )
        assert author.save()


        session.clear()

        author = authorClass.get(1)

        def book = GrailsHibernateUtil.getAssociationProxy(author, "book")

        author.discard()

        assertFalse "one-to-one association should have been lazy loaded", GrailsHibernateUtil.isInitialized(author, "book")



        assertNotNull "Finders with dynamic proxies aren't working!", authorClass.findByBook(book)        
    }



}