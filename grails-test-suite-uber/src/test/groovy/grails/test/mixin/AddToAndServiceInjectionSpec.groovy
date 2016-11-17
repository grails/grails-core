package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DataTest
import spock.lang.Specification

class AddToAndServiceInjectionSpec extends Specification implements DataTest {

    void setupSpec() {
        mockDomains Pirate, Ship
    }

    void 'test addTo'() {
        when:
        def pirate = new Pirate(name: 'Billy')
        def ship = new Ship()
        ship.addToPirates(pirate)

        then:
        1 == ship.pirates.size()
    }
}

@Entity
class Pirate {
    String name
    def pirateShipService
}


@Entity
class Ship {
    static hasMany = [pirates: Pirate]
}
