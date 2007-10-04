package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*

class ComponentMappingTests extends AbstractGrailsHibernateTests {

	void testComponentMapping() {
		def personClass = ga.getDomainClass("Person")
		def addressClass = ga.getDomainClass("Address")

		def p = personClass.newInstance()

		p.name = "Homer Simpson"
		def a1 = addressClass.newInstance()
		a1.number = "22"; a1.postCode = "3345243"
		def a2 = addressClass.newInstance()
		a2.number = "454"; a2.postCode = "340854"

		p.homeAddress = a1
		p.workAddress = a2

        p.save()
        session.flush()

        session.clear()

        p = personClass.clazz.get(1)

        assert p
        assert p.homeAddress
        assert p.workAddress

        assertEquals "22", p.homeAddress.number
        assertEquals "3345243", p.homeAddress.postCode
        assertEquals "454", p.workAddress.number
        assertEquals "340854", p.workAddress.postCode

        assertEquals "Homer Simpson", p.workAddress.person.name


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
	Person person
	String number
	String postCode
}
class StoreItem {
	Long id
	Long version

    String name
    String description
    Price price
    static embedded = ['price']
}

class Price {
	Long id
	Long version
 
    BigDecimal amount
    Integer quantity
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
