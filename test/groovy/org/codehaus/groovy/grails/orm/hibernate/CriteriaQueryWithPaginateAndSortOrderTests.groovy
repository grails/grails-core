package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.criterion.Example

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class CriteriaQueryWithPaginateAndSortOrderTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class CriteriaQueryWithPaginateAndSortOrderExample {
    String toSort
}
''')
    }

    void testCriteriaWithSortOrderAndPagination() {
        def Example = ga.getDomainClass("CriteriaQueryWithPaginateAndSortOrderExample").clazz
        assert Example.newInstance(toSort:"string 1").save(flush:true) : "should have saved"
        assert Example.newInstance(toSort:"string 2").save(flush:true) : "should have saved"


        session.clear()

        Example.createCriteria().list(
                max: 32,
                offset: 0) {
            and {
                like('toSort', '%string%')
            }
            order("toSort", "asc")
        }
    }

}
