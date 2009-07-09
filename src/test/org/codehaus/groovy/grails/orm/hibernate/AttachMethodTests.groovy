package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jul 21, 2008
 */
class AttachMethodTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
class AttachMethod {
    Long id
    Long version
    String name
}
''')
    }


    void testAttachMethod() {
        def testClass = ga.getDomainClass("AttachMethod").clazz

        def test = testClass.newInstance(name:"foo")

        assert test.save(flush:true)

        assert session.contains(test)
        assert test.isAttached()
        assert test.attached

        test.discard()

        assert !session.contains(test)
        assert !test.isAttached()
        assert !test.attached

        test.attach()

        assert session.contains(test)
        assert test.isAttached()
        assert test.attached

        test.discard()

        assertEquals test, test.attach()
    }

}