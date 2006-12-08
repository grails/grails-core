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

package org.codehaus.groovy.grails.cli;
        
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.springframework.core.io.support.PathMatchingResourcePatternResolver  
import gant.Gant
/**
 * Class that handles Grails command line interface for running scripts
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

class GrailsScriptRunner {  
	static final ANT = new AntBuilder() 
	static final RESOLVER  = new PathMatchingResourcePatternResolver()
	    
	static grailsHome
	static baseDir
	
	static main(args) {
                              
		ANT.property(environment:"env")
		grailsHome = ANT.antProject.properties.'env.GRAILS_HOME'
		
		if(!grailsHome) {
			println "Environment variable GRAILS_HOME not set. Please set it to the location of your Grails installation and try again."
			System.exit(0)
		}

		ANT.property(file:"${grailsHome}/build.properties")
		def grailsVersion =  ANT.antProject.properties.'grails.version'
		
		println """
Welcome to Grails ${grailsVersion} - http://grails.org/
Licensed under Apache Standard License 2.0
Grails home is set to: ${grailsHome}		
		"""		  
		if(args.length && args[0].trim()) {         
               

			baseDir = establishBaseDir()
            println "Base Directory: ${baseDir.absolutePath}"
		
			def scriptName
			def allArgs = args[0].trim() 
			      
			allArgs = processSystemArguments(allArgs)
			
			if(allArgs.indexOf(' ') > -1) {                                                                  				
				def tokens = allArgs.trim().split(" ")    
				calculateEnvironment(tokens[0])  
				scriptName = GCU.getNameFromScript(isEnvironmentArgs(tokens[0]) ? tokens[1] : tokens[0])
				if(isEnvironmentArgs(tokens[0]) && tokens.size() > 2) {
					System.setProperty("grails.cli.args", tokens[2..-1].join("\n"))
				}
				else if(!isEnvironmentArgs(tokens[0]) && tokens.size() -1) {
				   System.setProperty("grails.cli.args", tokens[1..-1].join("\n")) 
				}
				
			}   
			else {   
				setDefaultEnvironment(allArgs)
				scriptName = GCU.getNameFromScript(allArgs.trim())
			}                                
			println "Environment set to ${System.getProperty('grails.env')}"
			def scriptFile = new File("${baseDir.absolutePath}/scripts/${scriptName}.groovy")

			System.setProperty("base.dir", baseDir.absolutePath)
			
			try {      
				
				if(scriptFile.exists()) {
					println "Running script ${scriptFile.absolutePath}"
					Gant.main(["-f", scriptFile.absolutePath] as String[])				
				}     
				else {   
					callPluginOrGrailsScript(scriptName)
   				}				
			}
			catch(Throwable t) {
				println "Error executing script ${scriptFile}: ${t.message}"
				t.printStackTrace(System.out)
			}

		}
		else {           
			println "No script name specified. Use 'grails help' for more info"
			System.exit(0)
		}
	}  
	    
	static ENV_ARGS = ["dev", "prod", "test"] 
	private static isEnvironmentArgs(env) {
		ENV_ARGS.contains(env)
	}     
	 
	private static callPluginOrGrailsScript(scriptName) {
	   def potentialScripts = []
		def scriptFile = new File("${grailsHome}/scripts/${scriptName}.groovy") 
		if(scriptFile.exists()) {
			potentialScripts << scriptFile
		}                                                          
		try {
			def pluginScripts = RESOLVER.getResources("file:${baseDir.absolutePath}/plugins/**/scripts/${scriptName}.groovy")
			println "Plugin scripts  $pluginScripts"
			potentialScripts += pluginScripts.collect { it.file }			
		}
		catch(Exception e) {
			println "No plugin scripts found"
		} 
		if(potentialScripts.size()>0) {
            if(potentialScripts.size() == 1) {
				println "Running script ${scriptFile.absolutePath}"
				Gant.main(["-f", potentialScripts[0].absolutePath] as String[])																		
			}                                      
			else {
				println "Multiple options please select:"
				potentialScripts.eachWithIndex { f, i ->
					println "[${i+1}] $f "
				}                     
				ANT.input(message: "Enter #:", addproperty:"grails.script.number")
				def number = ANT.antProject.properties."grails.script.number".toInteger()
				println "Running script ${potentialScripts[number-1].absolutePath}"				
				Gant.main(["-f", potentialScripts[number-1].absolutePath] as String[])																						
			}
		}
		else {
			println "Script $scriptName not found."
		}    		
	}
	
	private static processSystemArguments(allArgs) {
		def lastMatch = null
		allArgs.eachMatch( /-D(.+?)=(.+?)\s+?/ ) { match ->
		   System.setProperty(match[1].trim(),match[2].trim())
		   lastMatch = match[0]
		}
		
		if(lastMatch) {
		   def i = allArgs.lastIndexOf(lastMatch)+lastMatch.size()
		   allArgs = allArgs[i..-1]
		}
		return allArgs
	}   
	   
	private static establishBaseDir() {
        def baseDir = new File("")
        if(!new File(baseDir, "grails-app").exists()) {
        	
        	// be careful with this next step...
        	// baseDir.parentFile will return null since baseDir is new File("")
        	// baseDir.absoluteFile needs to happen before retrieving the parentFile
    		def parentDir = baseDir.absoluteFile.parentFile
    		
    		// keep moving up one directory until we find 
    		// one that contains the grails-app dir or get 
    		// to the top of the filesystem...
    		while(parentDir != null && !new File(parentDir, "grails-app").exists()) {
    			parentDir = parentDir.parentFile
    		}
        	
    		if(parentDir != null) {
    			// if we found the project root, use it
    			baseDir = parentDir
    		}
        }
		return baseDir
	}      
	private static setDefaultEnvironment(args) {
		switch(args.toLowerCase()) {
			case "war":
				System.setProperty("grails.env", "production")					
			break
			case "test-app":
				System.setProperty("grails.env", "test")					
			break
			case "run-webtest":
				System.setProperty("grails.env", "test")					
			break					
			default:
				System.setProperty("grails.env", "development")					
			break
		}        		
	}
	private static calculateEnvironment(env) { 	
		switch(env) {
			case "dev":
				System.setProperty("grails.env", "development")
			break
			case "prod":
				System.setProperty("grails.env", "production")
			break
			case "test":
				System.setProperty("grails.env", "test")
			break	
			default:        
			    System.setProperty("grails.env.default", "true")
				System.setProperty("grails.env", "production")			
			break					
		}
	}   
	
}
