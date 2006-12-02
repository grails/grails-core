package org.codehaus.groovy.grails.jobs.plugins;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*


class QuartzGrailsPluginTests extends AbstractGrailsMockTests {

	void onSetUp() {
		gcl.parseClass(
"""
class SimpleJob {
	def startInterval = 0
	def timeout = 1000
	def execute() {
		println "hello"
	}		
}
class CronJob  {
 def cronExpression = "0 0 6 * * ?"

 def name = "MyTask"
 def group = "MyGroup"

 def execute(){
   print "Job run!"
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
		assert appCtx.containsBean("SimpleJobTrigger")
		assert appCtx.containsBean("SimpleJobJobDetail")
		assert appCtx.containsBean("CronJobJobDetail")
		assert appCtx.containsBean("CronJobTrigger")
		assert appCtx.containsBean("CronJob")
	}	
}