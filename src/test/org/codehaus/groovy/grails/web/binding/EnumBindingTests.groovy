package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class EnumBindingTests extends AbstractGrailsControllerTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

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
''')
    }


    void testBindEnumInConstructor() {
        def ctrl = ga.getControllerClass("StatusController").newInstance()

        def model = ctrl.bindMe()

        assertEquals "blah", model.statusTransition.title
        assertEquals "OPEN", model.statusTransition.status.toString()
    }
}