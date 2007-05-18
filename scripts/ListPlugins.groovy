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
 * Gant script that handles the creation of Grails plugins
 * 
 * @author Graeme Rocher
 *
 * @since 0.5
 */
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU  
 
DEFAULT_PLUGIN_DIST = "http://dist.codehaus.org/grails-plugins/"            

Ant.property(environment:"env")   
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )

task ( "default" : "Lists plug-ins that are hosted by the Grails server") {
   depends(checkVersion)
   listPlugins()
}     
                
task(listPlugins:"Implementation task") {
	try {
		def text = new URL(DEFAULT_PLUGIN_DIST).text  
		println '''
Plug-ins available in the Grails repository are listed below:
-------------------------------------------------------------
'''
		text.eachMatch( /<a href="(.+?)">/ ) {
			def file = it[1]
			if(file.startsWith("grails-")) {
				def fullName = file[7..-5]
				def name = fullName[0..fullName.indexOf('.')-3]
				def version = fullName[fullName.indexOf('.')-1..-1]

				println "Name: $name, Version: $version"
			}
		}
       println ''		
       println "To install type grails install-plugin [NAME] [VERSION]"		 
 	   println ''		
 	   println "For further info visit http://grails.org/Plugins"
	   println ''		
	}   
	catch(Exception e) {
        event("StatusError", "Unable to list plug-ins, please check you have a valid internet connection")
	}
}    


