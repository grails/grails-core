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
 * Gant script that evaluates all installed scripts to create help output
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"   

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )
    
class HelpEvaluatingCategory {     
	static defaultTask = ""
	static task(Object obj, Map args, Closure callable) {
		def e = args.find { e -> e.key == "default" }?.value
		if(e) {
			defaultTask = e
		}
	}                     
	static getDefaultTask(Object obj) {
		return defaultTask
	}
	
}

def shouldGenerateHelp =  { List scripts ->
	def countFile = new File("${grailsHome}/scripts/count.tmp")	
	if(!countFile.exists()) {  
		countFile << scripts.size()
		return true
	}                                             
	def count = new GroovyShell().evaluate(countFile) 
	if(count != scripts.size()) {
		countFile.write("${scripts.size()}")
		return true
	}                     
	def helpFile = new File("${grailsHome}/scripts/help.txt")
	if(scripts.find { helpFile.lastModified() < it.lastModified() }) {
		return true
	}	           
	return false
}                                       

task ( 'default' : "Prints out the help for each script") {
	def scripts = []   
    resolveResources("file:${grailsHome}/scripts/**.groovy").each { scripts << it.file }	
	resolveResources("file:${basedir}/scripts/*.groovy").each { scripts << it.file }		
	
	if(new File("${basedir}/plugins").exists()) {	
		resolveResources("file:${basedir}/plugins/*/scripts/*.groovy").each { scripts << it.file }  
	}

	def userHome = Ant.antProject.properties."user.home"
	if(new File("${userHome}/.grails/scripts/").exists()) {
		resolveResources("file:${userHome}/.grails/scripts/*.groovy").each { scripts << it.file }
	}
        
	def helpText = ""
	if(shouldGenerateHelp(scripts)) { 
		println "Generating Help, please wait. This happens when scripts change or the first time you use Grails."		
		  
		def gcl = new GroovyClassLoader()    		
		def sw = new StringWriter()      		
		def pw = new PrintWriter(sw)

		scripts.each { file ->
			use(HelpEvaluatingCategory.class) { 
				try {
					def script = gcl.parseClass(file).newInstance()			
					script.binding = binding
					script.run()

					def scriptName = GCU.getScriptName(file.name)

					pw.println "grails ${scriptName} -- ${getDefaultTask()}"					
				}                                                      
				catch(Throwable t) {
					println "Error creating help for ${file}: ${t.message}"
					t.printStackTrace(System.out)
				}
			}	   
		} 
		helpText = sw.toString()     
		new File("${grailsHome}/scripts/help.txt").write(helpText) 		  		
	}                                                              
	else {
		helpText = new File("${grailsHome}/scripts/help.txt").text
	}

	println """
Usage (optionals marked with *): 
grails [environment]* [target] [arguments]*

Examples: 
grails dev run-app	
grails create-app books

Available Targets:"""
	println helpText
	
} 

