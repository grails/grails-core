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

import groovy.xml.MarkupBuilder
import org.apache.commons.io.FilenameUtils
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.documentation.DocumentationContext
import org.codehaus.groovy.grails.documentation.DocumentedMethod
import org.codehaus.groovy.grails.documentation.DocumentedProperty
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.springframework.core.io.Resource
import grails.util.PluginBuildSettings
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.resolve.GrailsRepoResolver
import org.codehaus.groovy.grails.resolve.PluginInstallEngine
import org.codehaus.groovy.grails.plugins.GrailsPluginManager

/**
 * Plugin stuff. If included, must be included after "_ClasspathAndEvents".
 *
 * @author Graeme Rocher
 *
 * @since 1.1
 */

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

    def installEngine = createPluginInstallEngine()
    installEngine.resolvePluginDependencies()
}

target(initInplacePlugins: "Generates the plugin.xml descriptors for inplace plugins.") {
    depends(classpath)

    // Ensure that the "plugin.xml" is up-to-date for plugins
    // that haven't been installed, i.e. their path is declared
    // explicitly by the project.

    // TODO: At the moment we simple check whether plugin.xml exists for each plugin
    // TODO: otherwise fail
    PluginBuildSettings settings = pluginSettings

    Resource[] pluginDirs = settings.getInlinePluginDirectories()
    pluginDirs.each { Resource r ->
        def dir = r.file
        def pluginXml = new File(dir, "plugin.xml")
        if(!pluginXml.exists()) {
            println "The inplace plugin at [$dir.absolutePath] does not have a plugin.xml. Please run the package-plugin command inside the plugin directory."
            exit 1
        }
    }

}

/**
 * Compiles the sources for a single in-place plugin. A bit of a hack,
 * but any other solution would require too much refactoring to make it
 * into the 1.2 release.
 */
compileInplacePlugin = { File pluginDir ->
    def classesDirPath = grailsSettings.classesDir.path
    ant.mkdir(dir: classesDirPath)

    profile("Compiling inplace plugin sources to location [$classesDirPath]") {
        // First compile the plugins so that we can exclude any
        // classes that might conflict with the project's.
        def classpathId = "grails.compile.classpath"
        def pluginResources = pluginSettings.getPluginSourceFiles(pluginDir)
        
        if (pluginResources) {
            // Only perform the compilation if there are some plugins
            // installed or otherwise referenced.
            try {
                ant.groovyc(
                    destdir: classesDirPath,
                        classpathref: classpathId,
                        encoding:"UTF-8",
                        verbose: grailsSettings.verboseCompile,
                        listfiles: grailsSettings.verboseCompile) {
                    for(File dir in pluginResources.file) {
                        if (dir.exists() && dir.isDirectory()) {
                            src(path: dir.absolutePath)
                        }
                    }
                    exclude(name: "**/BootStrap.groovy")
                    exclude(name: "**/BuildConfig.groovy")
                    exclude(name: "**/Config.groovy")
                    exclude(name: "**/*DataSource.groovy")
                    exclude(name: "**/UrlMappings.groovy")
                    exclude(name: "**/resources.groovy")
                    javac(classpathref: classpathId, encoding: "UTF-8", debug: "yes")
                }
            }
            catch (e) {
                println "Failed to compile plugin at location [$pluginDir] with error: ${e.message}"
                exit 1
            }
        }
    }
}

/**
 * Generates the 'plugin.xml' file for a plugin. Returns an instance
 * of the plugin descriptor.
 */
