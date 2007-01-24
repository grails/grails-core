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
 * Gant script that handles the creation of Grails plugins
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU  
 
DEFAULT_PLUGIN_DIST = "http://dist.codehaus.org/grails-plugins/"            
ERROR_MESSAGE = """
You need to specify either the direct URL of the plugin or the name and version of a distributed Grails plugin found
at $DEFAULT_PLUGIN_DIST. 
For example: 
'grails install-plugin acegi 0.1' 
or 
'grails install-plugin $DEFAULT_PLUGIN_DIST/grails-acegi-0.1.zip"""

appName = ""

Ant.property(environment:"env")   
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )

task ( "default" : "Installs a plug-in for the given URL or name and version") {
   installPlugin()                                                      
}     
                
task(installPlugin:"Implementation task") {   
	def pluginsBase = "${basedir}/plugins"
	if(args) {      
		def pluginFile = new File(args.trim())
		Ant.mkdir(dir:pluginsBase)   		            
		
		if(args.trim().startsWith("http://")) {
			def url = new URL(args.trim())			
			Ant.mkdir(dir:"${basedir}/plugins")   
			                                         
			def slash = url.file.lastIndexOf('/')
			                       

			def file = "${basedir}/plugins/${url.file[slash+8..-1]}"
			println file
			Ant.get(dest:file,
				src:"${url}",
				verbose:true,
				usetimestamp:true)			
		    def dirName =  file[0..-5] 
			Ant.delete(dir:dirName, failonerror:false)
			Ant.mkdir(dir:dirName)
			Ant.unzip(dest:dirName, src:file)
		}  
		else if(pluginFile.exists()) {

			def pluginDir = new File("${pluginsBase}/${pluginFile.name[7..-5]}") 

			Ant.delete(dir:pluginDir, failonerror:false)			
			Ant.mkdir(dir:pluginDir)
			Ant.unzip(dest:pluginDir, src:pluginFile)
		} 
		else if(args.indexOf("\n") > -1) {
			def tokens = args.split("\n") 
			def name = tokens[0].trim()
			def version = tokens[1].trim()
			                         
			def dirName = "grails-${name}-${version}"
			Ant.mkdir(dir:"${basedir}/plugins")
			Ant.get(dest:"${basedir}/plugins/${dirName}.zip",
				src:"${DEFAULT_PLUGIN_DIST}/${dirName}.zip",
				verbose:true,
				usetimestamp:true)                    
			 
			              
			def pluginDir = "${basedir}/plugins/${dirName[7..-1]}"                                 
			Ant.delete(dir:pluginDir, failonerror:false)	 	
			Ant.mkdir(dir:pluginDir)				
			Ant.unzip(dest:pluginDir,
					   src:"${basedir}/plugins/${dirName}.zip")   		
			
		}                             
		else {
			println ERROR_MESSAGE
		}
	}   
	else {
		println ERROR_MESSAGE
	}
}    


