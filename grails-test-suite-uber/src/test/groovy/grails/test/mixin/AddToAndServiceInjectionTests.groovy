package grails.test.mixin

import spock.lang.Specification
import grails.persistence.Entity
import org.junit.Test
import org.junit.Ignore

/**
 */

@TestFor(Ship)
@Mock(Pirate)
class AddToAndServiceInjectionTests {

    @Test
    @Ignore // TODO: remove when upgrading to datastore 1.0.9
    void testAddTo() {
        def pirate = new Pirate(name: 'Billy')
        def ship = new Ship()
        ship.addToPirates(pirate)
        assert 1 == ship.pirates.size()
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
