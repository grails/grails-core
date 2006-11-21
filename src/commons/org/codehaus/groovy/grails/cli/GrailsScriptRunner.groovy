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
	
	static main(args) {
                              
		ANT.property(environment:"env")
		def grailsHome = ANT.antProject.properties.'env.GRAILS_HOME'
		
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
		if(args.length) {         
            def baseDir = new File("")  
			def scriptName
			def allArgs = args[0].trim() 
			
			def lastMatch = null
			allArgs.eachMatch( /-D(.+?)=(.+?)\s+?/ ) { match ->
			   System.setProperty(match[1].trim(),match[2].trim())
			   lastMatch = match[0]
			}
			
			if(lastMatch) {
			   def i = allArgs.lastIndexOf(lastMatch)+lastMatch.size()
			   allArgs = allArgs[i..-1]
			}
			
			if(allArgs.indexOf(' ') > -1) {                                                                  				
				def tokens = args[0].split(" ")    
				calculateEnvironment(tokens[0])  
				scriptName = GCU.getNameFromScript(isEnvironmentArgs(tokens[0]) ? tokens[1] : tokens[0])
				if(isEnvironmentArgs(tokens[0]) && tokens.size() > 2) {
					System.setProperty("grails.cli.args", tokens[2..-1].join(";"))
				}
				else if(!isEnvironmentArgs(tokens[0]) && tokens.size() -1) {
				   System.setProperty("grails.cli.args", tokens[1..-1].join(";")) 
				}
				
			}   
			else {   
				setDefaultEnvironment(allArgs)
				scriptName = GCU.getNameFromScript(args[0])
			}                                
			println "Environment set to ${System.getProperty('grails.env')}"

			def scriptFile = new File("scripts/${scriptName}.groovy")

			System.setProperty("base.dir", baseDir.absolutePath)
			
			if(scriptFile.exists()) {
				println "Running script ${scriptFile.absolutePath}"
				Gant.main(["-f", scriptFile.absolutePath] as String[])				
			}     
			else {  
				scriptFile = new File("${grailsHome}/scripts/${scriptName}.groovy") 
				println "Running script ${scriptFile.absolutePath}"
				Gant.main(["-f", scriptFile.absolutePath] as String[])								
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
		println "calcing environment"
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
				System.setProperty("grails.env", "development")			
			break					
		}
	}   
	
}
