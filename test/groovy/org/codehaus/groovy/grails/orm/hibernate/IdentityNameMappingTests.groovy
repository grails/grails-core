package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 */

public class IdentityNameMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
class IdentityNameMapping {
    Long id
    Long version
    String test

    static mapping = {
        id name:'test', generator:'assigned'
    }
}
''')
    }


    void testIdentityNameMapping() {

        def testClass = ga.getDomainClass("IdentityNameMapping").clazz
        def test = testClass.newInstance(test: "John")

        assertNotNull "Persistent instance with named and assigned identifier should have validated", test.save(flush: true)

        session.clear()
        
        assertNotNull "Persistent instance with named and assigned identifier should have been saved", testClass.get("John")
    }

}