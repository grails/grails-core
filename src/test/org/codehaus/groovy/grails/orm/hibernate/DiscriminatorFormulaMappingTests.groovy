package org.codehaus.groovy.grails.orm.hibernate

import java.sql.ResultSet

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class DiscriminatorFormulaMappingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Root {
	Integer tree = 1
    static mapping = {
        discriminator value:"1", formula:'case when tree in (1, 9) then 1 when tree in (2, 22) then 2 else tree end', type:"integer"
    }
}

@Entity
class Child2 extends Root{

    static mapping = {
       discriminator "2"
    }
}

@Entity
class Child3 extends Root {

    static mapping = {
       discriminator "3"
    }
	def beforeInsert = {
		tree = 3
	}
	
	
}

''')
    }


    void testDiscriminatorMapping() {
        def Root = ga.getDomainClass("Root").clazz
        def Child2 = ga.getDomainClass("Child2").clazz
        def Child3 = ga.getDomainClass("Child3").clazz

		//save with 1
        assertNotNull "should have saved root", Root.newInstance().save(flush:true)
		//save with 9
		def root = Root.newInstance()
		root.tree=9
		assertNotNull "should have saved root 9", root.save(flush:true)
		root = Root.newInstance()
		root.tree=2 //save a child2 from root
		assertNotNull "should have saved child2", root.save(flush:true)
		
		session.clear()
		
		assertEquals 3, Root.list().size()
		
		def child2 = Child2.newInstance()
		child2.tree = 22
		assertNotNull "should have saved child2", child2.save(flush:true)
		
		def child3 = Child3.newInstance()
		assertNotNull "should have saved child2", child3.save(flush:true)
		
		session.clear()
		
		assertEquals 5, Root.list().size()
		assertEquals 2, Child2.list().size()
		assertEquals 1, Child3.list().size()
		
        def conn = session.connection()

        ResultSet rs = conn.prepareStatement("select tree from root").executeQuery()
        rs.next()
        assertEquals 1, rs.getInt("tree")
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

}