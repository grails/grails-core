package org.grails.web.servlet.mvc

import grails.artefact.Artefact
import grails.test.mixin.TestFor

import org.junit.Test

@TestFor(ControllerInheritanceFooController)
class ControllerInheritanceTests  {
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

