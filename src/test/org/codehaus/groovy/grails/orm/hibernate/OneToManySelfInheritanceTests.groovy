package org.codehaus.groovy.grails.orm.hibernate

import java.sql.ResultSet

/**
 * @author Joshua Burnett
 * @since 1.2.1
 */

public class OneToManySelfInheritanceTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class OrgRoot {
}

@Entity
class Org extends OrgRoot{
	String name
	static hasMany = [children: Org]
	Org parent
	static constraints = {
		parent(nullable: true)
	}
}

@Entity
class ExtOrg extends Org {
}

''')
    }


    void testOneToManyWithSelf() {
		def Root = ga.getDomainClass("OrgRoot").clazz
        def Org = ga.getDomainClass("Org").clazz

		def org1 = Org.newInstance(name:"org1")
		assertNotNull "should have saved",org1.save(flush:true)
		def orga = Org.newInstance(name:"orga",parent: org1)
		assertNotNull "should have saved",orga.save(flush:true)
		def orgb = Org.newInstance(name:"orgb",parent: org1)
		assertNotNull "should have saved",orgb.save(flush:true)
		def orgGrandChild = Org.newInstance(name:"orggrand",parent: orga)
		assertNotNull "should have saved",orgGrandChild.save(flush:true)

		session.clear()
		
		assertEquals 4, Org.list().size()
		
		session.clear()
		def o = Org.get(1)
		assertEquals 2, o.children.size()
		def oa = Org.findByName("orga")
		assertEquals 1, oa.children.size()
		
		def ogrand = Org.findByName("orggrand")
		assertEquals 0, ogrand.children.size()
		assertEquals oa, ogrand.parent

    }

	void testOneToManyExt() {

        def ExtOrg = ga.getDomainClass("ExtOrg").clazz

		def org1 = ExtOrg.newInstance(name:"org1")
		assertNotNull "should have saved",org1.save(flush:true)
		def orga = ExtOrg.newInstance(name:"orga",parent: org1)
		assertNotNull "should have saved",orga.save(flush:true)
		def orgb = ExtOrg.newInstance(name:"orgb",parent: org1)
		assertNotNull "should have saved",orgb.save(flush:true)
		def orgGrandChild = ExtOrg.newInstance(name:"orggrand",parent: orga)
		assertNotNull "should have saved",orgGrandChild.save(flush:true)

		session.clear()

		assertEquals 4, ExtOrg.list().size()

		session.clear()
		def o = ExtOrg.get(1)
		assertEquals 2, o.children.size()
		def oa = ExtOrg.findByName("orga")
		assertEquals 1, oa.children.size()

		def ogrand = ExtOrg.findByName("orggrand")
		assertEquals 0, ogrand.children.size()
		assertEquals oa, ogrand.parent

   }
	

}