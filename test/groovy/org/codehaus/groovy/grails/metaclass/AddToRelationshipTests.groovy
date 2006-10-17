package org.codehaus.groovy.grails.metaclass;

import org.codehaus.groovy.grails.orm.hibernate.*

class AddToRelationshipTests extends AbstractGrailsHibernateTests {

	void testAddToRelationship() {
		def testClass = ga.getGrailsDomainClass("Test")
		def otherClass = ga.getGrailsDomainClass("Other")
		def t = testClass.newInstance()		
		def o1 = otherClass.newInstance()
		o1.name = "test1"
		def o2 = otherClass.newInstance()
		o2.name = "test2"
		
		
		t.add(to:'others', o1)
		t.add(to:'moreOthers', o2)
		
		assert t.others.contains(o1)
		assert t.moreOthers.contains(o2)
		
	}

	void onSetUp() {
		this.gcl.parseClass('''
class Test {
	Long id
	Long version
	Set others
	Set moreOthers
	def hasMany = [others:Other, moreOthers:Other]
}
class Other {
	Long id
	Long version
	String name
}
'''
		)
	}
	
	void onTearDown() {
		
	}


}
