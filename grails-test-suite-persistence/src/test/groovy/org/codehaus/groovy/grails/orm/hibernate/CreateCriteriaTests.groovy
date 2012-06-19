package org.codehaus.groovy.grails.orm.hibernate

import grails.orm.PagedResultList

/**
 * @author Siegfried Puchbauer
 */
class CreateCriteriaTests extends AbstractGrailsHibernateTests {

    void testCreateCriteriaMethod() {
        def books = []
        def dc = ga.getDomainClass("CreateCriteriaMethodBook")
        25.times {
            def book = dc.newInstance()
            book.title = "Book $it"
            books << book
        }
        books*.save(true)

        def results = dc.clazz.createCriteria().list(max: 10, offset: 0) {
            like("title","Book%")
        }

        assertEquals 10, results?.size()
        assertEquals 25, results?.totalCount
    }

    void testPaginatedQueryReturnsPagedResultList() {
        def dc = ga.getDomainClass("CreateCriteriaMethodBook")
        def stats = sessionFactory.statistics
        stats.statisticsEnabled = true
        def books = []
        8.times {
            def book = dc.newInstance()
            book.title = "Good Book $it"
            books << book
            book = dc.newInstance()
            book.title = "Bad Book $it"
            books << book
        }
        books*.save(true)
        stats.clear()
        def results = dc.clazz.createCriteria().list(max: 3, offset: 0) {
            like("title","Good Book%")
        }
        assertTrue 'results should have been a PagedResultList', results instanceof PagedResultList
        assertEquals 1, stats.queryExecutionCount

        assertEquals 3, results.size()
        assertEquals 8, results.totalCount
        assertEquals 2, stats.queryExecutionCount

        // refer to totalCount again and make sure another query was not sent to the database
        assertEquals 8, results.totalCount
        assertEquals 2, stats.queryExecutionCount
    }

    void onSetUp() {
        gcl.parseClass """
class CreateCriteriaMethodBook {
    Long id
    Long version
    String title

    boolean equals(obj) { title == obj?.title }
    int hashCode() { title ? title.hashCode() : super.hashCode() }
    String toString() { title }

    static constraints = {
        title(nullable:false)
    }
}
"""
    }
}
