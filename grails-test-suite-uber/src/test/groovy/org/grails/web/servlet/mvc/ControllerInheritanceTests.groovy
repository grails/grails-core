package org.grails.web.servlet.mvc

import grails.artefact.Artefact
import grails.testing.web.controllers.ControllerUnitTest
import org.junit.Test

class ControllerInheritanceTests implements ControllerUnitTest<ControllerInheritanceFooController> {
    // test for GRAILS-6247
    @Test
    void testCallSuperMethod() {
        controller.bar()
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

