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
 * Gant script that handles the packaging of Grails plug-ins
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU  

GCL = new GroovyClassLoader()

Ant.property(environment:"env")   
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

pluginResources = []

                
target( packagePlugins : "Packages any Grails plugins that are installed for this project") {
	depends( classpath )
	try {
	  
	    def basePluginFile = baseFile.listFiles().find { it.name.endsWith("GrailsPlugin.groovy")}
		def basePlugin = null

		if(basePluginFile) {
			basePlugin = new org.springframework.core.io.FileSystemResource(basePluginFile)
			pluginResources << basePlugin			
		}                        

		pluginResources += resolveResources("file:${basedir}/plugins/*/*GrailsPlugin.groovy").toList()
	   	for(p in pluginResources) {
	   		def pluginBase = p.file.parentFile.canonicalFile
	     	def pluginPath = pluginBase.absolutePath
			def pluginName = pluginBase.name
			def pluginNameWithVersion = pluginBase.name
			
	   		Ant.sequential {            
				if(new File("${pluginBase}/grails-app/conf/hibernate").exists()) {
		   			copy(todir:classesDirPath, failonerror:false) {
		   				fileset(dir:"${pluginBase}/grails-app/conf/hibernate", includes:"**", excludes:"*.groovy")
		   			}   			                     															
				}                           
				if(new File("${pluginBase}/grails-app/conf").exists()) {
		   			copy(todir:classesDirPath, failonerror:false) {
		   				fileset(dir:"${pluginBase}/grails-app/conf", includes:"*", excludes:"*.groovy")
		   			}   			                     										
				}
	            if(new File("${pluginPath}/web-app").exists()) {
					Ant.mkdir(dir:"${basedir}/web-app/plugins/${pluginName}")                           
					if(basePlugin != p) {
			  			copy(todir:"${basedir}/web-app/plugins/${pluginName}") {
			   				fileset(dir:"${pluginBase}/web-app", includes:"**", excludes:"**/WEB-INF/**, **/META-INF/**")
			   			}  		                     							
					}
				}
	   		}             
	   	}  		
	}
	catch(Exception e) {
		e.printStackTrace(System.out)
	}
}

