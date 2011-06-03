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

import grails.util.BuildSettings
import org.codehaus.groovy.grails.resolve.PluginResolveEngine

/**
 * Gant script that displays info about a given plugin
 *
 * @author Sergey Nebolsin
 *
 * @since 0.5.5
 */

includeTargets << grailsScript("_GrailsPlugins")

def displayHeader = {
     '''
--------------------------------------------------------------------------
Information about Grails plugin
--------------------------------------------------------------------------\
'''
}

def displayPluginInfo = { pluginName, version ->

    BuildSettings settings = grailsSettings
    def pluginResolveEngine = new PluginResolveEngine(settings.dependencyManager, settings)
    pluginXml = pluginResolveEngine.resolvePluginMetadata(pluginName, version)
    if (!pluginXml) {
        event("StatusError", ["Plugin with name '${pluginName}' was not found in the configured repositories"])
        exit 1
    }

    def plugin = pluginXml
    if (plugin == null) {
        event("StatusError", ["Plugin with name '${pluginName}' was not found in the configured repositories"])
        exit 1
    }

    def line = "Name: ${pluginName}"
    line += "\t| Latest release: ${plugin.@version}"
	def sw = new StringWriter()
	def output = new PrintWriter(sw)
	output.println displayHeader()
	output.println line
    output.println '--------------------------------------------------------------------------'
    def release = pluginXml
    if (release) {
        if (release.'title'.text()) {
            output.println "${release.'title'.text()}"
        }
        else {
            output.println "No info about this plugin available"
        }
        output.println '--------------------------------------------------------------------------'
        if (release.'author'.text()) {
            output.println "Author: ${release.'author'.text()}"
            output.println '--------------------------------------------------------------------------'
        }
        if (release.'authorEmail'.text()) {
            output.println "Author's e-mail: ${release.'authorEmail'.text()}"
            output.println '--------------------------------------------------------------------------'
        }
        if (release.'documentation'.text()) {
            output.println "Find more info here: ${release.'documentation'.text()}"
            output.println '--------------------------------------------------------------------------'
        }
        if (release.'description'.text()) {
            output.println "${release.'description'.text()}"
            output.println '--------------------------------------------------------------------------'
        }
    }
    else {
        output.println "<release ${releaseVersion} not found for this plugin>"
        output.println '--------------------------------------------------------------------------'
    }

	println sw.toString()
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
    depends(parseArguments)

    if (argsMap.params) {
        displayHeader()
        def pluginName = argsMap.params[0]
        def version = argsMap.params.size() > 1 ? argsMap.params[1] : null

        displayPluginInfo(pluginName, version)
        displayFooter()
    }
    else {
        event("StatusError", ["Usage: grails plugin-info <plugin-name> [version]"])
    }
}

setDefaultTarget("pluginInfo")
