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
package org.codehaus.groovy.grails.jobs.plugins

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.scheduling.quartz.CronTriggerBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerBean;


/**
 * A plug-in that configures Quartz job support in Grails 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class QuartzGrailsPlugin {
	
	def version = GrailsPluginUtils.getGrailsVersion()
	
	def doWithSpring = {
		def schedulerReferences = []
		application.grailsTasksClasses.each { jobClass ->
			
			def fullName = jobClass.fullName
			"${fullName}JobClass"(MethodInvokingFactoryBean) {
				targetObject = ref("grailsApplication", true)
				targetMethod = "getGrailsTaskClass"
				arguments = fullName
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
		
			schedulerReferences << ref("${fullName}Trigger")
		}
		if(schedulerReferences) {
			quartzScheduler(SchedulerFactoryBean) {
				triggers = schedulerReferences
			}			
		}
	}
}