package org.codehaus.groovy.grails.orm.hibernate;


class InheritanceQueryingTests extends AbstractGrailsHibernateTests {

	void testPolymorphicQuerying() {
		def cityClass = ga.getDomainClass("City")
		def countryClass = ga.getDomainClass("Country")
		def locationClass = ga.getDomainClass("Location")
		
		def city = cityClass.newInstance()
		city.properties = [code: "LON", name: "London", longitude: 49.1, latitude:
			53.1]
		def location = locationClass.newInstance()
		location.properties = [code: "XX", name: "The World"]
		def country = countryClass.newInstance()
		country.properties = [code: "UK", name: "United Kingdom", population: 10000000]
		                      
		country.save(true)
		city.save(true)
		location.save(true)
		
		assertEquals 1, cityClass.clazz.findAll().size() 
		assertEquals 1, countryClass.clazz.findAll().size() 
		assertEquals 3, locationClass.clazz.findAll().size() 
	
	}
	void onSetUp() {
		this.gcl.parseClass('''
class City extends Location {
    BigDecimal latitude
    BigDecimal longitude
}         
class Country extends Location {
    int population
}	
class Location {
	Long id
	Long version
    String name
    String code
}	

'''
		)
	}	
	
	void onTearDown() {
		
	}
}
