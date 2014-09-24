package org.grails.web.binding

import static org.junit.Assert.assertEquals
import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.TestFor

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@TestFor(StatusController)
class EnumBindingTests  {

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
