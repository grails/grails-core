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
package org.codehaus.groovy.grails.services.plugins

import org.codehaus.groovy.grails.plugins.support.*
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.aop.target.HotSwappableTargetSource
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean

/**
 * A plug-in that configures services in the spring context 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class ServicesGrailsPlugin {
	
	def version = GrailsPluginUtils.getGrailsVersion()
	def dependsOn = [hibernate:version]
	                 
    def watchedResources = "**/grails-app/services/*Service.groovy"
	                 
	def doWithSpring = {
		application.grailsServiceClasses.each { serviceClass ->
			"${serviceClass.fullName}ServiceClass"(MethodInvokingFactoryBean) {
				targetObject = ref("grailsApplication", true)
				targetMethod = "getGrailsServiceClass"
				arguments = serviceClass.fullName
			}
						
			if(serviceClass.transactional) {
				def props = new Properties()
				props."*"="PROPAGATION_REQUIRED"
				"${serviceClass.propertyName}"(TransactionProxyFactoryBean) {
					target = { bean ->
						bean.factoryBean = "${serviceClass.fullName}ServiceClass"
						bean.factoryMethod = "newInstance"
						bean.autowire = "byName"
					}
					proxyTargetClass = true
					transactionAttributes = props
					transactionManager = transactionManager
				}
			}
			else {
				"${serviceClass.propertyName}"(serviceClass.getClazz()) { bean ->
					bean.autowire =  true
				}
			}
		}
	}
	
	def onChange = { event ->
		if(event.source) {
			def serviceClass = application.addServiceClass(event.source)
			if(serviceClass.transactional) {
				log.warning "Transactional services classes cannot be reloaded. Skipping $serviceClass"
			}
			else {
				def serviceName = "${serviceClass.propertyName}"
				def beans = beans {
					"$serviceName"(serviceClass.getClazz()) { bean ->
						bean.autowire =  true
					}					
				}
				if(event.ctx) {
					event.ctx.registerBeanDefinition(serviceName, beans.getBeanDefinition(serviceName))
				}
			}
		}
	}
}