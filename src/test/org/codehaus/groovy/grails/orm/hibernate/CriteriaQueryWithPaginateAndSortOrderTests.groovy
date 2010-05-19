package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class CriteriaQueryWithPaginateAndSortOrderTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class CriteriaQueryWithPaginateAndSortOrderExample {
    String toSort
}
'''
    }

    void testCriteriaWithSortOrderAndPagination() {
        def Example = ga.getDomainClass("CriteriaQueryWithPaginateAndSortOrderExample").clazz
        assertNotNull "should have saved", Example.newInstance(toSort:"string 1").save(flush:true)
        assertNotNull "should have saved", Example.newInstance(toSort:"string 2").save(flush:true)

        session.clear()

        Example.createCriteria().list(max: 32, offset: 0) {
            and {
                like('toSort', '%string%')
            }
            order("toSort", "asc")
        }
    }
}
