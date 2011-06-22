package org.codehaus.groovy.grails.web.metaclass

import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ForwardMethodTests extends AbstractGrailsControllerTests {

    @Override
    protected Collection<Class> getControllerClasses() {
        [ForwardingController]
    }

    void testForwardMethod() {
        def testController = new ForwardingController()

        webRequest.controllerName = "fowarding"
        assertEquals "/grails/fowarding/two.dispatch",testController.one()
        assertEquals "/grails/next/go.dispatch",testController.three()
        assertEquals "/grails/next/go.dispatch?id=10",testController.four()
        assertEquals "bar", request.foo
    }
}

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
