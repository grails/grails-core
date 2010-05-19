package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Mar 14, 2008
 */
class ExplicitInsertWithSaveMethodTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class ExplicitInsertTest {
    Long id
    Long version
    String name
}
'''
    }

    void testExplicitInsert() {
        def test = ga.getDomainClass("ExplicitInsertTest").newInstance()
        test.name = "Foo"
        assertNotNull test.save(insert:true, flush:true)

        session.clear()

        test = ga.getDomainClass("ExplicitInsertTest").clazz.get(1)
        assertNotNull test
        test.name = "Bar"
        test.save(insert:true, flush:true)
    }
}
