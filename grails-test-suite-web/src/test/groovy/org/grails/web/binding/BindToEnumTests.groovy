package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Ignore
import spock.lang.Specification


/**
 * @author Graeme Rocher
 * @since 1.1
 */
@Ignore('grails-gsp is not on jakarta.servlet yet')
class BindToEnumTests extends Specification implements ControllerUnitTest<EnumBindingController>, DomainUnitTest<RoleHolder> {

    void testBindBlankValueToEnum() {
        params.role = ""

        when:
        def model = controller.save()

        then:
        model.holder.role == null
    }

    void testBindValueToEnum() {
        params.role = "USER"

        when:
        def model = controller.save()

        then:
        model.holder.role.toString() == "USER"
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

