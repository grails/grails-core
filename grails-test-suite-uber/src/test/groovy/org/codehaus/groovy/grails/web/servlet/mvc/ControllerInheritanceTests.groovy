package org.codehaus.groovy.grails.web.servlet.mvc

class ControllerInheritanceTests extends AbstractGrailsControllerTests {

    @Override
    protected void onSetUp() {
        gcl.parseClass '''
class ControllerInheritanceFooBaseController {

    void bar() {
        println('bar in base class')
    }
}

class ControllerInheritanceFooController extends ControllerInheritanceFooBaseController {}
        '''
    }

    // test for GRAILS-6247
    void testCallSuperMethod() {
        def controller = ga.getControllerClass("ControllerInheritanceFooController").newInstance()
        controller.bar()
    }
}
