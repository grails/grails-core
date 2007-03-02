package org.codehaus.groovy.grails.jobs.plugins;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.springframework.scheduling.quartz.*

class QuartzGrailsPluginTests extends AbstractGrailsMockTests {

	void onSetUp() {
		gcl.parseClass(
"""
class SimpleJob {
	long startDelay = 0
	long timeout = 1000
	def group = "MyGroup"
	boolean concurrent = false
	def execute() {
		println "SimpleJob executed!"
	}		
}
class SimpleDefaultJob {
	def execute() {
		println "SimpleDefaultJob executed!"
	}		
}
class CronJob  {
	def cronExpression = "0 15 10 ? * 6#3"
	def group = "MyGroup"
	boolean concurrent = false
	def execute(){
		print "CronJob executed!"
	}
}
class CronDefaultJob  {
	def cronExpression = ""
	def execute(){
		print "CronDefaultJob executed!"
	}
}
""")
	}
	
	void testJobsPlugin() {
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.jobs.plugins.QuartzGrailsPlugin")
		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()
		
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext()
		
		assert appCtx.containsBean("SimpleJob")
		assert appCtx.containsBean("SimpleJobJobDetail")
		assert appCtx.containsBean("SimpleJobTrigger")
		assert appCtx.containsBean("SimpleDefaultJob")
		assert appCtx.containsBean("SimpleDefaultJobJobDetail")
		assert appCtx.containsBean("SimpleDefaultJobTrigger")
		assert appCtx.containsBean("CronJob")
		assert appCtx.containsBean("CronJobJobDetail")
		assert appCtx.containsBean("CronJobTrigger")
		assert appCtx.containsBean("CronDefaultJob")
		assert appCtx.containsBean("CronDefaultJobJobDetail")
		assert appCtx.containsBean("CronDefaultJobTrigger")
		
		// test if properties of jobs are set as expected
		// note that the Spring api does not have getters for startDelay, group and concurrent properties
		// so these cannot be tested
		
		SimpleTriggerBean simpleJobTrigger = (SimpleTriggerBean)appCtx.getBean("SimpleJobTrigger")
		assertEquals(1000, simpleJobTrigger.getRepeatInterval())
		
		SimpleTriggerBean simpleDefaultJobTrigger = (SimpleTriggerBean)appCtx.getBean("SimpleDefaultJobTrigger")
		assertEquals(DefaultGrailsTaskClass.DEFAULT_TIMEOUT, simpleDefaultJobTrigger.getRepeatInterval())
		
		CronTriggerBean cronJobTrigger = (CronTriggerBean)appCtx.getBean("CronJobTrigger")
		assertEquals("0 15 10 ? * 6#3", cronJobTrigger.getCronExpression())
		
		CronTriggerBean cronDefaultJobTrigger = (CronTriggerBean)appCtx.getBean("CronDefaultJobTrigger")
		assertEquals(DefaultGrailsTaskClass.DEFAULT_CRON_EXPRESSION, cronDefaultJobTrigger.getCronExpression())
	}	

}