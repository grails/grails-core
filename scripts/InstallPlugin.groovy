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
 * @author Sergey Nebolsin
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
   depends(checkVersion)
   installPlugin()
}     
                
task(installPlugin:"Implementation task") {
    // fix for Windows-style path with backslashes
    def pluginsBase = "${basedir}/plugins".toString().replaceAll(/\\/,'/')
	if(args) {      
		def pluginFile = new File(args.trim())
        def pluginName
        Ant.mkdir(dir:pluginsBase)
		
		if(args.trim().startsWith("http://")) {
			def url = new URL(args.trim())			
			def slash = url.file.lastIndexOf('/')
            pluginName = "${url.file[slash+8..-5]}"
            def file = "${pluginsBase}/${pluginName}.zip"
			println file
			Ant.get(dest:file,
				src:"${url}",
				verbose:true,
				usetimestamp:true)			
		}
        else if(args.indexOf("\n") > -1) {
            def tokens = args.split("\n")
            pluginName = tokens[0].trim() + "-" + tokens[1].trim()

            pluginZipName = "grails-${pluginName}"
            Ant.get(dest:"${basedir}/plugins/${pluginZipName}.zip",
                src:"${DEFAULT_PLUGIN_DIST}/${pluginZipName}.zip",
                verbose:true,
                usetimestamp:true)
        }
		else if( new File(args.trim()).exists()) {
            pluginName = "${pluginFile.name[7..-5]}"
            Ant.copy(file:args.trim(),tofile:"${pluginsBase}/grails-${pluginName}.zip")
        }
		else {
    	    event("StatusError", [ ERROR_MESSAGE])
		}

        if( pluginName ) {
            Ant.delete(dir:"${pluginsBase}/${pluginName}", failonerror:false)
            Ant.mkdir(dir:"${pluginsBase}/${pluginName}")
            Ant.unzip(dest:"${pluginsBase}/${pluginName}", src:"${pluginsBase}/grails-${pluginName}.zip")

            // proceed _Install.groovy plugin script if exists
            def installScript = new File ( "${pluginsBase}/${pluginName}/scripts/_Install.groovy" )
            if( installScript.exists() ) {
                event("StatusUpdate", [ "Executing ${pluginName} plugin post-install script"])
                // instrumenting plugin scripts adding 'pluginBasedir' variable
                def instrumentedInstallScript = "def pluginBasedir = '${pluginsBase}/${pluginName}'\n" + installScript.text
                // we are using text form of script here to prevent Gant caching
                includeTargets << instrumentedInstallScript
            }
            event("StatusFinal", [ "Plugin ${pluginName} installed"])
        }
    }
	else {
        event("StatusError", [ ERROR_MESSAGE])
	}
}    


