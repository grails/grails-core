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
package org.codehaus.groovy.grails.plugins.scaffolding;

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.scaffolding.*

/**
* A plug-in that handles the configuration of Hibernate within Grails
*
* @author Graeme Rocher
* @since 0.4
*/
class ScaffoldingGrailsPlugin {

	def version = grails.util.GrailsUtil.getGrailsVersion()
	def dependsOn = [hibernate:version, controllers:version]
	def observe = ['controllers']
	
	static BEAN_DEFINITIONS = { scaffoldClass, controllerClass ->
        "${scaffoldClass.name}Domain"(	GrailsScaffoldDomain,
                                        scaffoldClass,
                                        ref("sessionFactory"))
        // setup the default response handler that simply delegates to a view
        "${scaffoldClass.name}ResponseHandler"(TemplateGeneratingResponseHandler) {
            templateGenerator = {DefaultGrailsTemplateGenerator bean->}
            viewResolver = ref("jspViewResolver")
            grailsApplication = ref("grailsApplication", true)
            scaffoldedClass = scaffoldClass
        }
        // setup a response handler factory which can be used to output different
        // responses based on the model returned by the scaffold domain

        "${scaffoldClass.name}ResponseHandlerFactory"(	DefaultGrailsResponseHandlerFactory,
                                ref("grailsApplication",true),
                                ref("${scaffoldClass.name}ResponseHandler") )

        "${controllerClass.fullName}Scaffolder" (DefaultGrailsScaffolder) {
            scaffoldRequestHandler = { DefaultScaffoldRequestHandler dsrh ->
                scaffoldDomain = ref("${scaffoldClass.name}Domain")
            }
            scaffoldResponseHandlerFactory = ref( "${scaffoldClass.name}ResponseHandlerFactory")
        }
		
	}
		
	def doWithSpring = {
		application.controllerClasses.each { controller ->
			log.debug("Checking controller ${controller.name} for scaffolding settings")
			  
			if(controller.isScaffolding()) {
				def scaffoldClass = controller.scaffoldedClass
				if(!scaffoldClass) {
					scaffoldClass = application.getArtefact(DomainClassArtefactHandler.TYPE, controller.name)?.clazz
				}
                   
				
				
				if(scaffoldClass) {				   
					
					log.debug("Configuring scaffolding for class [$scaffoldClass]")
					def beans = BEAN_DEFINITIONS.curry(scaffoldClass, controller)
					beans.delegate = delegate
					beans.call()
				}
				
			}
		}
	}
	    
	/**
	 * Handles registration of dynamic scaffolded methods
	 */
	def doWithDynamicMethods = { ctx ->
		registerScaffoldedActions(application, ctx)
        // configure scaffolders
        GrailsScaffoldingUtil.configureScaffolders(application, ctx);		
	}       
	
	def registerScaffoldedActions(application,ctx) {
			application.controllerClasses.each { controller ->
				if(controller.scaffolding) {
					if(ctx.containsBean("${controller.fullName}Scaffolder")) {
						def scaffolder = ctx."${controller.fullName}Scaffolder"

						scaffolder.supportedActionNames.each { name ->
							if(!controller.hasProperty(name)) {
								def getter = GCU.getGetterName(name)
								controller.metaClass."$getter" = {->
									scaffolder.getAction(delegate, name)
								}
							}
						}
					}
				}
			}
			
	}	
	

	def onChange = { event ->		
	    if(application.isControllerClass(event.source)) {
			def controllerClass = application.getControllerClass(event.source.name)

            if(controllerClass.scaffolding ) {
                def scaffoldClass = controllerClass.scaffoldedClass
                def ctx = event.ctx

                if(!scaffoldClass) {
                    scaffoldClass = application.getDomainClass(controllerClass.name)?.clazz
                }

                if(scaffoldClass) {

                    log.info "Re-configuring scaffolding for reloaded class ${event.source}"

                    def beans = beans(BEAN_DEFINITIONS.curry(scaffoldClass,controllerClass))

                	beans.registerBeans(ctx)   														

                    registerScaffoldedActions(event.application, ctx)
                    // configure scaffolders
                    GrailsScaffoldingUtil.configureScaffolders(event.application, ctx);		

                }
            }
        }
	}

}
