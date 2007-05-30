package org.codehaus.groovy.grails.reload;

 import org.codehaus.groovy.grails.web.servlet.mvc.*
 import org.codehaus.groovy.grails.commons.*
 import org.apache.commons.logging.*

/**
 * Tests for auto-reloading of controllers
 *
 * @author Graeme Rocher
 **/

class ControllerReloadTests extends AbstractGrailsControllerTests {

    def reloadedController = '''
class TestController {
    def testMe = {
        render "bar"
    }
}
    '''
    void testReloadController() {
        runTest {
            def testController = ga.getControllerClass("TestController").newInstance()
            testController.testMe.call()

            assertEquals "foo", testController.response.contentAsString

            def newGcl = new GroovyClassLoader()
            def event = [source:newGcl.parseClass(reloadedController),
                         ctx:appCtx]

            def plugin = mockManager.getGrailsPlugin("controllers")

            def eventHandler = plugin.instance.onChange
            eventHandler.delegate = [log: {false} as Log, application:ga]
            eventHandler.call(event)
            GrailsMetaClassUtils.copyExpandoMetaClass(testController.getClass(), event.source, true)

            def newController = ga.getControllerClass("TestController").newInstance()

            newController.testMe.call()

            assertEquals "foobar", newController.response.contentAsString
        }
    }

	void onSetUp() {
		gcl.parseClass(
'''
class TestController {
    def testMe = {
        render "foo"
    }
}
'''
        )
    }

}