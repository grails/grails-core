package org.codehaus.groovy.grails.web.binding

import grails.persistence.Entity

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class EnumBindingTests extends AbstractGrailsControllerTests {

    @Override
    protected Collection<Class> getDomainClasses() {
        [StatusTransition]
    }

    @Override
    protected Collection<Class> getControllerClasses() {
        [StatusController]
    }

    void testBindEnumInConstructor() {
        def ctrl = new StatusController()
        def model = ctrl.bindMe()

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

class StatusController {
    def bindMe = {
        [statusTransition:new StatusTransition(title:"blah", status:Status.OPEN)]
    }
}
