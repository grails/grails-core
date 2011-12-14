package org.codehaus.groovy.grails.orm.hibernate

import grails.orm.PagedResultList

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ListMethodTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class ListableBook {
    Long id
    Long version
    String title
}
'''
    }

    void testSortAndWithIgnoreCase() {
        def bookClass = ga.getDomainClass("ListableBook").clazz
        ['A','C','b', 'a', 'c', 'B'].each { bookClass.newInstance(title:it).save(flush:true) }

        assertEquals(['A','a','b','B',  'C', 'c'], bookClass.list(sort:'title').title)
        assertEquals(['A','B','C', 'a', 'b', 'c'], bookClass.list(sort:'title', ignoreCase:false).title)
    }

    void testPaginatedQueryReturnsPagedResultList() {
        def stats = sessionFactory.statistics
        stats.statisticsEnabled = true

        def bookClass = ga.getDomainClass("ListableBook").clazz
        ['A','C','b', 'a', 'c', 'B'].each { bookClass.newInstance(title:it).save(flush:true) }

        stats.clear()
        def results = bookClass.list(max: 2, offset: 0)
        assertTrue 'results should have been a PagedResultList', results instanceof PagedResultList
        assertEquals 1, stats.queryExecutionCount

        assertEquals 2, results.size()
        assertEquals 6, results.totalCount
        assertEquals 2, stats.queryExecutionCount

        // refer to totalCount again and make sure another query was not sent to the database
        assertEquals 6, results.totalCount
        assertEquals 2, stats.queryExecutionCount
    }
}
