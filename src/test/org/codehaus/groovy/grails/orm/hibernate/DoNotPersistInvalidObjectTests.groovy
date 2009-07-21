package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class DoNotPersistInvalidObjectTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class DoNotPersist {
    String name

    static constraints = {
        name size:1..5
    }
}
''')
    }


    void testDoNoPersistInvalidInstanceUsingDirtyChecking() {
        def testDomain = ga.getDomainClass("DoNotPersist").clazz

        def t = testDomain.newInstance(name:"bob")

        assertNotNull "should have saved test instance",t.save(flush:true)

        session.clear()

        t = testDomain.get(1)
        t.name = "fartooolong"

        session.flush()
        session.clear()

        t = testDomain.get(1)
        assertEquals "bob", t.name
    }

    void testPersistValidInstanceUsingDirtyChecking() {
        def testDomain = ga.getDomainClass("DoNotPersist").clazz

        def t = testDomain.newInstance(name:"bob")

        assertNotNull "should have saved test instance",t.save(flush:true)

        session.clear()

        t = testDomain.get(1)
        t.name = "fred"

        session.flush()
        session.clear()

        t = testDomain.get(1)
        assertEquals "fred", t.name
    }

}