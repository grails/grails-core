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
        def results = dc.clazz.createCriteria().list(max: 10, offset: 0) {
            like("title","Book%")
        }
        assertTrue 'results should have been a PagedResultList', results instanceof PagedResultList
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
