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
includeTargets << grailsScript("_GrailsPlugins")

target(installPlugin:"Installs a plug-in for the given URL or name and version") {
    depends(checkVersion, parseArguments, configureProxy)
    try {
        def pluginArgs = argsMap['params']

        // fix for Windows-style path with backslashes

        if (pluginArgs) {
            if (argsMap['global']) {
                globalInstall = true
            }

            ant.mkdir(dir:pluginsBase)

            def pluginFile = new File(pluginArgs[0])
            def urlPattern = ~"^[a-zA-Z][a-zA-Z0-9\\-\\.\\+]*://"
            if (pluginArgs[0] =~ urlPattern) {
                def url = new URL(pluginArgs[0])
                doInstallPluginFromURL(url)
            }
            else if (pluginFile.exists() && pluginFile.name.startsWith("grails-") && pluginFile.name.endsWith(".zip")) {
                doInstallPluginZip(pluginFile)
            }
            else {
                // The first argument is the plugin name, the second
                // (if provided) is the plugin version.
                doInstallPlugin(pluginArgs[0], pluginArgs[1])
            }

			event("StatusFinal", ["Plugin installed."])
        }
        else {
            event("StatusError", [ ERROR_MESSAGE])
        }
    }
    catch(Exception e) {
        logError("Error installing plugin: ${e.message}", e)
        exit(1)
    }
}

setDefaultTarget("installPlugin")
