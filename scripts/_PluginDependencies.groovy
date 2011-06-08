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
import grails.util.GrailsNameUtils
import grails.util.GrailsUtil
import grails.util.PluginBuildSettings

import groovy.xml.MarkupBuilder

import org.codehaus.groovy.grails.compiler.support.*
import org.apache.commons.io.FilenameUtils
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.documentation.DocumentationContext
import org.codehaus.groovy.grails.documentation.DocumentedMethod
import org.codehaus.groovy.grails.documentation.DocumentedProperty
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.resolve.GrailsRepoResolver
import org.codehaus.groovy.grails.resolve.PluginInstallEngine
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.ProfilingGrailsPluginManager

import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.plugins.GrailsPlugin
import org.codehaus.groovy.grails.plugins.publishing.PluginDescriptorGenerator

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

    profile("Resolving plugin dependencies") {
        def installEngine = createPluginInstallEngine()
        installEngine.resolvePluginDependencies()
    }
}

target(initInplacePlugins: "Generates the plugin.xml descriptors for inplace plugins.") {
    depends(classpath)
}

/**
 * Generates the 'plugin.xml' file for a plugin. Returns an instance
 * of the plugin descriptor.
 */
/**
 * Generates the 'plugin.xml' file for a plugin. Returns an instance
 * of the plugin descriptor.
 */
generatePluginXml = { File descriptor, boolean compilePlugin = true ->
    def pluginBaseDir = descriptor.parentFile
    def pluginProps = pluginSettings.getPluginInfo(pluginBaseDir.absolutePath)
    def plugin
    pluginGrailsVersion = "${GrailsUtil.grailsVersion} > *"

    if (compilePlugin) {
        try {
            // Rather than compiling the descriptor via Ant, we just load
            // the Groovy file into a GroovyClassLoader. We add the classes
            // directory to the class loader in case it didn't exist before
            // the associated plugin's sources were compiled.
            def gcl = new GroovyClassLoader(classLoader)
            gcl.addURL(grailsSettings.classesDir.toURI().toURL())

            pluginClass = gcl.parseClass(descriptor)
            plugin = pluginClass.newInstance()
            pluginProps = plugin.properties
        }
        catch (Throwable t) {
            event("StatusError", [t.message])
            t.printStackTrace(System.out)
            ant.fail("Cannot instantiate plugin file")
        }
    }

    if (pluginProps != null) {
       if (pluginProps["grailsVersion"]) {
          pluginGrailsVersion = pluginProps["grailsVersion"]
       }
    }
    def resourceList = pluginSettings.getArtefactResourcesForOne(descriptor.parentFile.absolutePath)
    // Work out what the name of the plugin is from the name of the descriptor file.
    pluginName = GrailsNameUtils.getPluginName(descriptor.name)

    // Remove the existing 'plugin.xml' if there is one.
    def pluginXml = new File(pluginBaseDir, "plugin.xml")
    pluginXml.delete()

    // Use MarkupBuilder with indenting to generate the file.
    pluginXml.withWriter { writer ->
        def generator = new PluginDescriptorGenerator(pluginName, resourceList)

        pluginProps["type"] = descriptor.name - '.groovy'
        generator.generatePluginXml(pluginProps, writer)

        return compilePlugin ? plugin : pluginProps
    }
}

