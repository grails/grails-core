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
package org.codehaus.groovy.grails.scaffolding.plugins;

import org.codehaus.groovy.grails.plugins.support.*
import org.codehaus.groovy.grails.scaffolding.*;

/**
 * A plug-in that handles the configuration of Hibernate within Grails 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class ScaffoldingGrailsPlugin {

	def version = GrailsPluginUtils.getGrailsVersion()
	def dependsOn = [hibernate:version, controllers:version]
	                 
	def doWithSpring = {
		application.controllers.each { controller ->
			log.debug("Checking controller ${controller.name} for scaffolding settings")
			
			def scaffoldClass = controller.scaffoldedClass
			if(!scaffoldClass) {
				scaffoldClass = application.getGrailsDomainClass(controller.name)?.clazz
			}
			
			if(scaffoldClass) {
				log.debug("Configuring scaffolding for class [$scaffoldClass]")
				// create the scaffold domain which is used to interact with persistence
				"${scaffoldClass.name}Domain"(	GrailsScaffoldDomain, 
												scaffoldClass.name,
												sessionFactory)
												
				// setup the default view resolver that resolves views from a Grails app
				scaffoldViewResolver(DefaultGrailsScaffoldViewResolver, ref("grailsApplication", true))
				// setup the default response handler that simply delegates to a view
				defaultScaffoldResponseHandler(ViewDelegatingScaffoldResponseHandler) { 
					scaffoldViewResolver = scaffoldViewResolver 
				}
				// setup a response handler factory which can be used to output different 
				// responses based on the model returned by the scaffold domain
				
				responseHandlerFactory(	DefaultGrailsResponseHandlerFactory,
										ref("grailsApplication",true),
										defaultScaffoldResponseHandler )										
					
				log.debug "Registering new scaffolder [${controller.fullName}Scaffolder]"
				"${controller.fullName}Scaffolder" (DefaultGrailsScaffolder) {
					scaffoldRequestHandler = { DefaultScaffoldRequestHandler dsrh ->
						scaffoldDomain = ref("${scaffoldClass.name}Domain")
					}
					scaffoldResponseHandlerFactory = responseHandlerFactory
				}
			}
		}
	}
	
}