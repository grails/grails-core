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
import org.codehaus.groovy.grails.resolve.PluginResolveEngine
import grails.util.BuildSettings

/**
 * Gant script that handles the installation of Grails plugins
 *
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsPackage")
includeTargets << grailsScript("_PluginDependencies")

ERROR_MESSAGE = """
You need to specify either the direct URL of the plugin or the name and version
of a distributed Grails plugin found at http://plugins.grails.org
For example:
'grails install-plugin spring-security-core 1.0'
or
'grails install-plugin http://plugins.grails.org/grails-spring-security-core/trunk/grails-spring-security-core-1.0.zip"""

globalInstall = false

target(cachePlugin:"Implementation target") {
    depends(configureProxy)
    fullPluginName = cacheKnownPlugin(pluginName, pluginRelease)
}

target(installDefaultPluginSet:"Installs the default plugin set used by Grails") {
    for (plugin in grailsSettings.defaultPluginMap) {
        def zipName = "grails-${plugin.key}-${plugin.value}"
        def pluginZip = new File("${grailsSettings.grailsHome}/plugins/${zipName}.zip")
        if (!pluginZip.exists()) {
            pluginZip = new File("${grailsSettings.grailsWorkDir}/plugins/${zipName}.zip")
        }
        if (pluginZip.exists()) {
            doInstallPluginZip pluginZip
        }
        else {
            doInstallPlugin plugin.key, plugin.value
        }
    }
}



