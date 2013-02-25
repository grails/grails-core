package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Jan 11, 2008
 */
class CustomCascadeMappingTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class CustomCascadeMappingOne {
    static hasMany = [foos:CustomCascadeMappingTwo, bars:CustomCascadeMappingTwo]

    static mapping = { applicationContext ->
        assert applicationContext != null
        println applicationContext
        foos cascade:'none', joinTable:'foos'
        bars cascade:'all', joinTable:'bars'
    }
}

@Entity
class CustomCascadeMappingTwo {
    String name
}
'''
    }

    void testCascadingBehaviour() {
        def oneClass = ga.getDomainClass("CustomCascadeMappingOne").clazz
        def twoClass = ga.getDomainClass("CustomCascadeMappingTwo").clazz

        def one = oneClass.newInstance()

        shouldFail {
            one.addToFoos(name:"foo1")
               .addToFoos(name:"foo2")
               .save(flush:true)
        }
        one.foos.clear()
        one.addToBars(name:"bar1")
           .addToBars(name:"bar2")
           .save(flush:true)

        session.clear()

        one = oneClass.get(1)

        assertEquals 0, one.foos.size()
        assertEquals 2, one.bars.size()
    }
}
