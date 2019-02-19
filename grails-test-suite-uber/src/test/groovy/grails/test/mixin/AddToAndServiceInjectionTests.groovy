package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DataTest
import spock.lang.Specification

class AddToAndServiceInjectionTests extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {
        [Ship, Pirate]
    }

    void testAddTo() {
        given:
        def pirate = new Pirate(name: 'Billy')
        def ship = new Ship()

        when:
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
