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

import org.codehaus.groovy.grails.compiler.support.*
import org.codehaus.groovy.grails.project.plugins.GrailsProjectPluginLoader
import org.codehaus.groovy.grails.resolve.GrailsRepoResolver
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.codehaus.groovy.grails.resolve.PluginInstallEngine

/**
 * Plugin stuff. If included, must be included after "_ClasspathAndEvents".
 *
 * @author Graeme Rocher
 *
 * @since 1.1
 */
if (getBinding().variables.containsKey("_plugin_dependencies_called")) return
_plugin_dependencies_called = true

includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsArgParsing")
includeTargets << grailsScript("_GrailsProxy")

// Properties
pluginsList = null
globalInstall = false
pluginsBase = "${grailsWorkDir}/plugins".toString().replaceAll('\\\\','/')

// Targets
target(resolveDependencies:"Resolve plugin dependencies") {
    depends(parseArguments, initInplacePlugins)
    def pluginZips = grailsSettings.pluginDependencies

    def installEngine = createPluginInstallEngine()
    for (zip in pluginZips) {
        installEngine.installResolvedPlugin(zip)
    }

    installEngine.checkPluginsToUninstall()
}

target(initInplacePlugins: "Generates the plugin.xml descriptors for inplace plugins.") {
    depends(classpath)
}

/**
 * Generates the 'plugin.xml' file for a plugin. Returns an instance
 * of the plugin descriptor.
 */
generatePluginXml = { File descriptor, boolean compilePlugin = true ->
    projectPackager.generatePluginXml(descriptor, compilePlugin)
}

target(loadPluginsAsync:"Asynchronously loads plugins") {
    Thread.start {
        loadPlugins()
    }
}

target(loadPlugins:"Loads Grails' plugins") {
    def pluginLoader = new GrailsProjectPluginLoader(grailsApp, classLoader, buildSettings, eventListener)
    pluginManager = pluginLoader.loadPlugins()
    grailsApp = pluginLoader.grailsApplication
}

/**
 * Runs a script contained within a plugin
 */
runPluginScript = { File scriptFile, fullPluginName, msg ->
    if (!scriptFile.exists()) {
        return
    }

    // instrumenting plugin scripts adding 'pluginBasedir' variable
    try {
        def instrumentedInstallScript = "def pluginBasedir = '${pluginsHome}/${fullPluginName}'\n".toString().replaceAll('\\\\','/') + scriptFile.text
        // we are using text form of script here to prevent Gant caching
        includeTargets << instrumentedInstallScript
    }
    catch(e) {
        grailsConsole.error "Error executing plugin $fullPluginName script: $scriptFile"
        exit 1
    }
}

readMetadataFromZip = { String zipLocation, pluginFile=zipLocation ->
    def installEngine = createPluginInstallEngine()
    installEngine.readMetadataFromZip(zipLocation)
}

/**
 * Uninstalls a plugin for the given name and version
 */
uninstallPluginForName = { name, version=null ->
    def pluginInstallEngine = createPluginInstallEngine()
    pluginInstallEngine.uninstallPlugin name, version
}

/**
 * Installs a plugin for the given name and optional version
 */
installPluginForName = { String name, String version = null ->
    PluginInstallEngine pluginInstallEngine = createPluginInstallEngine()
    if (name) {
        event("InstallPluginStart", ["$name-$version"])
        pluginInstallEngine.installPlugin(name, version, globalInstall)
    }
}

private PluginInstallEngine createPluginInstallEngine() {
    def pluginInstallEngine = new PluginInstallEngine(grailsSettings, pluginSettings, metadata, ant)
    pluginInstallEngine.eventHandler = { eventName, msg -> event(eventName, [msg]) }
    pluginInstallEngine.errorHandler = { msg ->
        event("StatusError", [msg])
        for (pluginDir in pluginInstallEngine.installedPlugins) {
            if (pluginInstallEngine.isNotInlinePluginLocation(new File(pluginDir.toString()))) {
                ant.delete(dir: pluginDir, failonerror: false)
            }
        }
        exit(1)
    }
    pluginInstallEngine.postUninstallEvent = {
        resetClasspath()
    }

    pluginInstallEngine.postInstallEvent = { pluginInstallPath ->
        File pluginEvents = new File("${pluginInstallPath}/scripts/_Events.groovy")
        if (pluginEvents.exists()) {
            eventListener.loadEventsScript(pluginEvents)
        }
        try {
            clean()
        }
        catch (e) {
            // ignore
        }
        resetClasspath()
    }
    pluginInstallEngine.isInteractive = isInteractive
    pluginInstallEngine.pluginDirVariableStore = binding
    pluginInstallEngine.pluginScriptRunner = runPluginScript
    return pluginInstallEngine
}

protected void resetClasspath() {
    classpathSet = false
    classpath()
}

doInstallPluginFromURL = { URL url ->
    withPluginInstall {
        def installEngine = createPluginInstallEngine()
        installEngine.installPlugin url, globalInstall
    }
}

doInstallPluginZip = { File file ->
    withPluginInstall {
        def installEngine = createPluginInstallEngine()
        installEngine.installPlugin file, globalInstall, true
    }
}

doInstallPlugin = { pluginName, pluginVersion = null ->
    withPluginInstall {
        def installEngine = createPluginInstallEngine()
        installEngine.installPlugin pluginName, pluginVersion, globalInstall
    }
}

eachRepository = { Closure callable ->
    IvyDependencyManager dependencyManager = grailsSettings.dependencyManager
    for (resolver in dependencyManager.chainResolver.resolvers) {
        if (resolver instanceof GrailsRepoResolver) {
            pluginsList = resolver.getPluginList(new File("${grailsWorkDir}/plugins-list-${resolver.name}.xml"))
            if (pluginsList != null) {
                callable(resolver.name, resolver.repositoryRoot)
            } else {
                grailsConsole.error "An error occurred resolving plugin list from resolver [${resolver.name} - ${resolver.repositoryRoot}]."
            }
        }
    }
}

private withPluginInstall(Closure callable) {
    try {
        fullPluginName = callable.call()
    }
    catch (e) {
        logError("Error installing plugin: ${e.message}", e)
        exit(1)
    }
}
