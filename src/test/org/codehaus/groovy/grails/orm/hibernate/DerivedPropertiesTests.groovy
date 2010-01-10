package org.codehaus.groovy.grails.orm.hibernate

class DerivedPropertiesTests extends AbstractGrailsHibernateTests {
	
	public void onSetUp() {
		gcl.parseClass '''
import grails.persistence.Entity
@Entity
class Product {
	Integer price
	Integer finalPrice
	static mapping = {
	    finalPrice formula: 'PRICE * 3'
	}
}
'''
	}
	
	void testDerivedPropertyValues() {
		def productClass = ga.getDomainClass('Product').clazz
		
		[10, 20, 30].each { price ->
		    def product = productClass.newInstance()
			product.price = price
			assertNotNull 'saving product failed', product.save(flush: true)
		}
		session.clear()
		
		assertEquals 30, productClass.findByPrice(10)?.finalPrice
		assertEquals 60, productClass.findByPrice(20)?.finalPrice
		assertEquals 90, productClass.findByPrice(30)?.finalPrice
	}
	
	void testQueryWithDynamicFinders() {
		def productClass = ga.getDomainClass('Product').clazz
		
		[10, 20, 30].each { price ->
			def product = productClass.newInstance()
			product.price = price
			assertNotNull 'saving product failed', product.save(flush: true)
		}
		session.clear()

		def cnt = productClass.count()
		assertEquals 3, cnt
		
		cnt = productClass.countByFinalPrice(60)
		assertEquals 1, cnt
		
		cnt = productClass.countByFinalPriceGreaterThan(50)
		assertEquals 2, cnt
	}
	
	void testQueryWithCriteria() {
		def productClass = ga.getDomainClass('Product').clazz
		
		[10, 20, 30].each { price ->
		def product = productClass.newInstance()
		product.price = price
		assertNotNull 'saving product failed', product.save(flush: true)
		}
		session.clear()
		
		def cnt = productClass.createCriteria().count {
			eq 'finalPrice', 60
		}
		assertEquals 1, cnt
		
		cnt = productClass.createCriteria().count {
			gt 'finalPrice', 50
		}
		assertEquals 2, cnt
	}
	
	void testNullabilityOfDerivedPropertiesSurvivesRefreshConstraints() {
		def productDomainClass = ga.getDomainClass('Product')
		def productClass = productDomainClass.clazz
		
		def product = productClass.newInstance()
		product.price = 40
		
		assertTrue 'validation should have passed before refreshing constraints', product.validate()
		
		productDomainClass.refreshConstraints()
		assertTrue 'validation should have passed after refreshing constraints', product.validate()
	}
}