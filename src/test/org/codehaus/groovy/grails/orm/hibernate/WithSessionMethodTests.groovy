package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.Session

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Sep 4, 2008
 */
class WithSessionMethodTests extends AbstractGrailsHibernateTests{


    void testWithSessionMethod() {
        def testClass = ga.getDomainClass("WithSessionMethod").clazz

        Session testSession
        testClass.withSession { Session session ->
              testSession = session
        }

        assertNotNull testSession
    }

    protected void onSetUp() {
        gcl.parseClass('''
class WithSessionMethod {
    Long id
    Long version
}
''')
    }


}