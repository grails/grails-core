package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.TestFor

import org.junit.Test
import static org.junit.Assert.*

/**
 * @author Rob Fletcher
 * @since 1.3.0
 */
@TestFor(CheckboxBindingController)
class CheckboxBindingTests {

    @Test
    void testBindingCheckedValuesToObject() {
        params.name = "Capricciosa"
        params."_delivery" = ""
        params."delivery" = "on"
        params."options._extraAnchovies" = ""
        params."options.extraAnchovies" = "on"
        params."options._stuffedCrust" = ""
        params."options.stuffedCrust" = "on"

        def model = controller.save()

        assertEquals "Capricciosa", model.pizza.name
        assertTrue "checked value 'delivery' failed to bind", model.pizza.delivery
        assertTrue "nested checked value 'options.extraAnchovies' failed to bind", model.pizza.options.extraAnchovies
        assertTrue "nested checked value 'options.stuffedCrust' failed to bind", model.pizza.options.stuffedCrust

    }

    @Test
    void testBindingUncheckedValuesToObject() {
        params.name = "Capricciosa"
        params."_delivery" = ""
        params.options = [_extraAnchovies: '', _stuffedCrust: '']

        def model = controller.save()

        assertEquals "Capricciosa", model.pizza.name
        assertFalse "unchecked value 'delivery' failed to bind", model.pizza.delivery
        assertFalse "nested unchecked value 'options.extraAnchovies' failed to bind", model.pizza.options.extraAnchovies
        assertFalse "nested unchecked value 'options.stuffedCrust' failed to bind", model.pizza.options.stuffedCrust

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
