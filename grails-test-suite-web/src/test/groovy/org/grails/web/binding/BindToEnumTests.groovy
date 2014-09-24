package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.TestFor

import org.junit.Test
import static org.junit.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
@TestFor(EnumBindingController)
class BindToEnumTests {

    @Test
    void testBindBlankValueToEnum() {
        params.role = ""

        def model = controller.save()

        assertNull "should have been null", model.holder.role
    }

    @Test
    void testBindValueToEnum() {
        params.role = "USER"

        def model = controller.save()

        assertEquals "USER", model.holder.role.toString()
    }
}

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

@Artefact('Controller')
class EnumBindingController {

    def save = {
        def h = new RoleHolder(params)
        [holder:h]
    }
}

