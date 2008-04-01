package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 */
class PackagedCustomMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
package test
class PackagedCustomMapping {
   Long id
   Long version
   String name

   static mapping = {
        cache usage:'read-only'
   }
}
''')
    }


    void testCacheMapping() {
        def testClass = ga.getDomainClass("test.PackagedCustomMapping").clazz

        def test = testClass.newInstance(name:"Fred")

        assert test.save(flush:true)

        session.clear()

        test = testClass.get(1)

        test.name = "Bob"
        shouldFail(UnsupportedOperationException) {
            test.save(flush:true)
        }
    }


}