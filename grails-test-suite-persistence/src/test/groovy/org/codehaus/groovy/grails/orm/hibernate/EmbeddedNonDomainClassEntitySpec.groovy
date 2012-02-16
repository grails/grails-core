package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 *
 */
class EmbeddedNonDomainClassEntitySpec extends GormSpec{

    void "Test that a non-domain class be embedded"() {
        given:"A domain class with an embedded class is created"
            def p = new Place(name:"London", address:new PlaceAddress(street: "Oxford St", postcode: "DFSDF11"))
            p.save(flush:true)
            session.clear()

        when:"The domain is queried"
            p = Place.get(p.id)

        then:"The embedded instance can be retrieved"
            p.address != null
            p.address.street == "Oxford St"
    }
    @Override
    List getDomainClasses() {
        [Place]
    }
}


/**
 * a business place for displaying on map
 */
@Entity
class Place {
    PlaceAddress address
    String name
    static embedded = ['address']
}

@Entity
class PlaceAddress {
    String street
    String postcode
}
