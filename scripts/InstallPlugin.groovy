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
import groovy.xml.dom.DOMCategory
 
appName = ""

Ant.property(environment:"env")   
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/ListPlugins.groovy" )

ERROR_MESSAGE = """
You need to specify either the direct URL of the plugin or the name and version
of a distributed Grails plugin found at ${DEFAULT_PLUGIN_DIST}
For example:
'grails install-plugin acegi 0.1'
or
'grails install-plugin ${BINARY_PLUGIN_DIST}/grails-acegi-0.1.zip"""

task ( "default" : "Installs a plug-in for the given URL or name and version") {
   depends(checkVersion)
   installPlugin()
}     
                
task(cachePlugin:"Implementation task") {
    depends(updatePluginsList)
    def pluginDistName
    use( DOMCategory ) {
        def plugin = pluginsList.'plugin'.find{ it.'@name' == pluginName }
        def release = null
        if( plugin ) {
            pluginRelease = pluginRelease ? pluginRelease : plugin.'@latest-release'
            if( pluginRelease ) {
                release = plugin.'release'.find{ rel -> rel.'@version' == pluginRelease }
                if( release ) {
                    pluginDistName = release.'file'.text()
                } else {
                    event("StatusError", ["Release ${pluginRelease} was not found for this plugin. Type 'grails plugin-info ${pluginName}'"])
                    System.exit(1)
                }
            } else {
                event("StatusError", ["Latest release information is not available for plugin '${pluginName}', specify concrete release to install"])
                System.exit(1)
            }
        } else {
            event("StatusError", ["Plugin '${pluginName}' not found in repository, type 'grails list-plugins'"])
            System.exit(1)
        }
    }
    def pluginCacheFileName = "${pluginsHome}/${pluginName}/grails-${pluginName}-${pluginRelease}.zip"
    if( !new File(pluginCacheFileName).exists() || pluginRelease.endsWith("SNAPSHOT") ) {
        Ant.mkdir(dir:"${pluginsHome}/${pluginName}")
        Ant.get(dest:pluginCacheFileName,
            src:"${pluginDistName}",
            verbose:true)
    }
}

task(installPlugin:"Implementation task") {
    // fix for Windows-style path with backslashes
    def pluginsBase = "${basedir}/plugins".toString().replaceAll(/\\/,'/')
	if(args) {      
		def pluginFile = new File(args.trim())
        Ant.mkdir(dir:pluginsBase)
		
		if(args.trim().startsWith("http://")) {
			def url = new URL(args.trim())			
			def slash = url.file.lastIndexOf('/')
            fullPluginName = "${url.file[slash+8..-5]}"
			Ant.get(dest:"${pluginsBase}/${fullPluginName}.zip",
				src:"${url}",
				verbose:true,
				usetimestamp:true)			
		}
        else if( new File(args.trim()).exists()) {
            fullPluginName = "${pluginFile.name[7..-5]}"
            Ant.copy(file:args.trim(),tofile:"${pluginsBase}/grails-${fullPluginName}.zip")
        }
        else {
            def tokens = args.split("\n")
            pluginName = tokens[0].trim() 
            pluginRelease = tokens.size() > 1 ? tokens[1].trim() : null
            cachePlugin()
            fullPluginName = "${pluginName}-${pluginRelease}"
            Ant.copy(file:"${pluginsHome}/${pluginName}/grails-${fullPluginName}.zip",tofile:"${pluginsBase}/grails-${fullPluginName}.zip")
        }

        if( fullPluginName ) {
            Ant.delete(dir:"${pluginsBase}/${fullPluginName}", failonerror:false)
            Ant.mkdir(dir:"${pluginsBase}/${fullPluginName}")
            Ant.unzip(dest:"${pluginsBase}/${fullPluginName}", src:"${pluginsBase}/grails-${fullPluginName}.zip")

            // proceed _Install.groovy plugin script if exists
            def installScript = new File ( "${pluginsBase}/${fullPluginName}/scripts/_Install.groovy" )
            if( installScript.exists() ) {
                event("StatusUpdate", [ "Executing ${fullPluginName} plugin post-install script"])
                // instrumenting plugin scripts adding 'pluginBasedir' variable
                def instrumentedInstallScript = "def pluginBasedir = '${pluginsBase}/${fullPluginName}'\n" + installScript.text
                // we are using text form of script here to prevent Gant caching
                includeTargets << instrumentedInstallScript
            }
            event("StatusFinal", [ "Plugin ${fullPluginName} installed"])
            event("PluginInstalled", [ fullPluginName ])
        }
    }
	else {
        event("StatusError", [ ERROR_MESSAGE])
	}
}    


