package org.codehaus.groovy.grails.orm.hibernate;

class ManyToManyWithInheritanceTests extends AbstractGrailsHibernateTests {

	@Override
	protected void onSetUp() {
		gcl.parseClass '''
import grails.persistence.*

@Entity
class Pentagon extends Shape{

}
@Entity
class Shape {       
	static hasMany = [attributes:ShapeAttribute]
}
@Entity
class ShapeAttribute{

	String name
	String value
	
	static hasMany = [shapes: Shape]
	static belongsTo = Shape

}
'''
	}
	
	void testManyToManyMappingWithInheritance() {
		def Pentagon = ga.getDomainClass("Pentagon").clazz
		def ShapeAttribute = ga.getDomainClass("ShapeAttribute").clazz
		
		def p = Pentagon.newInstance()
		p.addToAttributes(name:"sides", value:"5")
		
		assert p.save(flush:true) : "should have saved instance"
			
		session.clear()
		
		p = Pentagon.get(1)
		
		assert 1 == p.attributes.size() : "should have one attribute"
	}
}
