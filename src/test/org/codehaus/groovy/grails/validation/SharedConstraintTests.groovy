package org.codehaus.groovy.grails.validation

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests;

class SharedConstraintTests extends AbstractGrailsHibernateTests{

	@Override
	protected void onSetUp() {
		gcl.parseClass '''
grails.gorm.default.constraints = {
		amount(nullable: false, scale: 2, min: new BigDecimal(0), max: new BigDecimal('999999999999.99'))
}		
		''', 'Config'
		
		gcl.parseClass '''
import grails.persistence.*
		
@Entity
class Line {
	Date date
	BigDecimal amount

	static constraints = {
	amount(shared:'amount')
	}
}		
		'''
	}
	
	
	void testSharedConstraints() {
		def Line = ga.getDomainClass("Line").clazz
		
		def l = Line.newInstance()
		
		l.date = new Date()
		l.amount  = 1.1
		
		assert l.validate() : "instance should have validated!"
	}
}

