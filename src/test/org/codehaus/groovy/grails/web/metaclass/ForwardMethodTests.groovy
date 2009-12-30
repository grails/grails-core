package org.codehaus.groovy.grails.web.metaclass

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jan 9, 2009
 */

public class ForwardMethodTests extends AbstractGrailsControllerTests{

    protected void onSetUp() {
        gcl.parseClass('''
class ForwardingController {
    def one = {
        forward(action:'two')
    }

    def two = {
        render 'me'
    }

    def three = {
        forward(controller:'next', action:'go')
    }

    def four = {
       forward(controller:'next', action:'go',id:10, model:[foo:'bar'])
    }
}
''')
    }


    void testForwardMethod() {
        def testController = ga.getControllerClass("ForwardingController").newInstance()

        webRequest.controllerName = "fowarding"
        assertEquals "/grails/fowarding/two.dispatch",testController.one()
        assertEquals "/grails/next/go.dispatch",testController.three()
        assertEquals "/grails/next/go.dispatch?id=10",testController.four()
        assertEquals "bar", request.foo
    }

}