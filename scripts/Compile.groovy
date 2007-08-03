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
	try {
		compile()		
	}   
	catch(Exception e) {
		event("StatusFinal", ["Compilation error: ${e.message}"])
		e.printStackTrace()
		exit(1)
	}
}            
  
compilerClasspath = { testSources -> 

	def excludedPaths = ["views", "i18n"]	
	def pluginResources = resolveResources("file:${basedir}/plugins/*/grails-app/*").toList() +
						  resolveResources("file:${basedir}/plugins/*/src/java").toList() +
						  resolveResources("file:${basedir}/plugins/*/src/groovy").toList() 
	
	for(dir in new File("${basedir}/grails-app").listFiles()) {
	if(!excludedPaths.contains(dir.name) && dir.isDirectory())
	    src(path:"${dir}")
	} 
	for(dir in pluginResources.file) {
	if(!excludedPaths.contains(dir.name) && dir.isDirectory()) {
		src(path:"${dir}")
	}
	}
	src(path:"${basedir}/src/java")
	src(path:"${basedir}/src/groovy")   
	if(testSources) {
         src(path:"${basedir}/test/unit")
         src(path:"${basedir}/test/integration")
	}
}
task(compile : "Implementation of compilation phase") {    
	depends(dependencies, classpath)           
	
    event("CompileStart", ['source'])
	Ant.sequential {
		mkdir(dir:classesDirPath)
        groovyc(destdir:classesDirPath,
                project:baseName,
                classpathref:"grails.classpath",
				resourcePattern:"file:${basedir}/**/grails-app/**/*.groovy",
				compilerClasspath.curry(false))

	}
    event("CompileEnd", ['source'])
}


