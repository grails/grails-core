package org.codehaus.groovy.grails.orm.hibernate

class FindMethodTests extends AbstractGrailsHibernateTests {

    void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class FindMethodTestClass {
    String one
    Integer two
}
'''
    }

    void testFindMethodWithHQL() {
        def domain = ga.getDomainClass("FindMethodTestClass").clazz

        assertNotNull "should have saved", domain.newInstance(one:"one", two:2).save(flush:true)

        session.clear()

        assert domain.find("from FindMethodTestClass as f where f.one = ? and f.two = ?", ["one", 2]) : "should have returned a result"
    }
}