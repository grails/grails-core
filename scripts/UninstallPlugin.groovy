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
 * Gant script that handles the removal of Grails plugins from a project.
 *
 * @author Graeme Rocher
 *
 * @since 1.1
 */
includeTargets << grailsScript("_GrailsPlugins")

includePluginJarsOnClasspath=false

target(uninstallPlugin:"Uninstalls a plug-in for a given name") {
    depends(checkVersion, parseArguments, clean)

    if (argsMap['global']) {
        globalInstall = true
    }

    def pluginArgs = argsMap['params']
    if (pluginArgs) {

        def pluginName = pluginArgs[0]
        def pluginRelease = pluginArgs[1]

        uninstallPluginForName(pluginName, pluginRelease)

        event("PluginUninstalled", ["The plugin ${pluginName}-${pluginRelease} has been uninstalled from the current application"])
    }
    else {
        event("StatusError", ["You need to specify the plug-in name and (optional) version, e.g. \"grails uninstall-plugin feeds 1.0\""])
    }
}

setDefaultTarget("uninstallPlugin")
