/*
 * Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package org.codehaus.groovy.grails.plugins.quartz

import org.codehaus.groovy.grails.jobs.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.scheduling.quartz.CronTriggerBean
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.scheduling.quartz.SimpleTriggerBean
import org.springframework.util.MethodInvoker
import org.codehaus.groovy.grails.commons.TaskArtefactHandler
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.quartz.JobListener

/**
 * A plug-in that configures Quartz job support in Grails 
 * 
 * @author Graeme Rocher
 * @author Marcel Overdijk
 * @since 0.4
 */
class QuartzGrailsPlugin {
	
	def version = GrailsPluginUtils.getGrailsVersion()
	def dependsOn = [core:version,hibernate:version]
	def watchedResources = "file:./grails-app/jobs/*Job.groovy"

	def doWithSpring = {
		def schedulerReferences = []
		application.taskClasses.each { jobClass ->
			configureJobBeans.delegate = delegate
			configureJobBeans(jobClass)
			schedulerReferences << ref("${jobClass.fullName}Trigger")
		}
		// register SessionBinderJobListener to bind Hibernate Session to each Job's thread
		"${SessionBinderJobListener.NAME}"(SessionBinderJobListener) { bean ->
			bean.autowire = "byName"
		}
        quartzScheduler(SchedulerFactoryBean) {
            triggers = schedulerReferences
            jobListeners = [ref("${SessionBinderJobListener.NAME}")]
        }
	}

	def onChange = { event ->
        if(application.isArtefactOfType(TaskArtefactHandler.TYPE, event.source)) {
			log.debug("Job ${event.source} changed. Reloading...")
			def context = event.ctx
			if(!context) {
				log.debug("Application context not found. Can't reload")
				return
			}
			
			// get quartz scheduler
			def scheduler = context.getBean("quartzScheduler")
			if(scheduler) {

				// if job already exists, delete it from scheduler
				def jobClass = application.getTaskClass(event.source?.name)
				if(jobClass) {
					scheduler.deleteJob(jobClass.fullName, jobClass.group)
					log.debug("Job ${jobClass.fullName} deleted from scheduler")
				}
	
				// add job artefact to application
				jobClass = application.addArtefact(TaskArtefactHandler.TYPE, event.source)
	
				// configure and register job beans
				def fullName = jobClass.fullName
				def beans = beans {
					configureJobBeans.delegate = delegate
					configureJobBeans(jobClass)
				}
				event.ctx.registerBeanDefinition("${fullName}JobClass", beans.getBeanDefinition("${fullName}JobClass"))
				event.ctx.registerBeanDefinition("${fullName}", beans.getBeanDefinition("${fullName}"))
				event.ctx.registerBeanDefinition("${fullName}JobDetail", beans.getBeanDefinition("${fullName}JobDetail"))
				event.ctx.registerBeanDefinition("${fullName}Trigger", beans.getBeanDefinition("${fullName}Trigger"))
				
				// add job to scheduler, and associate trigger with it
				scheduler.scheduleJob(event.ctx.getBean("${fullName}JobDetail"), event.ctx.getBean("${fullName}Trigger"))
				log.debug("Job ${jobClass.fullName} scheduled")
			}
		}
	}
	
	def configureJobBeans = { jobClass ->
		def fullName = jobClass.fullName
		"${fullName}JobClass"(MethodInvokingFactoryBean) {
			targetObject = ref("grailsApplication", true)
			targetMethod = "getArtefact"
			arguments = [TaskArtefactHandler.TYPE, fullName]
		}
		"${fullName}"(ref("${fullName}JobClass")) { bean ->
			bean.factoryMethod = "newInstance"
			bean.autowire = "byName"
		}
		"${fullName}JobDetail"(MethodInvokingJobDetailFactoryBean) {
			targetObject = ref(fullName)
			targetMethod = GrailsTaskClassProperty.EXECUTE
			concurrent = jobClass.concurrent
			group = jobClass.group
			name = fullName
			if( jobClass.sessionRequired ) {
				jobListenerNames = ["${SessionBinderJobListener.NAME}"] as String[]
			}
		}
		if(!jobClass.cronExpressionConfigured) {
			"${fullName}Trigger"(SimpleTriggerBean) {
				jobDetail = ref("${fullName}JobDetail")
				startDelay = jobClass.startDelay
				repeatInterval = jobClass.timeout
			}
		}
		else {
			"${fullName}Trigger"(CronTriggerBean) {
				jobDetail = ref("${fullName}JobDetail")
				cronExpression = jobClass.cronExpression
			}
		}
	}
}