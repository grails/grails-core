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
import org.springframework.util.Assert

appName = ""

Ant.property(environment:"env")   
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/ListPlugins.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/Clean.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

ERROR_MESSAGE = """
You need to specify either the direct URL of the plugin or the name and version
of a distributed Grails plugin found at ${DEFAULT_PLUGIN_DIST}
For example:
'grails install-plugin acegi 0.1'
or
'grails install-plugin ${BINARY_PLUGIN_DIST}/grails-acegi-0.1.zip"""

target ( "default" : "Installs a plug-in for the given URL or name and version") {
   depends(checkVersion)

   installPlugin() 
}     
                
target(cachePlugin:"Implementation target") {
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

target(installPlugin:"Implementation target") {
    depends( configureProxy )
    try {
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
                event("InstallPluginStart", [fullPluginName])
                Ant.delete(dir:"${pluginsBase}/${fullPluginName}", failonerror:false)
                Ant.mkdir(dir:"${pluginsBase}/${fullPluginName}")
                Ant.unzip(dest:"${pluginsBase}/${fullPluginName}", src:"${pluginsBase}/grails-${fullPluginName}.zip")

                // for backwards compatability with older plug-ins we need to populate the plug-in resources
                // if they don't exist
                def resourceList = resolveResources("file:${pluginsBase}/${fullPluginName}/grails-app/**/*.groovy")
                def pluginXml = "${pluginsBase}/${fullPluginName}/plugin.xml"
                def xml = new XmlSlurper().parse(new File(pluginXml))
                def pluginVersion = xml.@version.text()
                def pluginName = xml.@name.text()

                def resourceElements = xml.resources.resource
                if(resourceElements.size()==0) {
                    def writer = new IndentPrinter( new PrintWriter( new FileWriter(pluginXml)))
                    def mkp = new MarkupBuilder(writer)
                    mkp.plugin(name:pluginName, version:pluginVersion) {
                        resources {
                            for(r in resourceList) {
                                 def matcher = r.URL.toString() =~ artefactPattern
                                 def name = matcher[0][1].replaceAll('/', /\./)
                                 resource(name)
                            }
                        }
                    }

                }

                // Add the plugin's directory to the binding so that any event
                // handlers in the plugin have access to it. Normally, this
                // variable is added in GrailsScriptRunner, but this plugin
                // hasn't been installed by that point.
                binding.setVariable("${pluginName}PluginDir", new File("${pluginsBase}/${fullPluginName}").absoluteFile)

                event("StatusUpdate", [ "Compiling plugin ${fullPluginName} ..."])
                // reset the classpath so that plug-in is recognised
                classpathSet = false
                classpath()
                loadEventHooks()
                // add any new plugin provided jars to the classpath
                def newJars = resolveResources("file:${pluginsBase}/${fullPluginName}/lib/*.jar")
                for(jar in newJars) {
                    rootLoader.addURL(jar.URL)
                }
                compile()

                packagePlugins()
                loadPlugins()

                if(!pluginManager.hasGrailsPlugin(pluginName)) {
                    Ant.delete(dir:"${pluginsBase}/${fullPluginName}", quiet:true, failOnError:false)
                    clean()
                    def plugin = pluginManager.getFailedPlugin(pluginName)

                    Assert.notNull plugin, "Grails Bug: If the plugin wasn't loaded it should be in the failed plugins list, but is not. Please report the issue."

                    println "Failed to install plug-in [${fullPluginName}]. Missing dependencies: ${plugin.dependencyNames.inspect()}"
                    event("PluginInstallFailed", [ "Plugin ${fullPluginName} failed to install"])
                }
                else {
                    // proceed _Install.groovy plugin script if exists
                    def installScript = new File ( "${pluginsBase}/${fullPluginName}/scripts/_Install.groovy" )
                    if( installScript.exists() ) {
                        event("StatusUpdate", [ "Executing ${fullPluginName} plugin post-install script"])
                        // instrumenting plugin scripts adding 'pluginBasedir' variable
                        def instrumentedInstallScript = "def pluginBasedir = '${pluginsBase}/${fullPluginName}'\n" + installScript.text
                        // we are using text form of script here to prevent Gant caching
                        includeTargets << instrumentedInstallScript
                    }
                    def providedScripts = resolveResources("file:${pluginsBase}/${fullPluginName}/scripts/*.groovy").findAll { !it.filename.startsWith('_')}
                    event("StatusFinal", [ "Plugin ${fullPluginName} installed"])
                    if(providedScripts) {
                        println "Plug-in provides the following new scripts:"
                        println "------------------------------------------"
                        providedScripts.file.each { file ->
                            def scriptName = GCU.getScriptName(file.name)
                            println "grails ${scriptName}"
                        }
                    }

                    event("PluginInstalled", [ fullPluginName ])
                }

            }
        }
        else {
            event("StatusError", [ ERROR_MESSAGE])
        }        
    }
    catch(Exception e) {
        println e.message
        e.printStackTrace()
    }
}
