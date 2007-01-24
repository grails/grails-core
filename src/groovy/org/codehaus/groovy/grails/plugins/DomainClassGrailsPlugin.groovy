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
package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.validation.*
import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.aop.target.HotSwappableTargetSource

/**
 * A plug-in that configures the domain classes in the spring context 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class DomainClassGrailsPlugin {
	
	def version = GrailsPluginUtils.getGrailsVersion()
	def dependsOn = [i18n:version]
	
	def doWithSpring = {
		application.grailsDomainClasses.each { dc ->
		    // Note the use of Groovy's ability to use dynamic strings in method names!
		    
			"${dc.fullName}DomainClass"(MethodInvokingFactoryBean) {
				targetObject = ref("grailsApplication", true)
				targetMethod = "getGrailsDomainClass"
				arguments = dc.fullName
			}
			"${dc.fullName}PersistentClass"(MethodInvokingFactoryBean) {
				targetObject = ref("${dc.fullName}DomainClass")
				targetMethod = "getClazz"        						
            }
            "${dc.fullName}Validator"(GrailsDomainClassValidator) {
                messageSource = ref("messageSource")
                domainClass = ref("${dc.fullName}DomainClass")                
            }

		}
	}
}