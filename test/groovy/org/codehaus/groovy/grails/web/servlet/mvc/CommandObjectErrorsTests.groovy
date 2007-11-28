/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 28, 2007
 */
package org.codehaus.groovy.grails.web.servlet.mvc
class CommandObjectErrorsTests extends AbstractGrailsControllerTests{

    public void onSetUp() {
        gcl.parseClass '''
class TestController {

    def index = { Form form ->
        [formErrors:form.hasErrors()]
    }
}
class Form {
    String input

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

}