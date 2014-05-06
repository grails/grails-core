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

import groovy.xml.dom.DOMCategory
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo

/**
 * Gant script that handles the listing of Grails plugins
 *
 * @author Sergey Nebolsin
 *
 * @since 0.5.5
 */

includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsPackage")
includeTargets << grailsScript("_PluginDependencies")

target(listPlugins: "Implementation target") {
    depends(parseArguments,configureProxy)

    if (argsMap.installed) {
        printInstalledPlugins()
    }
    else {
        pluginsList = grailsSettings.dependencyManager.downloadPluginList(new File(grailsWorkDir, "plugins-list-grailsCentral.xml"))
        printRemotePluginList("grailsCentral")
        printInstalledPlugins()
    }

    println '''
To find more info about plugin type 'grails plugin-info [NAME]'

To install type 'grails install-plugin [NAME] [VERSION]'

For further info visit http://grails.org/Plugins
'''
}

private printInstalledPlugins() {

    def installedPlugins = []
    def pluginInfos = pluginSettings.getPluginInfos()
    for (GrailsPluginInfo info in pluginInfos) {
        installedPlugins << formatPluginForPrint(info.name, info.version, info.title)
    }

    if (installedPlugins) {
        installedPlugins.sort()
        println """
Plug-ins you currently have installed are listed below:
-------------------------------------------------------------
${installedPlugins.join(System.getProperty("line.separator"))}
"""

    }
    else {
        println "You do not have any plugins installed."
    }
}

private printRemotePluginList(name) {

    def plugins = []
    use(DOMCategory) {
        pluginsList?.'plugin'.each {plugin ->

            def version
            def pluginLine = plugin.'@name'
            def versionLine = "<no releases>"
            def title = "No description available"
            if (plugin.'@latest-release') {
                version = plugin.'@latest-release'
                versionLine = "<${version}>"
            }
            else if (plugin.'release'.size() > 0) {
                // determine latest release by comparing version names in lexicografic order
                version = plugin.'release'[0].'@version'
                plugin.'release'.each {
                    if (!"${it.'@version'}".endsWith("SNAPSHOT") && "${it.'@version'}" > version) {
                        version = "${it.'@version'}"
                    }
                }
                versionLine = "<${version} (?)>\t"
            }
            def release = plugin.'release'.find {rel -> rel.'@version' == version}
            if (release?.'title') {
                title = release?.'title'.text()
            }
            plugins << formatPluginForPrint(pluginLine, versionLine, title)
        }
    }

    // Sort plugin descriptions
    if (plugins) {
        plugins.sort()
        println """
Plugins available in the $name repository are listed below:
-------------------------------------------------------------
${plugins.join(System.getProperty("line.separator"))}
"""

    }
    else {
        grailsConsole.error "No plugins found in repository: ${name}. This may be because the repository is behind an HTTP proxy."
    }
}

formatPluginForPrint = { pluginName, pluginVersion, pluginTitle ->
    "${pluginName.toString().padRight(20, " ")}${pluginVersion.toString().padRight(16, " ")} --  ${pluginTitle}"
}

setDefaultTarget("listPlugins")
