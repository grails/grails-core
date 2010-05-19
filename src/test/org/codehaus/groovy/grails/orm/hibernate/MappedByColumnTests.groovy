package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.*

class MappedByColumnTests extends AbstractGrailsHibernateTests {

    void testByConvention() {
        def airportClass = ga.getDomainClass("Airport")
        def routeClass = ga.getDomainClass("Route")

        def a = airportClass.newInstance()

        a.save()

        def r = routeClass.newInstance()
        a.addToRoutes(r)

        a.save()

        assertEquals 1, a.routes.size()
        assertEquals a, r.airport

        assertNull r.destination
    }

    void testOtherPropertyWithConvention() {
        def airportClass = ga.getDomainClass("Airport")
        def routeClass = ga.getDomainClass("Route")

        def a = airportClass.newInstance()

        a.save()

        def r = routeClass.newInstance()
        r.destination = a

        r.save()

        assertNotNull r.destination.id
    }

    void onSetUp() {
        gcl.parseClass '''
class Airport {
    Long id
    Long version
    Set routes

    static hasMany = [routes:Route]
}

class Route {
    Long id
    Long version

    Airport airport
    Airport destination

    static constraints = {
        airport(nullable:true)
        destination(nullable:true)
    }
}
'''
    }
}
