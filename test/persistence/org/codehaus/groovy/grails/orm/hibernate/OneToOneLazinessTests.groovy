package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

import org.hibernate.Hibernate

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

        def book = author.book

        assertFalse "one-to-one association should have been lazy loaded", Hibernate.isInitialized(book)


        assertEquals "The Stand", book.title

        assertTrue "lazy one-to-one association should have been initialized",Hibernate.isInitialized(book)
    }

    void testDynamicFinderWithLazyProxy() {
       def bookClass = ga.getDomainClass("OneToOneLazinessTestsBook").clazz
        def authorClass= ga.getDomainClass("OneToOneLazinessTestsAuthor").clazz

        def author = authorClass.newInstance(name:"Stephen King", book:bookClass.newInstance(title:"The Stand") )
        assert author.save()


        session.clear()

        author = authorClass.get(1)

        def book = author.book

        author.discard()

        assertFalse "one-to-one association should have been lazy loaded", Hibernate.isInitialized(book)



        assertNotNull "Finders with dynamic proxies aren't working!", authorClass.findByBook(book)        
    }



}