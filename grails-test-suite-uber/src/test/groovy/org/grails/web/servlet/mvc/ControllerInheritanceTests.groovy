package org.grails.web.servlet.mvc

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

class ControllerInheritanceTests extends Specification implements ControllerUnitTest<ControllerInheritanceFooController> {
    // test for GRAILS-6247
    void testCallSuperMethod() {
        when:
        controller.bar()

        then:
        noExceptionThrown()
    }
}

@Artefact('Controller')
class ControllerInheritanceFooBaseController {

    void bar() {
        println('bar in base class')
    }
}

@Artefact('Controller')
class ControllerInheritanceFooController extends ControllerInheritanceFooBaseController {}

