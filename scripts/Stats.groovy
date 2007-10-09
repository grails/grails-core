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
 * Gant script which generates stats for a Grails project.
 * 
 * @author Glen Smith
 *
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import groovy.text.SimpleTemplateEngine

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )

target ('default': "Generates basic stats for a Grails project") {

	// maps file path to 
	def pathToInfo = [
	      new Expando(name: "Controllers", filetype: ".groovy", path: "controllers"),
	      new Expando(name: "Domain Classes", filetype: ".groovy", path: "domain"),
	      new Expando(name: "Jobs", filetype: ".groovy", path: "jobs"),
	      new Expando(name: "Services", filetype: ".groovy", path: "services"),
	      new Expando(name: "Tag Libraries", filetype: ".groovy", path: "taglib"),
	      new Expando(name: "Groovy Helpers", filetype: ".groovy", path: "src.groovy"),
	      new Expando(name: "Java Helpers", filetype: ".java", path: "src.java"),
	      new Expando(name: "Unit Tests", filetype: ".groovy", path: "test.unit"),
          new Expando(name: "Integration Tests", filetype: ".groovy", path: "test.integration"),	      
	]
	
	
	new File(basedir).eachFileRecurse { file ->
			
		def match = pathToInfo.find { expando -> 
			file.path =~ expando.path && 
			file.path.endsWith(expando.filetype) 
		}
		if (match && file.isFile() ) {
			
			if (file.path.toLowerCase() =~ /web-inf/ || file.path.toLowerCase() =~ /plugins/) {
				// println "Skipping $file.path in WEB-INF or plugins dir"
			} else {
				match.filecount = match.filecount ? match.filecount+1 : 1
				// strip whitespace	
				def loc = file.readLines().findAll { line -> !(line ==~ /^\s*$/) }.size()	
				match.loc = match.loc ? match.loc + loc : loc
			}
		}
	
	}
	
	
	def totalFiles = 0
	def totalLOC = 0

	println '''
	+----------------------+-------+-------+
	| Name                 | Files |  LOC  |
	+----------------------+-------+-------+'''

	
	pathToInfo.each { info ->
	
		if (info.filecount) {
			println "	| " + 
				info.name.padRight(20," ") + " | " + 
				info.filecount.toString().padLeft(5, " ") + " | " +
				info.loc.toString().padLeft(5," ") + " | "
			totalFiles += info.filecount
			totalLOC += info.loc
		}
	
	}
	
	
	println "	+----------------------+-------+-------+"	
	println "	| Totals               | " + totalFiles.toString().padLeft(5, " ") + " | " + totalLOC.toString().padLeft(5, " ") + " | "
	println "	+----------------------+-------+-------+\n"	


}   


