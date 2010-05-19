package org.codehaus.groovy.grails.orm.hibernate

import java.sql.ResultSet

/**
 * @author Joshua Burnett
 * @since 1.2.1
 */
class DiscriminatorFormulaMappingTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class Root {
    Integer tree
    static mapping = {
        discriminator value:"1", formula:'case when tree in (1, 9) or tree is null then 1 when tree in (2, 22) then 2 else tree end', type:"integer"
    }
    static constraints = {
        tree(nullable:true)
    }
}

@Entity
class Child2 extends Root {
    Owner owner
    static mapping = {
        discriminator "2"
    }
    static constraints = {
        owner(nullable:true)
    }
    def beforeInsert = {
        tree = tree?:2
    }
}

@Entity
class Child3 extends Child2 {
    String name

    static mapping = {
        discriminator "3"
    }
    def beforeInsert = {
        tree = tree?:3
    }
}

@Entity
class Owner {

    static constraints = {}

    static hasMany = [childList:Child3,child2List:Child2]
}
'''
    }

    void testDiscriminatorMapping() {
        def Root = ga.getDomainClass("Root").clazz
        def Child2 = ga.getDomainClass("Child2").clazz
        def Child3 = ga.getDomainClass("Child3").clazz

        //save with 1
        assertNotNull "should have saved root", Root.newInstance().save(flush:true)
        //save with 9
        def root = Root.newInstance()
        root.tree = 9
        assertNotNull "should have saved root 9", root.save(flush:true)
        root = Root.newInstance()
        root.tree = 2 //save a child2 from root
        assertNotNull "should have saved child2", root.save(flush:true)

        session.clear()

        assertEquals 3, Root.list().size()

        def child2 = Child2.newInstance()
        child2.tree = 22
        assertNotNull "should have saved child2", child2.save(flush:true)

        def child3 = Child3.newInstance(name:"josh")

        assertNotNull "should have saved child3", child3.save(flush:true)

        session.clear()

        assertEquals 5, Root.list().size()
        session.clear()

        assertEquals 3, Child2.list().size()
        assertEquals 1, Child3.list().size()

        def conn = session.connection()

        ResultSet rs = conn.prepareStatement("select tree from root").executeQuery()
        rs.next()
        assertEquals null, rs.getString("tree")
        rs.next()
        assertEquals 9, rs.getInt("tree")
        rs.next()
        assertEquals 2, rs.getInt("tree")
        rs.next()
        assertEquals 22, rs.getInt("tree")
        rs.next()
        assertEquals 3, rs.getInt("tree")

        rs.close()
    }

    //test Work around for http://opensource.atlassian.com/projects/hibernate/browse/HHH-2855
    void testDiscriminatorMapping_HHH_2855() {
        def Root = ga.getDomainClass("Root").clazz
        def Child3 = ga.getDomainClass("Child3").clazz
        def Child2 = ga.getDomainClass("Child2").clazz
        def Owner = ga.getDomainClass("Owner").clazz

        def owner = Owner.newInstance()
        owner.addToChildList(Child3.newInstance())
        owner.addToChild2List(Child2.newInstance())

        assertNotNull "should have saved instance", owner.save(flush:true)

        session.clear()

        def a = Owner.get(1)

        def children = a.childList
        assertEquals 1, children.size()
        def children2 = a.child2List
        assertEquals 2, children2.size() //this will end with 2 because child3 extend child2
    }
}
