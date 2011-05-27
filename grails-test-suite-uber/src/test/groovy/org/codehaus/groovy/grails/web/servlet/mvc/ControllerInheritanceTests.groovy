package org.codehaus.groovy.grails.web.servlet.mvc

class ControllerInheritanceTests extends AbstractGrailsControllerTests {

    @Override
    protected void onSetUp() {
        gcl.parseClass '''
class ControllerInheritanceFooBaseController {

    protected void bar() {
        println('bar in base class')
    }
}

class ControllerInheritanceFooController extends ControllerInheritanceFooBaseController {

    @Override
    protected void bar() {
        println "bar in subclass"
        super.bar()
        println "bar after subclass"
    }

    def index = {
        bar()
        render 'hello'
    }
}
        '''
    }

    // test for GRAILS-6247
    void testCallSuperMethod() {
        def controller = ga.getControllerClass("ControllerInheritanceFooController").newInstance()
        controller.index()
    }
}
