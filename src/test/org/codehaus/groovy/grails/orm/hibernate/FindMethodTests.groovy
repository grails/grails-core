package org.codehaus.groovy.grails.orm.hibernate;

import groovy.util.GroovyTestCase;

class FindMethodTests extends AbstractGrailsHibernateTests {
	
	void onSetUp() {
		gcl.parseClass('''
import grails.persistence.*

@Entity
class FindMethodTestClass {
	String one
	Integer two
}

''')
		
	}
	
	void testFindMethodWithHQL() {
		def domain = ga.getDomainClass("FindMethodTestClass").clazz
		
		assert domain.newInstance(one:"one", two:2).save(flush:true) : "should have saved"
		
		session.clear()
		
		assert domain.find("from FindMethodTestClass as f where f.one = ? and f.two = ?", ["one", 2]) : "should have returned a result"
	}
}