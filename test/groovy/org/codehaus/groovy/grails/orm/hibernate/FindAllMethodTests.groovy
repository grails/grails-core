/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 30, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate
class FindAllMethodTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class FindAllTest {
    Long id
    Long version
    String name
}
'''
    }


    void testNoArgs() {
        def theClass = ga.getDomainClass("FindAllTest").clazz

        assertEquals 0, theClass.findAll().size()

        theClass.newInstance(name:"Foo").save(flush:true)

        assertEquals 1, theClass.findAll().size()

    }

}