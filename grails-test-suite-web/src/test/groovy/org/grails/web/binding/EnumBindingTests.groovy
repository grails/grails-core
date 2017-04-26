package org.grails.web.binding

import org.grails.core.support.MappingContextBuilder
import spock.lang.Specification
import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.TestFor

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@TestFor(StatusController)
class EnumBindingTests extends Specification {

    void setupSpec() {
        new MappingContextBuilder(grailsApplication).build(StatusTransition)
    }


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
