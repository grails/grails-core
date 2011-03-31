package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jul 21, 2008
 */
class AttachMethodTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class AttachMethod {
    Long id
    Long version
    String name
}
'''
    }

    void testAttachMethod() {
        def testClass = ga.getDomainClass("AttachMethod").clazz

        def test = testClass.newInstance(name:"foo")

        assertNotNull test.save(flush:true)

        assertTrue session.contains(test)
        assertTrue test.isAttached()
        assertTrue test.attached

        test.discard()

        assertFalse session.contains(test)
        assertFalse test.isAttached()
        assertFalse test.attached

        test.attach()

        assertTrue session.contains(test)
        assertTrue test.isAttached()
        assertTrue test.attached

        test.discard()

        assertEquals test, test.attach()
    }
}
