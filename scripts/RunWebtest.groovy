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
 * Gant script that executes Grails using an embedded Jetty server
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.mortbay.jetty.*
import org.mortbay.http.*


Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"  

includeTargets << new File ( "${grailsHome}/scripts/RunApp.groovy" )  

task ('default': "Run's all of the Web tests against a Grails application") { 
	depends( runApp )
    runWebTest()                
	stopServer()
}                 
task ( runWebTest : "Main implementation that executes a Grails' Web tests") {
	Ant.java(classname:"groovy.ui.GroovyMain", failonerror:true) {
		arg(line:"${basedir}/webtest/tests/TestSuite")
		classpath {
			pathelement(location:"${basedir}/webtest/tests")
			pathelement(location:"${grailsHome}/downloads/webtest/lib")
			fileset(dir:"${grailsHome}/downloads/webtest/lib",
					includes:"**/*.jar",
					excludes:"**/groovy-*") // avoid conflict with current runtime				   			
		}
	}
}    
