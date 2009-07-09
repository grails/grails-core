package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 */
class CircularOneToOneTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''

class CircularOneToOnePerson {
    Long id
    Long version
    CircularOneToOnePerson creator

    static constraints = {
        creator nullable:true
    }
}
''')
    }



    void testCircularOneToOne() {
        def testClass = ga.getDomainClass("CircularOneToOnePerson").clazz

        def test1 = testClass.newInstance()

        def test2 = testClass.newInstance()

        assert test1.save(flush:true)

        test2.creator = test1

        assert test2.save(flush:true)

        session.clear()

        test2 = testClass.get(2)


        assert test2
        assert test2.creator
    }
}