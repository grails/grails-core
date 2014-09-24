package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.TestFor

import org.junit.Test
import static org.junit.Assert.assertEquals

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@TestFor(EmbeddedAddressController)
class BindToObjectWithEmbeddableTests {

    @Test
    void testBindToObjectWithEmbedded() {
        params.name = "Joe"
        params.age= "45"
        params.address = [city: 'Brighton']

        def model = controller.save()

        assertEquals "Joe", model.person.name
        assertEquals 45, model.person.age
        assertEquals "Brighton", model.person.address.city
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