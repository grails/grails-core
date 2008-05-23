package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 23, 2008
 */
class GetMethodTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
class GetMethodTest {
    Long id
    Long version
}
''')
    }


    void testGetMethod() {
        def testClass = ga.getDomainClass("GetMethodTest").clazz

        assertNull testClass.get(null)
        assertNull testClass.get(1)
        assertNull testClass.get(1L)

        assert testClass.newInstance().save(flush:true)

        assertNotNull testClass.get(1)
    }


}