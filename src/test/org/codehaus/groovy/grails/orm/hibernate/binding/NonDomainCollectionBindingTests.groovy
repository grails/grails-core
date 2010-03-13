package org.codehaus.groovy.grails.orm.hibernate.binding

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
 * @author Rob Fletcher
 * @since 1.3
 */
class NonDomainCollectionBindingTests extends AbstractGrailsHibernateTests {

    void onSetUp() {
        gcl.parseClass """
class DataBindingBook {
    Long id
    Long version
    String title
    List topics
    static hasMany = [topics: String]
}
"""
    }

    void testListCanExpand() {
        def Book = ga.getDomainClass("DataBindingBook").clazz
        def book = Book.newInstance(title: "Fear & Loathing in Las Vegas", topics: ["journalism"])
        book.save(flush: true, failOnError: true)
        session.clear()

        def params = ["topics[1]": "counterculture"]

        book = book.refresh()
        book.properties = params

        assertEquals 2, book.topics.size()
        assertEquals(["journalism", "counterculture"], book.topics)
    }

    void testEmptyListElementCanBePopulated() {
        def Book = ga.getDomainClass("DataBindingBook").clazz
        def book = Book.newInstance(title: "Fear & Loathing in Las Vegas", topics: ["journalism", null, "satire"])
        book.save(flush: true, failOnError: true)
        session.clear()

        def params = ["topics[1]": "counterculture"]

        book = book.refresh()
        book.properties = params

        assertEquals 3, book.topics.size()
        assertEquals(["journalism", "counterculture", "satire"], book.topics)
    }
}
