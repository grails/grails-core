package org.codehaus.groovy.grails.orm.hibernate

import grails.orm.PagedResultList

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 22, 2007
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
        def bookClass = ga.getDomainClass("ListableBook").clazz
        ['A','C','b', 'a', 'c', 'B'].each { bookClass.newInstance(title:it).save(flush:true) }

        def results = bookClass.list(max: 2, offset: 0)
        assertTrue 'results should have been a PagedResultList', results instanceof PagedResultList
    }
}
