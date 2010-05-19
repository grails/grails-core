package org.codehaus.groovy.grails.orm.hibernate

class InheritanceQueryingTests extends AbstractGrailsHibernateTests {

    void testPolymorphicQuerying() {
        def cityClass = ga.getDomainClass("InheritanceQueryingCity")
        def countryClass = ga.getDomainClass("InheritanceQueryingCountry")
        def locationClass = ga.getDomainClass("InheritanceQueryingLocation")

        def city = cityClass.newInstance()
        city.properties = [code: "LON", name: "London", longitude: 49.1, latitude: 53.1]
        def location = locationClass.newInstance()
        location.properties = [code: "XX", name: "The World"]
        def country = countryClass.newInstance()
        country.properties = [code: "UK", name: "United Kingdom", population: 10000000]

        country.save()
        city.save()
        location.save()

        assertEquals 1, cityClass.clazz.findAll().size()
        assertEquals 1, countryClass.clazz.findAll().size()
        assertEquals 3, locationClass.clazz.findAll().size()
    }

    protected void onSetUp() {
        gcl.parseClass '''
class InheritanceQueryingCity extends InheritanceQueryingLocation {
    BigDecimal latitude
    BigDecimal longitude
}

class InheritanceQueryingCountry extends InheritanceQueryingLocation {
    int population
}

class InheritanceQueryingLocation extends InheritanceQueryingVersioned {
    String name
    String code
}

abstract class InheritanceQueryingVersioned {
    Long id
    Long version
}
'''
    }
}