generatePluginXml = { File descriptor ->
    // Load the plugin descriptor class and instantiate it so we can
    // access its properties.
    Class pluginClass
    def plugin
    try {
        // Rather than compiling the descriptor via Ant, we just load
        // the Groovy file into a GroovyClassLoader. We add the classes
        // directory to the class loader in case it didn't exist before
        // the associated plugin's sources were compiled.
        def gcl = new GroovyClassLoader(classLoader)
        gcl.addURL(grailsSettings.classesDir.toURI().toURL())

        pluginClass = gcl.parseClass(descriptor)
        plugin = pluginClass.newInstance()
    }
    catch (Throwable t) {
        event("StatusError", [ t.message])
        t.printStackTrace(System.out)
        ant.fail("Cannot instantiate plugin file")
    }

    // Work out what the name of the plugin is from the name of the
    // descriptor file.
    pluginName = GrailsNameUtils.getPluginName(descriptor.name)

    // Remove the existing 'plugin.xml' if there is one.
    def pluginXml = new File(descriptor.parentFile, "plugin.xml")
    pluginXml.delete()

    // Use MarkupBuilder with indenting to generate the file.
    def writer = new IndentPrinter(new PrintWriter(new FileWriter(pluginXml)))
    def xml = new MarkupBuilder(writer)

    // Write the content!
    def props = ['author','authorEmail','title','description','documentation']
    def resourceList = pluginSettings.getArtefactResourcesForOne(descriptor.parentFile.absolutePath)

    def rcComparator = [ compare: {a, b -> a.URI.compareTo(b.URI) } ] as Comparator
    Arrays.sort(resourceList, rcComparator)

    pluginGrailsVersion = "${GrailsUtil.grailsVersion} > *"
    if(plugin.metaClass.hasProperty(plugin,"grailsVersion")) {
        pluginGrailsVersion = plugin.grailsVersion
    }

    xml.plugin(name:"${pluginName}",version:"${plugin.version}", grailsVersion:pluginGrailsVersion) {
        props.each {
            if( plugin.properties[it] ) "${it}"(plugin.properties[it])
        }
        resources {
            for(r in resourceList) {
                 def matcher = r.URL.toString() =~ artefactPattern
                 def name = matcher[0][1].replaceAll('/', /\./)
                 resource(name)
            }
        }
        dependencies {        
            if(plugin.metaClass.hasProperty(plugin,'dependsOn')) {
                for(d in plugin.dependsOn) {
                    delegate.plugin(name:d.key, version:d.value)
                }
            }

            IvyDependencyManager dependencyManager = grailsSettings.dependencyManager
            dependencyManager.serialize(delegate, false) 
        }

        def docContext = DocumentationContext.instance
        if(docContext) {
            behavior {
                for(DocumentedMethod m in docContext.methods) {
                    method(name:m.name, artefact:m.artefact, type:m.type?.name) {
                        description m.text
                        if(m.arguments) {
                            for(arg in m.arguments) {
                                argument type:arg.name
                            }
                        }
                    }
                }
                for(DocumentedMethod m in docContext.staticMethods) {
                    'static-method'(name:m.name, artefact:m.artefact, type:m.type?.name) {
                        description m.text
                        if(m.arguments) {
                            for(arg in m.arguments) {
                                argument type:arg.name
                            }
                        }
                    }
                }
                for(DocumentedProperty p in docContext.properties) {
                    property(name:p.name, type:p?.type?.name, artefact:p.artefact) {
                        description p.text
                    }
                }
            }            
        }
    }

    return plugin
}

