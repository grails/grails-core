package grails.test.mixin

import org.junit.Test
import grails.persistence.Entity
import grails.artefact.Artefact

/**
 */
@TestFor(ShipController)
@Mock([Ship2, Pirate2])
class UnitTestDataBindingAssociatonTests {
    @Test
    void testBindingAssociationInUnitTest() {
        def pirate = new Pirate2(name: 'Joe')
        pirate.save(failOnError: true, validate: false)

        def ship = new Ship2(pirate: pirate).save(failOnError: true, validate: false)

        // TODO: This currently doesn't work. See GRAILS-9120
//        params."pirate" = [id: pirate.id, name: 'new name']
        params."pirate.id" = pirate.id
        params."pirate" = [name:'new name']
        params.id = ship.id
        controller.pirate()

        assert  'new name' == ship.pirate.name
    }
}

@Entity
class Pirate2 {
    String name
}
@Entity
class Ship2 {
    Pirate2 pirate
}

@Artefact("Controller")
class ShipController {
    def pirate ={
        def shipInstance = Ship2.get(params.id)
        shipInstance.properties = params

        assert 'new name' == shipInstance.pirate.name
        render text: 'I am done'
    }
}
