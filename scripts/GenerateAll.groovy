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

/**
 * Gant script that generates a CRUD controller and matching views for a given domain class
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import groovy.text.SimpleTemplateEngine
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.scaffolding.*
import org.springframework.mock.web.MockServletContext
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.commons.ApplicationHolder;


Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Bootstrap.groovy" )
    
generateViews = true
generateController = true

target ('default': "Generates a CRUD interface (controller + views) for a domain class") {
	depends( checkVersion, packageApp )
	typeName = "Domain Class"
	promptForName()
	generateAll()
}            

target(generateAll:"The implementation target") {
	                                     
	rootLoader.addURL(classesDir.toURI().toURL())
    loadApp()
    
    def name = args.trim()
    name = name.indexOf('.') > -1 ? name : GCU.getClassNameRepresentation(name)
    def domainClass = grailsApp.getDomainClass(name)
	
	if(!domainClass) {
   		println "Domain class not found in grails-app/domain, trying hibernate mapped classes..."		
		try {
			def config = new GrailsRuntimeConfigurator(grailsApp, appCtx)  
			appCtx = config.configure(appCtx.servletContext)     			
		}   
		catch(Exception e) {
			println e.message
			e.printStackTrace()
		}
		domainClass = grailsApp.getDomainClass(name)  
	}

   if(domainClass) {
		def generator = new DefaultGrailsTemplateGenerator()                                        
		if(generateViews) {
			event("StatusUpdate", ["Generating views for domain class ${domainClass.fullName}"])				
			generator.generateViews(domainClass, basedir)
		}                                                                                       
		if(generateController) {
			event("StatusUpdate", ["Generating controller for domain class ${domainClass.fullName}"])		
			generator.generateController(domainClass, basedir)				
		}
		event("StatusFinal", ["Finished generation for domain class ${domainClass.fullName}"])
	}                                                
	else {
		event("StatusFinal", ["No domain class found for name ${name}. Please try again and enter a valid domain class name"])		
	}
}
