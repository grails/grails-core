package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*

class ComponentDomainTests extends AbstractGrailsMockTests {

	void testComponentDomain() {
		def personClass = ga.getDomainClass("Person")
		//def bookClass = ga.getDomainClass("Address")


        def homeAddress = personClass.getPropertyByName("homeAddress")
        def workAddress = personClass.getPropertyByName("workAddress")
        assert homeAddress.isEmbedded()
        assert workAddress.isEmbedded()

        assert homeAddress.referencedDomainClass
        assertEquals "Address",homeAddress.referencedDomainClass.name

	}

	void onSetUp() {
		this.gcl.parseClass('''
class Person {
	Long id
	Long version
	String name
	Address homeAddress
	Address workAddress

	static embedded = ['homeAddress', 'workAddress']
}
class Address {
	Long id
	Long version
	String number
	String postCode
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
