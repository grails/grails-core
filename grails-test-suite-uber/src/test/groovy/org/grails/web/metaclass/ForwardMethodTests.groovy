package org.grails.web.metaclass

import grails.artefact.Artefact
import grails.testing.web.GrailsWebUnitTest
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ForwardMethodTests extends Specification implements GrailsWebUnitTest{

    void testForwardMethod() {
        given:
        def testController = new ForwardingController()

        webRequest.controllerName = "fowarding"

        expect:
        "/fowarding/two" == testController.one()
        "/next/go" == testController.three()
        "/next/go/10" == testController.four()
        "bar" == request.foo
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
