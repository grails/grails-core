package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.Session

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Sep 4, 2008
 */
class WithSessionMethodTests extends AbstractGrailsHibernateTests {

    void testWithSessionMethod() {
        def testClass = ga.getDomainClass("WithSessionMethod").clazz

        Session testSession
        testClass.withSession { Session session ->
            testSession = session
        }

        assertNotNull testSession
    }

    void testWithNewSessionMethod() {

        def testClass = ga.getDomainClass("WithSessionMethod").clazz

        Session testSession
        def returnValue = testClass.withNewSession { Session session ->
            testSession = session
            5
        }

        assertNotNull testSession
        assertEquals 5, returnValue
    }

    protected void onSetUp() {
        gcl.parseClass '''
class WithSessionMethod {
    Long id
    Long version
}
'''
    }
}
