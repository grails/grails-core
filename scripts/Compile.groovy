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
includeTargets << new File ( "${grailsHome}/scripts/GetDependencies.groovy" )    
       
if(!Ant.antProject.properties."groovyJarSet") {
	Ant.path ( id : 'groovyJarSet' ) { 
		fileset ( dir : "${grailsHome}/lib" , includes : "*.jar" ) 
	}	
}    
Ant.taskdef ( 	name : 'groovyc' , 
				classname : 'org.codehaus.groovy.ant.Groovyc' , 
				classpathref : 'groovyJarSet' )



task ('default': "Performs compilation on any source files (Java or Groovy) in the 'src' tree") {
	compile()
}            

task(compile : "Implementation of compilation phase") {    
	depends(dependencies, classpath)           
	
	println "Compiling sources..."
	Ant.sequential {
		mkdir(dir:"${basedir}/web-app/WEB-INF/classes") 
		
 		javac(srcdir:"${basedir}/src/java",destdir:"${basedir}/web-app/WEB-INF/classes",
 				classpathref:"grails.classpath",debug:"on",deprecation:"on", optimize:"off")

 		groovyc(srcdir:"${basedir}/src/groovy",destdir:"${basedir}/web-app/WEB-INF/classes",
				classpathref:"grails.classpath")

        deleteAppClasses()
	}
}

task(compileTests: "Compiles test cases located in src/test") {

	if(new File("${basedir}/src/test").exists()) {
		println "Compiling test cases.."
		depends(classpath)

		Ant.sequential {
			mkdir(dir:"${basedir}/target/test-classes")

			javac(srcdir:"${basedir}/src/test",destdir:"${basedir}/target/test-classes",
					classpathref:"grails.classpath",debug:"on",deprecation:"on", optimize:"off")

			groovyc(srcdir:"${basedir}/src/test",destdir:"${basedir}/target/test-classes",
					classpathref:"grails.classpath")

            deleteAppClasses()
		}
		
	}	
}

task(deleteAppClasses: "Delete application classes compiled by groovyc") {
    // Remove classes that were compiled by groovyc but are part of app, so compile time injection can still work
    def grailsDir = resolveResources("file:${basedir}/grails-app/**/*.groovy")

    Ant.delete() {
        fileset( dir: "$basedir/web-app/WEB-INF/classes") {
            grailsDir.each() {
                include(name: (it.file.name - '.groovy') + '.class' )
                include(name: (it.file.name - '.groovy') + '$*.class')
            }
        }
    }
}
