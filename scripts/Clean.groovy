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
 * Gant script that cleans a Grails project
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import groovy.text.SimpleTemplateEngine

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

task ('default': "Cleans a Grails project") {
	clean()
}   

task ( clean: "Implementation of clean") {
	depends( cleanCompiledSources, cleanGrailsApp)
}

task ( cleanCompiledSources : "Cleans compiled Java and Groovy sources") {
	def webInf = "${basedir}/web-app/WEB-INF"
	Ant.delete(dir:"${webInf}/classes")
	Ant.delete(dir:"${webInf}/test/classes", failonerror:false)	
	Ant.delete(dir:"${webInf}/test/reports", failonerror:false)		
	Ant.delete(file:webXmlFile.absolutePath, failonerror:false)
	Ant.delete(dir:"${webInf}/lib")
	Ant.delete(dir:"${classesDirPath}")
	Ant.mkdir(dir:"${webInf}/classes")
	Ant.mkdir(dir:"${webInf}/lib")	
}   

task (cleanGrailsApp : "Cleans the Grails application sources") {
	def appDir = "${basedir}/web-app/WEB-INF/grails-app"
	Ant.delete(dir:appDir)
	Ant.mkdir(dir:appDir)	
}