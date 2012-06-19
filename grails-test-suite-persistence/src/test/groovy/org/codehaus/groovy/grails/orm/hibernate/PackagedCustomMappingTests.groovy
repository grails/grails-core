package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 */
class PackagedCustomMappingTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
package test
class PackagedCustomMapping {
    Long id
    Long version
    String name

    static mapping = {
        cache usage:'read-only'
    }
}
'''
    }

    void testCacheMapping() {
        def testClass = ga.getDomainClass("test.PackagedCustomMapping").clazz

        def test = testClass.newInstance(name:"Fred")
        assertNotNull test.save(flush:true)

        session.clear()

        testClass.withNewSession {
            test = testClass.get(1)
            test.name = "Bob"
            shouldFail(IllegalStateException) {
                test.save(flush:true)
            }
        }

    }
}
