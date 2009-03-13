package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*

class ComponentMappingTests extends AbstractGrailsHibernateTests {       

	void testComponentMapping() {
		def personClass = ga.getDomainClass("ComponentMappingPerson")
		def addressClass = ga.classLoader.loadClass("ComponentMappingAddress")

		def p = personClass.newInstance()

		p.name = "Homer Simpson"
		def a1 = addressClass.newInstance()
		a1.number = "22"; a1.postCode = "3345243" ; a1.person = p
		def a2 = addressClass.newInstance()
		a2.number = "454"; a2.postCode = "340854" ; a2.person = p

		p.homeAddress = a1
		p.workAddress = a2
                                                                                                        
        assertNotNull "should have saved the person", p.save()
        session.flush()

        session.clear()

        p = personClass.clazz.get(1)

        assertNotNull "should have persisted the person", p
        assertNotNull "should have persisted the home address", p.homeAddress
        assertNotNull "should have persisted the work address", p.workAddress

        assertEquals "22", p.homeAddress.number
        assertEquals "3345243", p.homeAddress.postCode
        assertEquals "454", p.workAddress.number
        assertEquals "340854", p.workAddress.postCode

        assertEquals "Homer Simpson", p.workAddress.person.name


	}

    void testNullableComponent() {
        def personClass = ga.getDomainClass("ComponentMappingPerson")
        def addressClass = ga.classLoader.loadClass("ComponentMappingAddress")

        def p = personClass.newInstance()

        p.name = "Homer Simpson"
        def a1 = addressClass.newInstance()
        a1.number = "22"; a1.postCode = "3345243" ; a1.person = p

        p.homeAddress = a1

        assertNotNull "should have saved the person", p.save()
        session.flush()

        session.clear()

        p = personClass.clazz.get(1)

        assertNotNull "should have persisted the person", p
        assertNotNull "should have persisted the home address", p.homeAddress
        assertNull "should not have persisted the work address", p.workAddress

        assertEquals "22", p.homeAddress.number
        assertEquals "3345243", p.homeAddress.postCode
        
    }

	void onSetUp() {
		this.gcl.parseClass('''
class ComponentMappingPerson {
	Long id
	Long version
	String name
	ComponentMappingAddress homeAddress
	ComponentMappingAddress workAddress

	static embedded = ['homeAddress', 'workAddress']
    static constraints = {
        workAddress nullable:true
    }
}
class ComponentMappingAddress {
	ComponentMappingPerson person
	String number
	String postCode
}
class ComponentMappingStoreItem {
	Long id
	Long version

    String name
    String description
    ComponentMappingPrice price
    static embedded = ['price']
}

class ComponentMappingPrice {
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
