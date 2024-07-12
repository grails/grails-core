package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import org.junit.Test
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @author Rob Fletcher
 * @since 1.3.0
 */
@Ignore('grails-gsp is not on jakarta.servlet yet')
class CheckboxBindingTests extends Specification implements ControllerUnitTest<CheckboxBindingController>, DomainUnitTest<Pizza> {

    void testBindingCheckedValuesToObject() {
        given:
        params.name = "Capricciosa"
        params."_delivery" = ""
        params."delivery" = "on"
        params."options._extraAnchovies" = ""
        params."options.extraAnchovies" = "on"
        params."options._stuffedCrust" = ""
        params."options.stuffedCrust" = "on"

        when:
        def model = controller.save()

        then:
        model.pizza.name == "Capricciosa"
        model.pizza.delivery
        model.pizza.options.extraAnchovies
        model.pizza.options.stuffedCrust
    }

    void testBindingUncheckedValuesToObject() {
        given:
        params.name = "Capricciosa"
        params."_delivery" = ""
        params.options = [_extraAnchovies: '', _stuffedCrust: '']

        when:
        def model = controller.save()

        then:
        model.pizza.name == "Capricciosa"
        !model.pizza.delivery
        !model.pizza.options.extraAnchovies
        !model.pizza.options.stuffedCrust
    }
}

@Entity
class Pizza {
    String name
    boolean delivery = true
    Options options = new Options()
    static embedded = ["options"]
}

class Options {
    boolean extraAnchovies = true
    boolean stuffedCrust = true
}

@Artefact('Controller')
class CheckboxBindingController {
    def save = {
        def p = new Pizza(params)
        [pizza: p]
    }
}
