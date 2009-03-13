/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 22, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
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


        assertEquals( ['a','A','B','b',  'c', 'C'], bookClass.list(sort:'title').title )
        assertEquals( ['A','B','C', 'a', 'b', 'c'], bookClass.list(sort:'title', ignoreCase:false).title )
    }

}