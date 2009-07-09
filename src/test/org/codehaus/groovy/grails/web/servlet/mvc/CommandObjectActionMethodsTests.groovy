/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Sep 21, 2007
 */
package org.codehaus.groovy.grails.web.servlet.mvc
class CommandObjectActionMethodsTests extends AbstractGrailsControllerTests {

    public void onSetUp() {
        gcl.parseClass('''
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


        def ctrl = ga.getControllerClass("SampleController").newInstance()
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

    void testInvokeControllerMethodWithPassedCommandObject() {
        def cmd1 = ga.getClassLoader().loadClass("SampleCommand").newInstance()
        def cmd2 = ga.getClassLoader().loadClass("SecondCommand").newInstance()


        def ctrl = ga.getControllerClass("SampleController").newInstance()
        cmd1.first = "one"
        cmd1.second = "two"

        def model = ctrl.defaultRenderWithCmd(cmd1)

        assertEquals "one", model.cmd.first
        assertEquals "two", model.cmd.second

        cmd2.name = "hello"
        model = ctrl.twoCommandAction(cmd1,cmd2)

        assertEquals "one", model.cmd1.first
        assertEquals "two", model.cmd1.second
        assertEquals "hello", model.cmd2.name

        
    }

}