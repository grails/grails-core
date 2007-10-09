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
 * Gant script that displays info about a given plugin
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

target ( "default" : "Displays info about plugin") {
    pluginInfo()
}

def displayHeader = {
    println '''
--------------------------------------------------------------------------
Information about Grails plugin
--------------------------------------------------------------------------\
'''
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
                def naturalVersion = releaseVersion
                if( ! releaseVersion ) {
                    plugin.'release'.each {
                        if( !releaseVersion || (!"${it.'@version'}".endsWith("SNAPSHOT") && "${it.'@version'}" > releaseVersion )) releaseVersion = "${it.'@version'}"
                    }
                    if( releaseVersion ) naturalVersion = "${releaseVersion} (?)"
                    else naturalVersion = '<no info available>'
                }
                line += "\t| Latest release: ${naturalVersion}"
            } else {
                releaseVersion = version
                line += "\t| Release: ${releaseVersion}"
            }
            println line
            println '--------------------------------------------------------------------------'
            if( releaseVersion ) {
                def release = plugin.'release'.find{ rel -> rel.'@version' == releaseVersion }
                if( release ) {
                    if( release.'title'.text() ) {
                        println "${release.'title'.text()}"
                    } else {
                        println "No info about this plugin available"
                    }
                    println '--------------------------------------------------------------------------'
                    if( release.'author'.text() ) {
                        println "Author: ${release.'author'.text()}"
                        println '--------------------------------------------------------------------------'
                    }
                    if( release.'authorEmail'.text() ) {
                        println "Author's e-mail: ${release.'authorEmail'.text()}"
                        println '--------------------------------------------------------------------------'
                    }
                    if( release.'documentation'.text() ) {
                        println "Find more info here: ${release.'documentation'.text()}"
                        println '--------------------------------------------------------------------------'
                    }
                    if( release.'description'.text() ) {
                        println "${release.'description'.text()}"
                        println '--------------------------------------------------------------------------'
                    }
                } else {
                    println "<release ${releaseVersion} not found for this plugin>"
                    println '--------------------------------------------------------------------------'
                }
           }

            def releases = ""
            plugin.'release'.findAll{ it.'@type' == 'svn'}.each {
                releases += " ${it.'@version'}"
            }
            def zipReleases = ""
            plugin.'release'.findAll{ it.'@type' == 'zip'}.each {
                zipReleases += " ${it.'@version'}"
            }
            if( releases ) {
                println "Available full releases: ${releases}"
            } else {
                println "Available full releases: <no full releases available for this plugin now>"
            }
            if( zipReleases ) {
                println "Available zip releases:  ${zipReleases}"
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

target(pluginInfo:"Implementation target") {
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

