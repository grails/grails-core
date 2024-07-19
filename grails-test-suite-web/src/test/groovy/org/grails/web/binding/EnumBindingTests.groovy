package org.grails.web.binding

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Ignore
import spock.lang.Specification
import grails.artefact.Artefact
import grails.persistence.Entity

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@Ignore('grails-gsp is not on jakarta.servlet yet')
class EnumBindingTests extends Specification implements ControllerUnitTest<StatusController>, DomainUnitTest<StatusTransition> {

    void testBindEnumInConstructor() {
        when:
        def model = controller.bindMe()

        then:
        model.statusTransition.title == "blah"
        model.statusTransition.status.toString() == "OPEN"
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
