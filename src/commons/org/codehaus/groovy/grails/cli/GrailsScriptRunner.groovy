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
import org.codehaus.groovy.grails.commons.metaclass.*
import org.springframework.core.io.support.PathMatchingResourcePatternResolver  
import org.codehaus.groovy.runtime.InvokerHelper;
import gant.Gant        
import grails.util.GrailsUtil

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
	static userHome = System.properties.'user.home'       
	static version = GrailsUtil.getGrailsVersion()
	
	static main(String[] args) {
		MetaClassRegistry registry = InvokerHelper
										.getInstance()
										.getMetaRegistry();

		if(!(registry.getMetaClassCreationHandler() instanceof ExpandoMetaClassCreationHandle))
			registry.setMetaClassCreationHandle(new ExpandoMetaClassCreationHandle());
                              
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
		if(args.size() && args[0].trim()) {         
               

			baseDir = establishBaseDir()
            println "Base Directory: ${baseDir.absolutePath}"
		         		    
		    def rootLoader = getClass().classLoader ? getClass().classLoader.rootLoader : Thread.currentThread().getContextClassLoader().rootLoader
		                        
		   /*def appDir = new File("${baseDir.absolutePath}/grails-app")
			Adding the grails-app folders to the root loader causes re-load issues as
            root loader returns old class before the grails GCL attempts to recompile it
			if(appDir.exists()) {
				appDir.eachDir { dir ->
					rootLoader?.addURL(dir.toURL())
				}
			}*/

			def allArgs = args[0].trim()

            allArgs = processSystemArguments(allArgs).trim().split(" ")
            def currentParamIndex = 0
            if( isEnvironmentArgs(allArgs[currentParamIndex]) ) {
                // use first argument as environment name and step further
                calculateEnvironment(allArgs[currentParamIndex++])
            } else {
                // first argument is a script name so check for default environment
                setDefaultEnvironment(allArgs[currentParamIndex])
            }

            if( currentParamIndex >= allArgs.size() ) {
                println "You should specify a script to run. Run 'grails help' for a complete list of available scripts."
                System.exit(0)
            }

            // use current argument as script name and step further
            def paramName = allArgs[currentParamIndex++]
            if (paramName[0] == '-') {
                paramName = paramName[1..-1]
            }
            def scriptName = GCU.getNameFromScript(paramName)

            if( currentParamIndex < allArgs.size() ) {
                // if we have additional params provided - store it in system property
                System.setProperty("grails.cli.args", allArgs[currentParamIndex..-1].join("\n"))
            }

            if(!new File(baseDir.absolutePath, "grails-app").exists() && (!['CreateApp','CreatePlugin','PackagePlugin','Help','ListPlugins','PluginInfo','SetProxy'].contains(scriptName))) {
            	println "${baseDir.absolutePath} does not appear to be part of a Grails application."
            	println "Run 'grails help' for a complete list of available scripts."
            	println 'Exiting.'
            	System.exit(-1)
            }
            
			println "Environment set to ${System.getProperty('grails.env')}"
                                        
			System.setProperty("base.dir", baseDir.absolutePath)
			
			try {      
				callPluginOrGrailsScript(scriptName)
			}
			catch(Throwable t) {
				println "Error executing script ${scriptName}: ${t.message}"
				t.printStackTrace(System.out)
			}

		}
		else {           
			println "No script name specified. Use 'grails help' for more info"
			System.exit(0)
		}
	}  
	    
	static ENV_ARGS = [dev:"development",prod:"production",test:"test"]
    // this map contains default environments for several scripts in form 'script-name':'env-code'
    static DEFAULT_ENVS = ['war': 'prod','test-app':'test','run-webtest':'test']
    private static isEnvironmentArgs(env) {
		ENV_ARGS.keySet().contains(env)
	}
    private static setDefaultEnvironment(args) {
        if(!System.properties."grails.env") {
            def environment = DEFAULT_ENVS[args.toLowerCase()]
            System.setProperty("grails.env", environment ? ENV_ARGS[environment] : ENV_ARGS['dev'] )
        }
    }
    private static calculateEnvironment(env) {
        def environment = ENV_ARGS[env]
        if( environment ) {
            System.setProperty("grails.env", environment)
        } else {
            System.setProperty("grails.env.default", "true")
            System.setProperty("grails.env", ENV_ARGS['prod'] )
        }
    }

	private static callPluginOrGrailsScript(scriptName) {
		def potentialScripts = [] 

		def userHome = ANT.antProject.properties."user.home"
		
		def scriptLocations = ["${baseDir.absolutePath}/scripts", "${grailsHome}/scripts", "${userHome}/.grails/scripts"]
		scriptLocations.each {
			def scriptFile = new File("${it}/${scriptName}.groovy")
			if(scriptFile.exists()) {
				potentialScripts << scriptFile
			}
		}
		                       
		try {
			def pluginScripts = RESOLVER.getResources("file:${baseDir.absolutePath}/plugins/**/scripts/${scriptName}.groovy")
			potentialScripts += pluginScripts.collect { it.file }			
		}
		catch(Exception e) {
			println "Note: No plugin scripts found"
		} 
		if(potentialScripts.size()>0) {  
			potentialScripts = potentialScripts.unique()
            if(potentialScripts.size() == 1) {
				println "Running script ${potentialScripts[0].absolutePath}"
                                                               
				Gant.main(["-c","-d","${userHome}/.grails/${version}/scriptCache","-f", potentialScripts[0].absolutePath] as String[])																		
			}                                      
			else {
				println "Multiple options please select:"  
				def validArgs = []
				potentialScripts.eachWithIndex { f, i ->
					println "[${i+1}] $f "
					validArgs << i+1
				}               
				ANT.input(message: "Enter # ",validargs:validArgs.join(","), addproperty:"grails.script.number")
                def number = ANT.antProject.properties."grails.script.number".toInteger()        

				println "Running script ${potentialScripts[number-1].absolutePath}"				
				Gant.main(["-f", potentialScripts[number-1].absolutePath] as String[])																						
			}
		}
		else {
			println "Script $scriptName not found."
            println "Run 'grails help' for a complete list of available scripts."
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
		def sysProp = System.getProperty("base.dir")
		def baseDir
		if(sysProp) {
			baseDir = sysProp == '.' ? new File("") : new File(sysProp)
		}          
		else {
	        baseDir = new File("")
	        if(!new File(baseDir.absolutePath, "grails-app").exists()) {

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
			
		}
		return baseDir
	}      

}
