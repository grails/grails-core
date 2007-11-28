/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 28, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class FindByMethodTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class Book {
    Long id
    Long version
    String title
    Date releaseDate
    static constraints  = {
        releaseDate(nullable:true)
    }
}
'''
    }


    void testNullParameters() {
        def bookClass = ga.getDomainClass("Book").clazz

        assert bookClass.newInstance(title:"The Stand").save()

        assert bookClass.findByReleaseDate(null)
        assert bookClass.findByTitleAndReleaseDate("The Stand", null)

    }

}