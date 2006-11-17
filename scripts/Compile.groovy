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
 * Gant script that creates a new Grails controller
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import groovy.text.SimpleTemplateEngine

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )     

Ant.path ( id : 'groovyJarSet' ) { 
	fileset ( dir : "${grailsHome}/lib" , includes : "*.jar" ) 
}
Ant.taskdef ( 	name : 'groovyc' , 
				classname : 'org.codehaus.groovy.ant.Groovyc' , 
				classpathref : 'groovyJarSet' )


task ('default': "Performs compilation on any source files (Java or Groovy) in the 'src' tree") {
	compile()
}            

task(compile : "Implementation of compilation phase") { 
	println "Compiling sources.."
	Ant.sequential {                       
		mkdir(dir:"${basedir}/web-app/WEB-INF/classes")
		path(id:"classpath") {
			fileset(dir:"lib")
			fileset(dir:"${grailsHome}/lib")
			fileset(dir:"${grailsHome}/dist")        
			fileset(dir:"${basedir}/web-app/WEB-INF/classes")
		}                                                  
		javac(srcdir:"${basedir}/src/java",destdir:"${basedir}/web-app/WEB-INF/classes",
				classpathref:"classpath",debug:"on",deprecation:"on", optimize:"off")

		groovyc(srcdir:"${basedir}/src/groovy",destdir:"${basedir}/web-app/WEB-INF/classes",
				classpathref:"classpath")
	}
}

