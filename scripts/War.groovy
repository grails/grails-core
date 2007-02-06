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
 * Gant script that creates a WAR file from a Grails project
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Clean.groovy" ) 
includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

task ('default': "Creates a WAR archive") {
	war()
} 

task (war: "The implementation task") {
	depends( clean, packagePlugins, packageApp )
	 
	try {
		Ant.copy(todir:"${basedir}/web-app/WEB-INF/grails-app", overwrite:true) {
			fileset(dir:"${basedir}/grails-app", includes:"**") 
		}       
		appCtxFile = "${basedir}/web-app/WEB-INF/applicationContext.xml"
		Ant.copy(file:appCtxFile, tofile:"${basedir}/.appctxbck",overwrite:true)
		
		Ant.copy(todir:"${basedir}/web-app/WEB-INF/lib") {
			fileset(dir:"${grailsHome}/dist") {
					include(name:"grails-*.jar")
			}
			fileset(dir:"${basedir}/lib") {
					include(name:"*.jar")
			}
		}
		
		Ant.replace(file:appCtxFile, 
				token:"classpath*:", value:"" ) 

		def fileName = new File(basedir).name
		warName = "${basedir}/${fileName}.war"

		warPlugins()		
		Ant.jar(destfile:warName, basedir:"${basedir}/web-app")		
		
	}   
	finally {
		cleanUpAfterWar()
	}
	println "Created WAR at ${warName}"	
}                                                                    
   
task(cleanUpAfterWar:"Cleans up after performing a WAR") {
 	Ant.move(file:"${basedir}/.appctxbck", tofile:appCtxFile, overwrite:true)
	Ant.delete(dir:"${basedir}/web-app/WEB-INF/grails-app", failonerror:true)
	Ant.delete(dir:"${basedir}/web-app/WEB-INF/plugins") 
	Ant.delete {
		fileset(dir:"${basedir}/web-app/WEB-INF/lib") {
				include(name:"grails-*.jar")
		}
	}	
}
task(warPlugins:"Includes the plugins in the WAR") {  
	Ant.sequential {
		mkdir(dir:"${basedir}/web-app/WEB-INF/plugins")
		copy(todir:"${basedir}/web-app/WEB-INF/plugins", failonerror:false) {
			fileset(dir:"${basedir}/plugins")  {
				include(name:"**/grails-app/**")
				exclude(name:"**/grails-app/i18n")				
			}
		}
	}
}