target(loadPluginsAsync:"Asynchronously loads plugins") {
    Thread.start {
        loadPlugins()
    }
}
target(loadPlugins:"Loads Grails' plugins") {
    if (!PluginManagerHolder.pluginManager) { // plugin manager already loaded?
        PluginManagerHolder.inCreation = true
        compConfig.setTargetDirectory(pluginClassesDir)
        def unit = new CompilationUnit (compConfig , null , new GroovyClassLoader(classLoader))
        def pluginFiles = pluginSettings.pluginDescriptors

        for (plugin in pluginFiles) {
            def pluginFile = plugin.file
            def className = pluginFile.name - '.groovy'
            def classFile = new File("${classesDirPath}/${className}.class")

            if (pluginFile.lastModified() > classFile.lastModified()) {
                unit.addSource (pluginFile)
            }
        }

        try {
            profile("compiling plugins") {
                unit.compile()
            }

            def application
            def pluginClasses = []
            profile("construct plugin manager with ${pluginFiles.inspect()}") {
                for (plugin in pluginFiles) {
                    def className = plugin.file.name - '.groovy'
                    pluginClasses << classLoader.loadClass(className)
                }

                profile("creating plugin manager with classes ${pluginClasses}") {
                    if (grailsApp == null) {
                        grailsApp = new DefaultGrailsApplication(new Class[0], new GroovyClassLoader(classLoader))
                        ApplicationHolder.application = grailsApp
                    }

                    if (enableProfile) {
                        pluginManager = new ProfilingGrailsPluginManager(pluginClasses as Class[], grailsApp)
                    }
                    else {
                        pluginManager = new DefaultGrailsPluginManager(pluginClasses as Class[], grailsApp)
                    }

                    pluginSettings.pluginManager = pluginManager
                }
            }
            profile("loading plugins") {
                event("PluginLoadStart", [pluginManager])
                pluginManager.loadPlugins()
                PluginManagerHolder.setPluginManager(pluginManager)
                def baseDescriptor = pluginSettings.basePluginDescriptor
                if (baseDescriptor) {
                    def baseName = FilenameUtils.getBaseName(baseDescriptor.filename)
                    def plugin = pluginManager.getGrailsPluginForClassName(baseName)
                    if (plugin) {
                        plugin.basePlugin = true
                    }
                }
                if (pluginManager.failedLoadPlugins) {
                    event("StatusError", ["Error: The following plugins failed to load due to missing dependencies: ${pluginManager.failedLoadPlugins*.name}"])
                    for (p in pluginManager.failedLoadPlugins) {
                        println "- Plugin: $p.name"
                        println "   - Dependencies:"
                        for (depName in p.dependencyNames) {
                            GrailsPlugin depInfo = pluginManager.getGrailsPlugin(depName)
                            def specifiedVersion = p.getDependentVersion(depName)
                            def invalid = depInfo && GrailsPluginUtils.isValidVersion(depInfo.version, specifiedVersion) ? '' : '[INVALID]'
                            println "       ${invalid ? '!' :'-' } ${depName} (Required: ${specifiedVersion}, Found: ${depInfo?.version ?: 'Not Installed'}) ${invalid}"
                        }

                    }
                    exit(1)
                }

                pluginManager.doArtefactConfiguration()
                grailsApp.initialise()

/*                if (org.springframework.util.ClassUtils.isPresent("com.springsource.loaded.Plugins", rootLoader)) {
                    org.codehaus.groovy.grails.cli.agent.GrailsPluginManagerReloadPlugin.register()
                }*/
                event("PluginLoadEnd", [pluginManager])
            }
        }
        catch (Exception e) {
            GrailsUtil.deepSanitize(e).printStackTrace()
            event("StatusFinal", [ "Error loading plugin manager: " + e.message ])
            exit(1)
        }


    }
    else {
        // Add the plugin manager to the binding so that it can be accessed from any target.
        pluginManager = PluginManagerHolder.pluginManager
    }
}

/**
 * Reads a plugin.xml descriptor for the given plugin name
 */
readPluginXmlMetadata = { String pluginName ->
    def pluginDir = getPluginDirForName(pluginName)?.file
    new XmlSlurper().parse(new File("${pluginDir}/plugin.xml"))
}

/**
 * Reads all installed plugin descriptors returning a list
 */
readAllPluginXmlMetadata = {->
    pluginSettings.pluginXmlMetadata.findAll { it.file.exists() }.collect { new XmlSlurper().parse(it.file) }
}


/**
 * Runs a script contained within a plugin
 */
runPluginScript = { File scriptFile, fullPluginName, msg ->
    if (scriptFile.exists()) {
        // instrumenting plugin scripts adding 'pluginBasedir' variable
		try {
	        def instrumentedInstallScript = "def pluginBasedir = '${pluginsHome}/${fullPluginName}'\n".toString().replaceAll('\\\\','/') + scriptFile.text
	        // we are using text form of script here to prevent Gant caching
	        includeTargets << instrumentedInstallScript			
		}
		catch(e) {
			console.error "Error executing plugin $fullPluginName script: $scriptFile"
			exit 1
		}		
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
    PluginManagerHolder.pluginManager = null
    GrailsResourceLoaderHolder.resourceLoader = new GrailsResourceLoader(pluginSettings.getArtefactResources())
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
            callable(resolver.name, resolver.repositoryRoot)
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
