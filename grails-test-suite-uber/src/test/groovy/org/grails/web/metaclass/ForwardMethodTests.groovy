package org.grails.web.metaclass

import grails.artefact.Artefact

import org.grails.web.servlet.mvc.AbstractGrailsControllerTests

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
        assertEquals "/fowarding/two",testController.one()
        assertEquals "/next/go",testController.three()
        assertEquals "/next/go?id=10",testController.four()
        assertEquals "bar", request.foo
    }
}

@Artefact('Controller')
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
