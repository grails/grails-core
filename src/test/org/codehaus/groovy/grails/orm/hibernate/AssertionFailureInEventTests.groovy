package org.codehaus.groovy.grails.orm.hibernate


/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class AssertionFailureInEventTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class AssertionParent {
    static hasMany = [ childs : AssertionChild ]

    def beforeUpdate =
    {
        calc()
    }

    def calc =
    {
        this.childs.each {
            it.s = "change"
        }

    }
}

@Entity
class AssertionChild {
    static belongsTo = [AssertionParent];
    static hasMany = [ subChilds : AssertionSubChild ]
    String s
}

@Entity
class AssertionSubChild {
    static belongsTo = [AssertionChild];
    String s
}
''')
    }


    // test for HHH-2763 and GRAILS-4453
    void testNoAssertionErrorInEvent() {
        def Parent = ga.getDomainClass("AssertionParent").clazz
        def Child = ga.getDomainClass("AssertionChild").clazz
        def p = Parent.newInstance().save()
        p.addToChilds(Child.newInstance(s:"one"))
        p.save(flush:true)

        session.clear()

        p = Parent.findById(1, [fetch:[childs:'join']])

        p.addToChilds(Child.newInstance(s:"two"))
        p.save(flush:true)

        session.clear()

        p = Parent.get(1)
        p.childs.each { println it.s }
    }


}