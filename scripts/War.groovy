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

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/Clean.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

task ('default': "Creates a WAR archive") {
	depends( clean, packageApp )
	
	Ant.copy(todir:"${basedir}/web-app/WEB-INF/grails-app", overwrite:true) {
		fileset(dir:"${basedir}/grails-app", includes:"**") 
	}       
	def appCtxFile = "${basedir}/web-app/WEB-INF/applicationContext.xml"
	Ant.copy(file:appCtxFile, tofile:"${basedir}/.appctxbck",overwrite:true)
	Ant.replace(file:appCtxFile, 
			token:"classpath:", value:"" ) 
			   
	def fileName = new File(basedir).name
	def warName = "${basedir}/${fileName}.war"
	Ant.jar(destfile:warName, basedir:"${basedir}/web-app")		
	Ant.move(file:"${basedir}/.appctxbck", tofile:appCtxFile, overwrite:true)
	Ant.delete(dir:"${basedir}/web-app/WEB-INF/grails-app")
	
	println "Created WAR at ${warName}"
}

