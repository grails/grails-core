package org.codehaus.groovy.grails.orm.hibernate;


class InheritanceQueryingTests extends AbstractGrailsHibernateTests {

	void testPolymorphicQuerying() {
		def cityClass = ga.getDomainClass("InheritanceQueryingCity")
		def countryClass = ga.getDomainClass("InheritanceQueryingCountry")
		def locationClass = ga.getDomainClass("InheritanceQueryingLocation")
		
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
class InheritanceQueryingCity extends InheritanceQueryingLocation {
    BigDecimal latitude
    BigDecimal longitude
}         
class InheritanceQueryingCountry extends InheritanceQueryingLocation {
    int population
}	
class InheritanceQueryingLocation extends InheritanceQueryingVersioned{
    String name
    String code
}	
abstract class InheritanceQueryingVersioned {
	Long id
	Long version
}
class ApplicationDataSource {
   boolean pooling = true
   String dbCreate = "create-drop" // one of 'create', 'create-drop','update'
   String url = "jdbc:hsqldb:mem:devDB"
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
