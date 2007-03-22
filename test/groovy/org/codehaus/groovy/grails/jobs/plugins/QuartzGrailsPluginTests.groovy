package org.codehaus.groovy.grails.jobs.plugins;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.springframework.scheduling.quartz.*

class QuartzGrailsPluginTests extends AbstractGrailsMockTests {

	void onSetUp() {
		gcl.parseClass("""\
import org.hibernate.SessionFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager

class SimpleJob {
	long startDelay = 0
	long timeout = 1000
	def group = "MyGroup"
	boolean concurrent = false
	boolean sessionRequired = false
	
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

        def appCtx

		try {
            plugin.doWithRuntimeConfiguration(springConfig)

            appCtx = springConfig.getApplicationContext()

            assert appCtx.containsBean("SimpleJob")
            assert appCtx.containsBean("SimpleJobJobDetail")
			def simpleJobDetail = appCtx.getBean("SimpleJobJobDetail")
            assert appCtx.containsBean("SimpleJobTrigger")
            assert appCtx.containsBean("SimpleDefaultJob")
            assert appCtx.containsBean("SimpleDefaultJobJobDetail")
			def simpleDefaultJobDetail = appCtx.getBean("SimpleDefaultJobJobDetail")
            assert appCtx.containsBean("SimpleDefaultJobTrigger")
            assert appCtx.containsBean("CronJob")
            assert appCtx.containsBean("CronJobJobDetail")
            assert appCtx.containsBean("CronJobTrigger")
            assert appCtx.containsBean("CronDefaultJob")
            assert appCtx.containsBean("CronDefaultJobJobDetail")
            assert appCtx.containsBean("CronDefaultJobTrigger")
            assert appCtx.containsBean("sessionBinderListener")

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

			assertEquals(0, simpleJobDetail.jobListeners.size() )

			// SessionBinderJobListener must be registered for SimpleDefaultJob
			assertEquals(1, simpleDefaultJobDetail.jobListeners.size())
        } finally {
    	    appCtx?.getBean('quartzScheduler').shutdown()
        }
	}
	
	void testParameters() {
		// Test integer parameters and default values
		Class jobClass = gcl.parseClass('''
	            class TestJob {
					def startDelay = 100
					def timeout = 1000
					def execute() {
						println "TestJob executed!"
					}
				}		
		''')
		GrailsTaskClass taskClass = new DefaultGrailsTaskClass(jobClass)
		assertEquals( 100, taskClass.startDelay )
		assertEquals( 1000, taskClass.timeout )
		assertTrue(taskClass.sessionRequired)
		assertTrue(taskClass.concurrent)
		assertEquals( 'GRAILS_JOBS', taskClass.group)
		assertFalse( taskClass.cronExpressionConfigured)
		
		// Test with Long parameters and some specific values
		jobClass = gcl.parseClass('''
            class TestJob1 {
				def startDelay = 10L
				def timeout = 100L
				def concurrent = false
				def sessionRequired = false
				def group = 'MyGroup'

				def execute() {
					println "TestJob executed!"
				}
			}		
		''')
		taskClass = new DefaultGrailsTaskClass(jobClass)
		assertEquals( 10, taskClass.startDelay )
		assertEquals( 100, taskClass.timeout )
		assertFalse(taskClass.sessionRequired)
		assertFalse(taskClass.concurrent)
		assertEquals( 'MyGroup', taskClass.group)
	}
	
	void testInvalidParameterTypes() {
		Class jobClass = gcl.parseClass('''
            class TestJob {
				def startDelay = "0"
				def timeout = 1000
				def execute() {
					println "TestJob executed!"
				}
			}		
		''')
		try {
			new DefaultGrailsTaskClass(jobClass)
			fail()
		} catch( IllegalArgumentException iae ) {
			// Greate
		}

		jobClass = gcl.parseClass('''
            class TestJob1 {
				def startDelay = 0
				def timeout = "1000"
				def execute() {
					println "TestJob executed!"
				}
			}		
		''')
		try {
			new DefaultGrailsTaskClass(jobClass)
			fail()
		} catch( IllegalArgumentException iae ) {
			// Greate
		}
	}

}