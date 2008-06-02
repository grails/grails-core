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


    void testHasErrors() {
        def controller = ga.getControllerClass("TestController").newInstance()

        assertTrue controller.index()['formErrors']

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

}