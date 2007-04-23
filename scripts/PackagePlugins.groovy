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

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )

task ( "default" : "Performs packaging of Grails plugins for when they are distributed as part of a WAR") {
   packagePlugins()                                                      
}     
                
task( packagePlugins : "Packages any Grails plugins that are installed for this project") {
	depends( classpath )   
	Ant.mkdir(dir:"${basedir}/web-app/WEB-INF/lib")
	try {
	   	def plugins = resolveResources("**GrailsPlugin.groovy").toList()
		plugins += resolveResources("plugins/*/*GrailsPlugin.groovy").toList()
	   	plugins?.each { p ->  	   
	   		def pluginBase = p.file.parentFile  
	     	def pluginPath = pluginBase.absolutePath
			def pluginName = pluginBase.name
			def pluginNameWithVersion = pluginBase.name
			
	   		Ant.sequential {            
				if(new File("${pluginBase}/lib").exists()) {
		   			copy(todir:"${basedir}/web-app/WEB-INF/lib", failonerror:false) {
		   				fileset(dir:"${pluginBase}/lib", includes:"**")
		   			}   			                     					
				}                                           
				if(new File("${pluginBase}/grails-app/conf").exists()) {
		   			copy(todir:"${basedir}/web-app/WEB-INF/classes", failonerror:false) {
		   				fileset(dir:"${pluginBase}/grails-app/conf", includes:"*", excludes:"*.groovy")
		   			}   			                     										
				}
				if(new File("${pluginBase}/grails-app/views").exists()) { 
					def pluginViews = "${basedir}/web-app/WEB-INF/plugins/${pluginNameWithVersion}/grails-app/views"
					mkdir(dir:pluginViews)   			
	   				copy(todir:pluginViews, failonerror:false) {
	   					fileset(dir:"${pluginBase}/grails-app/views", includes:"**")
	   				}
	            }
	            if(new File("${pluginPath}/web-app").exists()) {
					Ant.mkdir(dir:"${basedir}/web-app/plugins/${pluginName}")
		  			copy(todir:"${basedir}/web-app/plugins/${pluginName}") {
		   				fileset(dir:"${pluginBase}/web-app", includes:"**", excludes:"**/WEB-INF/**, **/META-INF/**")
		   			}  		                     	
				}
				path(id:"classpath") {
					fileset(dir:"${basedir}/lib")
					fileset(dir:"${grailsHome}/lib")
					fileset(dir:"${grailsHome}/dist")        
					fileset(dir:"${basedir}/web-app/WEB-INF/classes")  
					fileset(dir:"${pluginBase}/lib")
				}       
				if(new File("${pluginBase}/src/java").exists()) {                                        
					javac(srcdir:"${pluginBase}/src/java",destdir:"${basedir}/web-app/WEB-INF/classes",
							classpathref:"classpath",debug:"on",deprecation:"on", optimize:"off")
				}
				if(new File("${pluginBase}/src/groovy").exists()) { 
					groovyc(srcdir:"${pluginBase}/src/groovy",destdir:"${basedir}/web-app/WEB-INF/classes",
							classpathref:"classpath")			
				}
	   		}             
	   	}  		
	}
	catch(Exception e) {
		e.printStackTrace(System.out)
	}
}

