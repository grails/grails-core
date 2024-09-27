package grails.test.mixin

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.persistence.Entity
import grails.artefact.Artefact
import spock.lang.Specification

class UnitTestDataBindingAssociatonTests extends Specification implements ControllerUnitTest<ShipController>, DataTest {

    void setupSpec() {
        mockDomains Ship2, Pirate2
    }

    void testBindingAssociationInUnitTest() {
        when:
        def pirate = new Pirate2(name: 'Joe')
        pirate.save(failOnError: true, validate: false)

        def ship = new Ship2(pirate: pirate).save(failOnError: true, validate: false)

        // TODO: This currently doesn't work. See GRAILS-9120
//        params."pirate" = [id: pirate.id, name: 'new name']
        params."pirate.id" = pirate.id
        params."pirate" = [name:'new name']
        params.id = ship.id
        controller.pirate()

        then:
        'new name' == ship.pirate.name
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
