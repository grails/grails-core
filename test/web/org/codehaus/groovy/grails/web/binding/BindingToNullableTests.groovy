package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 11, 2009
 */

public class BindingToNullableTests extends AbstractGrailsControllerTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Person {
	String name
	Date dateOfBirth

	static constraints = {
		dateOfBirth nullable: true
	}
}

class PersonController {

    def update = {
        def p = new Person()
        p.properties = params
        if(p.hasErrors()) {
            [personInstance:p]
        }
        else {
            redirect action:"foo"
        }
    }
}
''')
    }


    void testDataBindingBlankStringToNull() {
        def controller = ga.getControllerClass("PersonController").newInstance()

        controller.params.name = "fred"
        controller.params.dateOfBirth = ''

        def model = controller.update()

        assertNotNull "should have redirected with no validation error",controller.response.redirectedUrl

    }
    void testDataBindingToNull() {
        def controller = ga.getControllerClass("PersonController").newInstance()

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