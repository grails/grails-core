package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.TestFor
import spock.lang.Specification
import org.grails.core.support.MappingContextBuilder

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@TestFor(EmbeddedAddressController)
class BindToObjectWithEmbeddableTests extends Specification {

    void setupSpec() {
        new MappingContextBuilder(grailsApplication).build(EmbeddedAddressPerson)
    }

    void testBindToObjectWithEmbedded() {
        params.name = "Joe"
        params.age= "45"
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