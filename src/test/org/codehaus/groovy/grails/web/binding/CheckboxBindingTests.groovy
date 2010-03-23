package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Rob Fletcher
 * @since 1.3.0
 */
class CheckboxBindingTests extends AbstractGrailsControllerTests {
    protected void onSetUp() {
        gcl.parseClass """
import grails.persistence.*

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

class CheckboxBindingController {
    def save = {
        def p = new Pizza(params)
        [pizza: p]
    }
}

"""
    }

    void testBindingCheckedValuesToObject() {
        def controller = ga.getControllerClass("CheckboxBindingController").newInstance()

        controller.params.name = "Capricciosa"
        controller.params."_delivery" = ""
        controller.params."delivery" = "on"
        controller.params."options._extraAnchovies" = ""
        controller.params."options.extraAnchovies" = "on"
        controller.params."options._stuffedCrust" = ""
        controller.params."options.stuffedCrust" = "on"

        def model = controller.save()

        assertEquals "Capricciosa", model.pizza.name
        assertTrue "checked value 'delivery' failed to bind", model.pizza.delivery
        assertTrue "nested checked value 'options.extraAnchovies' failed to bind", model.pizza.options.extraAnchovies
        assertTrue "nested checked value 'options.stuffedCrust' failed to bind", model.pizza.options.stuffedCrust

    }

    void testBindingUncheckedValuesToObject() {
        def controller = ga.getControllerClass("CheckboxBindingController").newInstance()

        controller.params.name = "Capricciosa"
        controller.params."_delivery" = ""
        controller.params."options._extraAnchovies" = ""
        controller.params."options._stuffedCrust" = ""

        def model = controller.save()

        assertEquals "Capricciosa", model.pizza.name
        assertFalse "unchecked value 'delivery' failed to bind", model.pizza.delivery
        assertFalse "nested unchecked value 'options.extraAnchovies' failed to bind", model.pizza.options.extraAnchovies
        assertFalse "nested unchecked value 'options.stuffedCrust' failed to bind", model.pizza.options.stuffedCrust

    }

}
