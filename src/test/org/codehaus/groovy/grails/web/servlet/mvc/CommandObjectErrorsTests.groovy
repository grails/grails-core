/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 28, 2007
 */
package org.codehaus.groovy.grails.web.servlet.mvc

import org.springframework.validation.Errors

class CommandObjectErrorsTests extends AbstractGrailsControllerTests{

    public void onSetUp() {
        gcl.parseClass '''
class TestController {

    def index = { Form form ->
        [formErrors:form.hasErrors()]
    }

    def two = { Form form ->
        [formErrors:form.errors]
    }

    def three = { Form form ->
        [form:form]
    }

    def four = { Form form ->
         form.validate()
        [formErrors:form.errors]
    }

    def five = { Form form ->
		[form: new Form()]
    }

    def validate = { Form form ->

        [formErrors:form.validate()]
    }
}
class Form {
    String input
    URL url

    static constraints = {
        input(size:5..10, nullable:false)
    }
}
'''
    }

	void testCommandObjectsDontShareErrors() {
		def controller = ga.getControllerClass("TestController").newInstance()

		def model = controller.five()

        def error
		def form = model.form
		assertNotNull 'did not find expected form', form
		assertFalse 'form should not have had errors', form.hasErrors()
	}

	void testClearErrors() {
        def controller = ga.getControllerClass("TestController").newInstance()

        controller.params.url = "not_a_url"
        controller.params.input = "helloworld"

        def model = controller.three()

        def form = model.form
        assertNotNull 'did not find expected form', form
        assertTrue 'form should have had errors', form.hasErrors()
        form.clearErrors()
        assertFalse 'clearErrors did not work', form.hasErrors()
    }

    void testHasErrors() {
        def controller = ga.getControllerClass("TestController").newInstance()

        assertTrue controller.index()['formErrors']

    }

    void testValidate() {
        def controller = ga.getControllerClass("TestController").newInstance()

        assertFalse controller.validate()['formErrors']

    }

    void testHasBindingErrors() {

        def controller = ga.getControllerClass("TestController").newInstance()

        controller.params.url = "not_a_url"
        controller.params.input = "helloworld"

        def model = controller.two()

        Errors errors = model?.formErrors
        assert errors
        assert errors.hasErrors()
        assertEquals 1, errors.allErrors.size()
    }

    void testValidatingTwice() {
        // GRAILS-4918
        def controller = ga.getControllerClass("TestController").newInstance()

        controller.params.url = "http://grails.org"
        controller.params.input = "someverylongstringthatfailsvalidation"

        def model = controller.four()

        Errors errors = model?.formErrors
        assert errors
        assert errors.hasErrors()
        assertEquals 1, errors.allErrors.size()
    }

}