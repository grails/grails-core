package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class BindToEnumTests extends AbstractGrailsControllerTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class RoleHolder {

    EnumRole role

    static constraints = {
        role nullable:true
    }
}

enum EnumRole {
    USER, ADMINISTRATOR, EDITOR
}

class EnumBindingController {

    def save = {
        def h = new RoleHolder(params)
        [holder:h]
    }
}

''')
    }


    void testBindBlankValueToEnum() {
        def controller = ga.getControllerClass("EnumBindingController").newInstance()

        controller.params.role = ""

        def model = controller.save()

        assertNull "should have been null", model.holder.role
    }

    void testBindValueToEnum() {
        def controller = ga.getControllerClass("EnumBindingController").newInstance()

        controller.params.role = "USER"

        def model = controller.save()

        assertEquals "USER", model.holder.role.toString()

    }

}