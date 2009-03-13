package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: May 23, 2008
 */
class GetMethodTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''
class GetMethodTest {
    Long id
    Long version
    String name
}

class GetMethodZeroIdTest {
    Long id
    Long version
    static mapping = {
        id generator:'assigned'
    }
}
''')
    }


    void testGetMethod() {
        def testClass = ga.getDomainClass("GetMethodTest").clazz

        assertNull testClass.get(null)
        assertNull testClass.get(1)
        assertNull testClass.get(1L)

        assert testClass.newInstance(name:"Foo").save(flush: true)

        assertNotNull testClass.get(1)
    }

    void testReadMethod() {
        def testClass = ga.getDomainClass("GetMethodTest").clazz

        assertNull testClass.read(null)
        assertNull testClass.read(1)
        assertNull testClass.read(1L)

        assert testClass.newInstance(name:"Foo").save(flush: true)

        assertNotNull testClass.read(1L)

        def test = testClass.read(1)

        test.name = "Bar"

        session.flush()
        session.clear()

        test = testClass.read(1)

        assertEquals "Foo", test.name

    }


    void testGetMethodZeroId() {
        def testZeroIdClass = ga.getDomainClass("GetMethodZeroIdTest").clazz

        assertNull testZeroIdClass.get(null)
        assertNull testZeroIdClass.get(0)
        assertNull testZeroIdClass.get(0L)

        def zeroInstance = testZeroIdClass.newInstance()
        zeroInstance.id = 0
        assert zeroInstance.save(flush: true)

        assertNotNull testZeroIdClass.get(0)
    }
}