target(loadPlugins:"Loads Grails' plugins") {
    if(!PluginManagerHolder.pluginManager) { // plugin manager already loaded?
		compConfig.setTargetDirectory(classesDir)
	    def unit = new CompilationUnit ( compConfig , null , new GroovyClassLoader(classLoader) )
		def pluginFiles = pluginSettings.pluginDescriptors

        for(plugin in pluginFiles) {
            def pluginFile = plugin.file
            def className = pluginFile.name - '.groovy'
	        def classFile = new File("${classesDirPath}/${className}.class")

            if(pluginFile.lastModified() > classFile.lastModified()) {
                unit.addSource ( pluginFile )
            }
		}

        try {
            profile("compiling plugins") {
	    		unit.compile ()
			}

			def application
            def pluginClasses = []
            profile("construct plugin manager with ${pluginFiles.inspect()}") {
				for(plugin in pluginFiles) {
				   def className = plugin.file.name - '.groovy'
	               pluginClasses << classLoader.loadClass(className)
				}

                profile("creating plugin manager with classes ${pluginClasses}") {
                    if(grailsApp == null) {
                        grailsApp = new DefaultGrailsApplication(new Class[0], new GroovyClassLoader(classLoader))
                        ApplicationHolder.application = grailsApp
                    }
                    pluginManager = new DefaultGrailsPluginManager(pluginClasses as Class[], grailsApp)

                    PluginManagerHolder.setPluginManager(pluginManager)
                    pluginSettings.pluginManager = pluginManager
                }
	        }
	        profile("loading plugins") {
				event("PluginLoadStart", [pluginManager])
	            pluginManager.loadPlugins()
                def baseDescriptor = pluginSettings.basePluginDescriptor
                if(baseDescriptor) {                    
                    def baseName = FilenameUtils.getBaseName(baseDescriptor.filename)
                    def plugin = pluginManager.getGrailsPluginForClassName(baseName)
                    if(plugin) {
                        plugin.basePlugin = true
                    }
                }
                if(pluginManager.failedLoadPlugins) {
                    event("StatusError", ["Error: The following plugins failed to load due to missing dependencies: ${pluginManager.failedLoadPlugins*.name}"])
                    for(p in pluginManager.failedLoadPlugins) {
                        println "- Plugin: $p.name, Dependencies: $p.dependencyNames"
                    }
                    exit(1)
                }

                pluginManager.doArtefactConfiguration()
                grailsApp.initialise()
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
        // Add the plugin manager to the binding so that it can be accessed
        // from any target.
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
    pluginSettings.pluginXmlMetadata.collect { new XmlSlurper().parse(it.file) }
}

/**
 * @deprecated Use "pluginSettings.pluginXmlMetadata" instead.
 */
getPluginXmlMetadata = {
    pluginSettings.pluginXmlMetadata
}
/**
 * Obtains the directory for the given plugin name
 */
getPluginDirForName = { String pluginName ->
    pluginSettings.getPluginDirForName(pluginName)
}
/**
 * Obtains all of the plugin directories
 * @deprecated Use "pluginSettings.pluginDirectories".
 */
getPluginDirectories = {->
    pluginSettings.pluginDirectories
}
/**
 * Obtains an array of all plugin source files as Spring Resource objects
 * @deprecated Use "pluginSettings.pluginSourceFiles".
 */
getPluginSourceFiles = {
    pluginSettings.pluginSourceFiles
}
/**
 * Obtains an array of all the plugin provides Gant scripts
 * @deprecated Use "pluginSettings.pluginScripts".
 */
getPluginScripts = {
    pluginSettings.pluginScripts
}
/**
 * Gets a list of all scripts known to the application (excluding private scripts starting with _)
 * @deprecated Use "pluginSettings.availableScripts".
 */
getAllScripts = {
    pluginSettings.availableScripts
}
/**
 * Obtains a list of all Grails plugin descriptor classes
 * @deprecated Use "pluginSettings.pluginDescriptors".
 */
getPluginDescriptors = {
    pluginSettings.pluginDescriptors
}
/**
 * Gets the base plugin descriptor
 * @deprecated Use "pluginSettings.basePluginDescriptor".
 */
getBasePluginDescriptor = {
    pluginSettings.basePluginDescriptor
}
/**
 * Runs a script contained within a plugin
 */
runPluginScript = { File scriptFile, fullPluginName, msg ->
    if (scriptFile.exists()) {
        event("StatusUpdate", ["Executing ${fullPluginName} plugin $msg"])
        // instrumenting plugin scripts adding 'pluginBasedir' variable
        def instrumentedInstallScript = "def pluginBasedir = '${pluginsHome}/${fullPluginName}'\n".toString().replaceAll('\\\\','/') + scriptFile.text
        // we are using text form of script here to prevent Gant caching
        includeTargets << instrumentedInstallScript
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
        resetClasspath()
    }
    pluginInstallEngine.isInteractive = isInteractive
    pluginInstallEngine.pluginDirVariableStore = binding
    pluginInstallEngine.pluginScriptRunner = runPluginScript
    return pluginInstallEngine
}

protected GrailsPluginManager resetClasspath() {
    classpathSet = false
    classpath()
    PluginManagerHolder.pluginManager = null
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
        installEngine.installPlugin file, globalInstall
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
    for(resolver in dependencyManager.chainResolver.resolvers) {
        if(resolver instanceof GrailsRepoResolver) {
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