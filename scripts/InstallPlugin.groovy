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
import org.codehaus.groovy.grails.resolve.PluginResolveEngine

includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsPackage")
includeTargets << grailsScript("_PluginDependencies")

target(installPlugin:"Installs a plug-in for the given URL or name and version") {
    depends(checkVersion, parseArguments, configureProxy)

    try {
        def pluginArgs = argsMap['params']
        if (!pluginArgs) {
            event("StatusError", [ ERROR_MESSAGE])
            return
        }

        def pluginFile = new File(pluginArgs[0])
        def urlPattern = ~"^[a-zA-Z][a-zA-Z0-9\\-\\.\\+]*://"
        if (pluginArgs[0] =~ urlPattern) {
            grailsConsole.warn """
Since Grails 2.3, it is no longer possible to install plugins directly via a URL.

Upload the plugin to a Maven-compatible repository and declare the dependency in grails-app/conf/BuildConfig.groovy.
"""
            return
        }

        if (pluginFile.exists() && pluginFile.name.startsWith("grails-") && pluginFile.name.endsWith(".zip")) {
            grailsConsole.warn """
Since Grails 2.3, it is no longer possible to install plugins directly from the file sytem.

If you wish to use local plugins then run 'maven-install' in the plugin directory to install the plugin into your local Maven cache.

Then inside your application's grails-app/conf/BuildConfig.groovy file declare the dependency and it will be resolved from you Maven cache.

If you make a change to the plugin simply run 'maven-install' in the directory of the plugin project again and the change will be picked up by the application (if the plugin version ends with -SNAPSHOT)
"""
            return
        }

        // show what to add to BuildConfig.groovy
        def sw = new StringWriter()
        def pluginResolveEngine = new PluginResolveEngine(grailsSettings.dependencyManager, grailsSettings)
        String pluginName = argsMap.params[0]
        String version = argsMap.params.size() > 1 ? argsMap.params[1] : null
        pluginResolveEngine.renderInstallInfo(pluginName, version, sw)
        grailsConsole.warn sw.toString()
    }
    catch(Exception e) {
        logError("Error installing plugin: ${e.message}", e)
        exit(1)
    }
}

setDefaultTarget("installPlugin")
