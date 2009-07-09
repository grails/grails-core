package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class MappedByColumn2Tests extends AbstractGrailsHibernateTests {

	void testWithConfig() {
		def airportClass = ga.getDomainClass("Airport")
		def routeClass = ga.getDomainClass("Route")
		
		def a = airportClass.newInstance()
		
		a.save(true)
		
		def r = routeClass.newInstance()
		a.addToRoutes(r)
		
		a.save(true)
		
		assertEquals 1, a.routes.size()
		assertEquals a, r.destination
		
		assertNull r.airport
		
	}
	
	void onSetUp() {
		this.gcl.parseClass('''
class Airport {
	Long id
	Long version
	Set routes

	static mappedBy = [routes:'destination']
	static hasMany = [routes:Route]
}
class Route {
	Long id
	Long version
	
	Airport airport
	Airport destination

    static constraints = {
        airport nullable:true
    }
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
