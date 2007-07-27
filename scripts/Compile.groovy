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
 * Gant script that compiles Groovy and Java files in the src tree
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoaderHolder
import org.codehaus.groovy.grails.compiler.injection.*
import org.springframework.core.io.*
import groovy.text.SimpleTemplateEngine
import org.codehaus.groovy.ant.*
import org.codehaus.groovy.control.*
import java.security.CodeSource

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
				classname : 'org.codehaus.groovy.grails.compiler.GrailsCompiler' , 
				classpathref : 'groovyJarSet' )



task ('default': "Performs compilation on any source files (Java or Groovy) in the 'src' tree") {
	compile()
}            

task(compile : "Implementation of compilation phase") {    
	depends(dependencies, classpath)           
	
    event("CompileStart", ['source'])
    event("StatusUpdate", ["Compiling sources"])

 
	Ant.sequential {
		mkdir(dir:"${basedir}/web-app/WEB-INF/classes") 
		          

		def excludedPaths = ["views", "i18n"]

		def destDir = "${userHome}/.grails/${grailsVersion}/tmp/${baseName}/classes"    		

		mkdir(dir:destDir)
        groovyc(destdir:destDir,
                classpathref:"grails.classpath",
				resourcePattern:"file:${basedir}/grails-app/**/*.groovy") {
            for(dir in new File("${basedir}/grails-app").listFiles()) {
                if(!excludedPaths.contains(dir.name) && dir.isDirectory())
                    src(path:"${dir}")
            }
            src(path:"${basedir}/src/java")
            src(path:"${basedir}/src/groovy")           
        }

        def rootLoader = getClass()
            			    .classLoader
			                .rootLoader

        rootLoader?.addURL(new File(destDir).toURL())

	}
    event("CompileEnd", ['source'])
}


task(compileTests: "Compiles test cases located in src/test") {

	if(new File("${basedir}/src/test").exists()) {
        event("CompileStart", ['tests'])
	    event("StatusUpdate", ["Compiling test cases"])
		depends(classpath)

		Ant.sequential {
			mkdir(dir:"${basedir}/target/test-classes")

			javac(srcdir:"${basedir}/src/test",destdir:"${basedir}/target/test-classes",
					classpathref:"grails.classpath",debug:"on",deprecation:"on", optimize:"off")

			groovyc(srcdir:"${basedir}/src/test",destdir:"${basedir}/target/test-classes",
					classpathref:"grails.classpath")

            deleteAppClasses()
		}
        event("CompileEnd", ['tests'])
	}
}

