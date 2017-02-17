package org.grails.web.binding

import grails.testing.web.controllers.ControllerUnitTest

import static org.junit.Assert.assertEquals
import grails.artefact.Artefact
import grails.persistence.Entity

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class EnumBindingTests implements ControllerUnitTest<StatusController> {

    @Test
    void testBindEnumInConstructor() {
        def model = controller.bindMe()

        assertEquals "blah", model.statusTransition.title
        assertEquals "OPEN", model.statusTransition.status.toString()
    }
}
@Entity
class StatusTransition {
    String title
    Status status
}
enum Status {
    OPEN, IN_PROGRESS, ON_HOLD, DONE
}

@Artefact('Controller')
class StatusController {
    def bindMe = {
        [statusTransition:new StatusTransition(title:"blah", status:Status.OPEN)]
    }
}
