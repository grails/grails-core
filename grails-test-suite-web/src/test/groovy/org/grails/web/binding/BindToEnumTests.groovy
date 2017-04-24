package org.grails.web.binding

import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.TestFor
import org.grails.core.support.MappingContextBuilder
import spock.lang.Specification


/**
 * @author Graeme Rocher
 * @since 1.1
 */
@TestFor(EnumBindingController)
class BindToEnumTests extends Specification {

    void setupSpec() {
        new MappingContextBuilder(grailsApplication).build(RoleHolder)
    }

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

