package org.grails.web.binding

import static org.junit.Assert.*
import grails.artefact.Artefact
import grails.persistence.Entity
import grails.test.mixin.TestFor

import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@TestFor(NullBindingPersonController)
class BindingToNullableTests {

    @Test
    void testDataBindingBlankStringToNull() {
        controller.params.name = "fred"
        controller.params.dateOfBirth = ''

        def model = controller.update()

        assertNotNull "should have redirected with no validation error",controller.response.redirectedUrl
    }

    @Test
    void testDataBindingToNull() {
        controller.params.name = "fred"
        controller.params.dateOfBirth = 'invalid'

        def model = controller.update()

        if (controller.response.redirectedUrl) {
            fail "Request should not have been redirected as there should be errors, but was redirected to $controller.response.redirectedUrl"
        }

        def person = model.personInstance
        assertEquals "fred", person.name
        assertTrue person.hasErrors()
        assertEquals("typeMismatch", person.errors.getFieldError("dateOfBirth").code)
    }
}

@Entity
class NullBindingPerson {
    String name
    Date dateOfBirth

    static constraints = {
        dateOfBirth nullable: true
    }
}

@Artefact('Controller')
class NullBindingPersonController {

    def update = {
        def p = new NullBindingPerson()
        p.properties = params
        if (p.hasErrors()) {
            [personInstance:p]
        }
        else {
            redirect action:"foo"
        }
    }
}

