package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.*
import org.hibernate.LazyInitializationException

class ManyToManyLazinessTests extends AbstractGrailsHibernateTests {

    void testManyToManyLazyLoading() {
        def authorClass = ga.getDomainClass("M2MLAuthor")
        def bookClass = ga.getDomainClass("M2MLBook")
        def a = authorClass.newInstance()

        a.addToBooks(bookClass.newInstance())
        a.save()
        session.flush()

        session.evict(a)

        a = authorClass.clazz.get(1)
        session.evict(a)
        assertFalse session.contains(a)

        shouldFail(LazyInitializationException) {
            assertEquals 1, a.books.size()
        }
    }

    void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class M2MLBook {
    static belongsTo = M2MLAuthor
    static hasMany = [authors:M2MLAuthor]
}

@Entity
class M2MLAuthor {
    static hasMany = [books:M2MLBook]
}
'''
    }
}
