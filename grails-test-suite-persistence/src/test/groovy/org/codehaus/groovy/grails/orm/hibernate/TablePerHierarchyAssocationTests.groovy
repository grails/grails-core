package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Mar 18, 2008
 */
class TablePerHierarchyAssocationTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class  TablePerHierarchyRoot {
     String name
     TablePerHierarchOneToMany one
}
@Entity
class TablePerHierarchSub1 extends TablePerHierarchyRoot {}
@Entity
class TablePerHierarchSub2 extends TablePerHierarchyRoot {}

@Entity
class TablePerHierarchOneToMany {

   static hasMany = [subs:TablePerHierarchSub1, roots:TablePerHierarchyRoot]
}
'''
    }

    void testLoadSubclassAssociation() {
        def testClass = ga.getDomainClass("TablePerHierarchOneToMany").clazz
        def rootClass = ga.getDomainClass("TablePerHierarchyRoot").clazz
        def sub1Class = ga.getDomainClass("TablePerHierarchSub1").clazz
        def sub2Class = ga.getDomainClass("TablePerHierarchSub2").clazz

        def test =  testClass.newInstance()
                             .addToSubs(name:"one")
                             .addToSubs(name:"two")
                             .addToRoots(name:"three")

        test.addToRoots(sub2Class.newInstance(name:"four"))
        test.addToRoots(rootClass.newInstance(name:"five"))

        assertNotNull test.save(flush:true)

        session.clear()

        test = testClass.get(1)
        assertEquals 2, test.subs.size()
        assertEquals 5, test.roots.size()
    }
}
