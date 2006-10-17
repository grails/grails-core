package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class ManyToManyTests extends AbstractGrailsHibernateTests {

	void testManyToManyDomain() {
		def testDomain = ga.getGrailsDomainClass("Test")
		def otherDomain = ga.getGrailsDomainClass("Other")

		
		def others = testDomain?.getPropertyByName("others")
		def tests = otherDomain?.getPropertyByName("tests")
				
		assert others?.isManyToMany()
		assert tests?.isManyToMany()
		assert !others?.isOneToMany()
		assert !tests?.isOneToMany()				
	}
	void testManyToManyMapping() {
		def testClass = ga.getGrailsDomainClass("Test")
		def otherClass = ga.getGrailsDomainClass("Other")
		def t = testClass.newInstance()
		
		t.addOther(otherClass.newInstance())
		t.save(true)
		
		t = null
		
		assertEquals 1, otherClass.clazz.list().size()
		
		def o = otherClass.clazz.get(1)
		assert o
		assert o.tests
		
		t = testClass.clazz.get(1)
		assert t
		assert t.others
		
		assertEquals o, t.others.find { it.id == 1}

	}

	void onSetUp() {
		this.gcl.parseClass('''
class Test {
	Long id
	Long version
	Set others
	def hasMany = [others:Other]
}
class Other {
	Long id
	Long version
	Set tests
	def belongsTo = Test
	def hasMany = [tests:Test]
}
class ApplicationDataSource {
	   boolean pooling = true
	   boolean logSql = true
	   String dbCreate = "create-drop" // one of 'create', 'create-drop','update'
	   String url = "jdbc:hsqldb:mem:testDB"
	   String driverClassName = "org.hsqldb.jdbcDriver"
	   String username = "sa"
	   String password = ""  
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
