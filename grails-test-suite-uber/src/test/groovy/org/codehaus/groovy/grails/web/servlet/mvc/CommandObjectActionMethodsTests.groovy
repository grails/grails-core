package org.codehaus.groovy.grails.web.servlet.mvc

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class CommandObjectActionMethodsTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        gcl.parseClass('''
@grails.artefact.Artefact("Controller")
class SampleController {
    def renderWithCmd = { SampleCommand cmd ->
        render(view:'alt', model:[cmd:cmd])
    }

    def defaultRenderWithCmd = { SampleCommand cmd ->
        [cmd:cmd]
    }

    def twoCommandAction = { SampleCommand cmd1, SecondCommand cmd2 ->
        [cmd1:cmd1, cmd2:cmd2]
    }
}

class SampleCommand {
    String first
    String second
}
class SecondCommand {
    String name
}
        ''')
    }

    void testInvokeControllerMethodWithCommandObject() {
        def cmd = ga.getClassLoader().loadClass("SampleCommand").newInstance()
        def ctrl = ga.getControllerClass("SampleController").clazz.newInstance()
        ctrl.params.first = "one"
        ctrl.params.second = "two"

        def model = ctrl.defaultRenderWithCmd()

        assertEquals "one", model.cmd.first
        assertEquals "two", model.cmd.second
    }

    void testInvokeControllerMethodWithCommandObjectAndRenderMethod() {
        def cmd = ga.getClassLoader().loadClass("SampleCommand").newInstance()
        def ctrl = ga.getControllerClass("SampleController").newInstance()
        ctrl.params.first = "one"
        ctrl.params.second = "two"

        def model = ctrl.renderWithCmd()
    }

}
