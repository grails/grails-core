package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Mar 18, 2008
 */
class TablePerHierarchyAsssocationTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass '''
class  TablePerHierarchyRoot {
     Long id
     Long version
     String name
     TablePerHierarchOneToMany one
}
class TablePerHierarchSub1 extends TablePerHierarchyRoot {}
class TablePerHierarchSub2 extends TablePerHierarchyRoot {}

class TablePerHierarchOneToMany {
   Long id
   Long version
   Set subs
   Set all
   static hasMany = [subs:TablePerHierarchSub1, all:TablePerHierarchyRoot]
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
                    .addToAll(name:"three")


        test.addToAll(sub2Class.newInstance(name:"four"))
        test.addToAll(rootClass.newInstance(name:"five"))

        assert test.save(flush:true)

        session.clear()

        test = testClass.get(1)

        assertEquals 2, test.subs.size()
        assertEquals 5, test.all.size()


    }
}