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
 * Gant script that handles the installation of Grails plugins
 * 
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 *
 * @since 0.4
 */
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU  
import groovy.xml.dom.DOMCategory
import groovy.xml.MarkupBuilder

appName = ""

Ant.property(environment:"env")   
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/ListPlugins.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

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
    depends(configureProxy,updatePluginsList)
    def pluginDistName
    def plugin
    use( DOMCategory ) {
        plugin = pluginsList.'plugin'.find{ it.'@name'.toLowerCase() == pluginName.toLowerCase() }
        def release = null
        if( plugin ) {
            pluginRelease = pluginRelease ? pluginRelease : plugin.'@latest-release'
            if( pluginRelease ) {
                release = plugin.'release'.find{ rel -> rel.'@version' == pluginRelease }
                if( release ) {
                    pluginDistName = release.'file'.text()
                } else {
                    event("StatusError", ["Release ${pluginRelease} was not found for this plugin. Type 'grails plugin-info ${pluginName}'"])
                    exit(1)
                }
            } else {
                event("StatusError", ["Latest release information is not available for plugin '${pluginName}', specify concrete release to install"])
                exit(1)
            }
        } else {
            event("StatusError", ["Plugin '${pluginName}' was not found in repository, type 'grails list-plugins'"])
            exit(1)
        }

        def pluginCacheFileName = "${pluginsHome}/${plugin.'@name'}/grails-${plugin.'@name'}-${pluginRelease}.zip"        
        if( !new File(pluginCacheFileName).exists() || pluginRelease.endsWith("SNAPSHOT") ) {
            Ant.mkdir(dir:"${pluginsHome}/${pluginName}")
            Ant.get(dest:pluginCacheFileName,
                src:"${pluginDistName}",
                verbose:true)
        }
    }
}

task(installPlugin:"Implementation task") {
    depends( configureProxy )
    // fix for Windows-style path with backslashes
    def pluginsBase = "${basedir}/plugins".toString().replaceAll('\\\\','/')
	if(args) {      
		def pluginFile = new File(args.trim())
        Ant.mkdir(dir:pluginsBase)
		
		if(args.trim().startsWith("http://")) {
			def url = new URL(args.trim())			
			def slash = url.file.lastIndexOf('/')
            fullPluginName = "${url.file[slash+8..-5]}"
			Ant.get(dest:"${pluginsBase}/grails-${fullPluginName}.zip",
				src:"${url}",
				verbose:true,
				usetimestamp:true)			
		}
        else if( new File(args.trim()).exists() && pluginFile.name.startsWith("grails-") && pluginFile.name.endsWith(".zip" )) {
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

            // for backwards compatability with older plug-ins we need to populate the plug-in resources
            // if they don't exist
            def resourceList = resolveResources("file:${pluginsBase}/${fullPluginName}/grails-app/**/*.groovy")
            def pluginXml = "${pluginsBase}/${fullPluginName}/plugin.xml"
            def xml = new XmlSlurper().parse(new File(pluginXml))
            def resourceElements = xml.resources.resource
            if(resourceElements.size()==0) {
                def writer = new IndentPrinter( new PrintWriter( new FileWriter(pluginXml)))
                def mkp = new MarkupBuilder(writer)
                mkp.plugin(name:xml.@name, version:xml.@version) {
                    resources {
                        for(r in resourceList) {
                             def matcher = r.URL.toString() =~ /\S+?\/grails-app\/\S+?\/(\S+?).groovy/
                             def name = matcher[0][1].replaceAll('/', /\./)
                             resource(name)
                        }
                    }
                }

            }

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


