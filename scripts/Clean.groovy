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

target ('default': "Cleans a Grails project") {
	clean()
	cleanTestReports()
}   

target ( clean: "Implementation of clean") {
    event("CleanStart", [])
    depends(cleanCompiledSources, cleanGrailsApp, cleanWarFile)
    event("CleanEnd", [])
}

target ( cleanCompiledSources : "Cleans compiled Java and Groovy sources") {
	def webInf = "${basedir}/web-app/WEB-INF"
	Ant.delete(dir:"${webInf}/classes")
	Ant.delete(file:webXmlFile.absolutePath, failonerror:false)
	Ant.delete(dir:"${webInf}/lib")
	Ant.delete(dir:classesDirPath)
	Ant.delete(dir:resourcesDirPath)
    Ant.delete(dir:testDirPath)
}

target ( cleanTestReports:"Cleans the test reports") {
	Ant.delete(dir:"${basedir}/test/reports", failonerror:false)    
}
target (cleanGrailsApp : "Cleans the Grails application sources") {
	def appDir = "${basedir}/web-app/WEB-INF/grails-app"
	Ant.delete(dir:appDir)
}

target (cleanWarFile : "Cleans the deployable .war file") {
    def fileName = grailsAppName
    def version = Ant.antProject.properties.'app.version'
    if (version) {
        version = '-'+version
    } else {
        version = ''
    }
    warName = "${basedir}/${fileName}${version}.war"
    Ant.delete(file:warName, failonerror:false)
}
