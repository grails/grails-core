package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class UnidirectionalOneToManyWithSortOrderTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Parent {
    static hasMany = [ childs : Child ]
    static mapping = {
       childs sort:"s"
    }
}

@Entity
class Child {
    static belongsTo = [Parent]
    String s
}

''')
    }



    void testUnidirectionalOneToManyWithSortOrder() {
        if(notYetImplemented()) return
        def Parent = ga.getDomainClass("Parent").clazz
        def Child = ga.getDomainClass("Child").clazz


        assert Parent.newInstance()
                      .addToChilds(s:"foo")
                      .addToChilds(s:"bar")
                      .addToChilds(s:"zed")
                      .addToChilds(s:"lop")
                      .save(flush:true)

        session.clear()


        def p = Parent.get(1)

        assertEquals 4, p.childs.size()
        def children = p.childs.toList()
        assertEquals "bar", children[0].s
        assertEquals "foo", children[1].s
        assertEquals "lop", children[2].s
        assertEquals "zed", children[3].s

    }
}