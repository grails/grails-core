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
 * Gant script that displays info about plugin
 *
 * @author Sergey Nebolsin
 *
 * @since 0.5.5
 */
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory


Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

includeTargets << new File ( "${grailsHome}/scripts/ListPlugins.groovy" )

task ( "default" : "Displays info about plugin") {
    pluginInfo()
}

def displayHeader = {
    println '''
--------------------------------------------------------------------------
Information about Grails plugin
--------------------------------------------------------------------------\
'''
}

def clarify = { element, prop ->
    if( element."${prop}" ) return element."${prop}".text()
    else '<no info available>'
}

def displayPluginInfo = { pluginName, version ->
    use(DOMCategory) {
        def plugin = pluginsList.'plugin'.find{ it.'@name' == pluginName }
        if( plugin == null ) {
            event("StatusError", ["Plugin with name '${pluginName}' is not found in Grails repository"])
            System.exit(1)
        } else {
            def line = "Name: ${pluginName}"
            def releaseVersion = null
            if( !version ) {
                releaseVersion = plugin.'@latest-release'
                line += "\t| Latest release: ${releaseVersion ? releaseVersion : '<no info available>'}"
            } else {
                releaseVersion = version
                line += "\t| Release: ${releaseVersion}"
            }
            println line
            println '--------------------------------------------------------------------------'
            if( releaseVersion ) {
                def release = plugin.'release'.find{ rel -> rel.'@version' == releaseVersion }
                if( release ) {
                    println "${clarify(release,'title')}"
                    println '--------------------------------------------------------------------------'
                    println "Author: ${clarify(release,'author')}"
                    println '--------------------------------------------------------------------------'
                    println "Author e-mail: ${clarify(release,'authorEmail')}"
                    println '--------------------------------------------------------------------------'
                    println "${clarify(release,'description')}"
                    println '--------------------------------------------------------------------------'
                } else {
                    println "<release ${releaseVersion} not found for this plugin>"
                    println '--------------------------------------------------------------------------'
                }
           }

            def releases = ""
            plugin.'release'.each {
                releases += " ${it.'@version'}"
            }
            if( releases ) {
                println "Available releases: ${releases}"
            } else {
                println "Available releases: <no releases available for this plugin now>"
            }
        }
    }
}

def displayFullPluginInfo = { pluginName ->
    use(DOMCategory) {
        pluginsList.'plugin'.each { plugin ->
            def pluginLine = plugin.'@name'
            def version = "unknown"
            def title = "No description available"
            if( plugin.'@latest-release' ) {
                version = plugin.'@latest-release'
                def release = plugin.'release'.find{ rel -> rel.'@version' == plugin.'@latest-release' }
                if( release?.'title' ) {
                    title = release?.'title'.text()
                }
            }
            pluginLine += "${spacesFormatter[pluginLine.length()..-1]}<${version}>"
            pluginLine += "\t--\t${title}"
            plugins << pluginLine
        }
    }
}

def displayFooter = {
    println '''
To get info about specific release of plugin 'grails plugin-info [NAME] [VERSION]'

To get list of all plugins type 'grails list-plugins'

To install latest version of plugin type 'grails install-plugin [NAME]'

To install specific version of plugin type 'grails install-plugin [NAME] [VERSION]'

For further info visit http://grails.org/Plugins
'''
}

task(pluginInfo:"Implementation task") {
    if( args ) {
        depends(updatePluginsList)
        displayHeader()
        def arguments = args.split("\n")
        def pluginName = arguments[0]
        def version = arguments.size() > 1 ? arguments[1] : null
        displayPluginInfo( pluginName, version )
        displayFooter()
    } else {
        event("StatusError", "Usage: grails plugin-info <plugin-name> [version]")
    }
}

