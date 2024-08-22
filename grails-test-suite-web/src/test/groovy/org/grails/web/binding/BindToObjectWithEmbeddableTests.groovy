package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BindToObjectWithEmbeddableTests extends Specification implements ControllerUnitTest<EmbeddedAddressController>, DomainUnitTest<EmbeddedAddressPerson> {

    void testBindToObjectWithEmbedded() {
        params.name = "Joe"
        params.age= 45
        params.address = [city: 'Brighton']

        when:
        def model = controller.save()

        then:
        model.person.name == "Joe"
        model.person.age == 45
        model.person.address.city == "Brighton"
    }
}


@Entity
class EmbeddedAddressPerson {

    static embedded = ['address']

    String name
    int age
    EmbeddedAddress address = new EmbeddedAddress()
}

class EmbeddedAddress {
    String street
    String street2
    String city
    String state
    String zip

    static constraints = {
        street2(nullable:true)
    }
}

@Artefact('Controller')
class EmbeddedAddressController {

    def save = {
        def p = new EmbeddedAddressPerson(params)
        [person:p]
    }
}