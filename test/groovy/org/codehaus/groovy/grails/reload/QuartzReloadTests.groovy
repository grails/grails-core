package org.codehaus.groovy.grails.reload;

 import org.codehaus.groovy.grails.web.servlet.mvc.*
 import org.codehaus.groovy.grails.commons.*
 import org.apache.commons.logging.*
 import org.codehaus.groovy.grails.plugins.web.*

/**
 * Tests for auto-reloading of Quartz jobs
 *
 * @author Marcel Overdijk
 **/

class QuartzReloadTests extends AbstractGrailsPluginTests {

    def reloadJob = '''
class SimpleJob {
	long startDelay = 0
	long timeout = 2000
	def group = "MyGroup"
	boolean concurrent = false
	boolean sessionRequired = true
	
	def execute() {
	}		
}
'''
    void testReloadJob() {

			// verify that job beans are registered
            assert appCtx.containsBean("SimpleJob")
            assert appCtx.containsBean("SimpleJobJobDetail")
            assert appCtx.containsBean("SimpleJobTrigger")

            // test if properties of jobs are set as expected
            // note that the Spring api does not have getters for startDelay, group and concurrent properties
            // so these cannot be tested
			def simpleJobDetail = appCtx.getBean("SimpleJobJobDetail")
            def simpleJobTrigger = appCtx.getBean("SimpleJobTrigger")
			assertEquals(0, simpleJobDetail.jobListeners.size() )
            assertEquals(1000, simpleJobTrigger.getRepeatInterval())

			// reload job
            def event = [source: new GroovyClassLoader().parseClass(reloadJob), ctx: appCtx]
            def plugin = mockManager.getGrailsPlugin("quartz")
            def eventHandler = plugin.instance.onChange
            eventHandler.delegate = plugin
            eventHandler.call(event)

			// verify that job beans are registered
            assert appCtx.containsBean("SimpleJob")
            assert appCtx.containsBean("SimpleJobJobDetail")
            assert appCtx.containsBean("SimpleJobTrigger")

            // test if properties of jobs are set as expected
            // note that the Spring api does not have getters for startDelay, group and concurrent properties
            // so these cannot be tested
			simpleJobDetail = appCtx.getBean("SimpleJobJobDetail")
            simpleJobTrigger = appCtx.getBean("SimpleJobTrigger")
			assertEquals(1, simpleJobDetail.jobListeners.size() )
            assertEquals(2000, simpleJobTrigger.getRepeatInterval())

			// although test coverage is not 100% percent, 
			// this test gives a good indication if job reloading works or not
    }

	void onSetUp() {
		gcl.parseClass(
'''
class SimpleJob {
	long startDelay = 0
	long timeout = 1000
	def group = "MyGroup"
	boolean concurrent = false
	boolean sessionRequired = false
	
	def execute() {
	}		
}
'''
        )

		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.quartz.QuartzGrailsPlugin")
    }

}