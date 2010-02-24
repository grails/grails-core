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

import java.util.zip.ZipFile
import java.util.zip.ZipEntry

import org.apache.commons.io.FilenameUtils
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.documentation.DocumentationContext
import org.codehaus.groovy.grails.documentation.DocumentedMethod
import org.codehaus.groovy.grails.documentation.DocumentedProperty
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.springframework.core.io.Resource
import grails.util.PluginBuildSettings
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.codehaus.groovy.grails.resolve.GrailsRepoResolver

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




// TODO: temporary until we refactor Grails core into real plugins
CORE_PLUGINS = ['core', 'i18n','converters','mimeTypes', 'controllers','webflow', 'dataSource', 'domainClass', 'filters','logging', 'groovyPages']


// Properties
pluginsList = null
globalInstall = false
pluginsBase = "${grailsWorkDir}/plugins".toString().replaceAll('\\\\','/')


installedPlugins = [] // a list of plugins that have been installed

boolean isCorePlugin(name)  {
    CORE_PLUGINS.contains(name)
}


// Targets
target(resolveDependencies:"Resolve plugin dependencies") {
    depends(parseArguments, initInplacePlugins)

    IvyDependencyManager dependencyManager = grailsSettings.dependencyManager

    def plugins = dependencyManager.pluginDependencyDescriptors.collect { DependencyDescriptor dd ->
       def id = dd.getDependencyRevisionId()
       [
            name:id.name,
            version: id.revision
       ]
    }

    boolean resolveRequired = false

    def pluginsToInstall = []
    for(p in plugins) {
        def name = p.name
        def version = p.version
        def fullName = "$name-$version"
        def pluginLoc = getPluginDirForName(name)
        if(!pluginLoc?.exists()) {
            println "Plugin [${fullName}] not installed."
            pluginsToInstall << name
            resolveRequired = true
        }
        else if(pluginLoc) {
            def dirName = pluginLoc.filename
            PluginBuildSettings settings = pluginSettings
            if(!dirName.endsWith(version) && !settings.isInlinePluginLocation(pluginLoc)) {
                println "Upgrading plugin [$dirName] to [${fullName}]."
                pluginsToInstall << name
                resolveRequired = true
            }
        }

    }
    if(resolveRequired) {
        println "Downloading new plugins. Please wait..."
        ResolveReport report = dependencyManager.resolvePluginDependencies()
        if(report.hasError()) {
            println "Failed to resolve plugins."
            exit 1
        }
        else {
            for(ArtifactDownloadReport ar in report.allArtifactsReports) {
                def arName = ar.artifact.moduleRevisionId.name
                if(pluginsToInstall.contains(arName)) {
                    doInstallPluginZip ar.localFile
                }
            }
            resetClasspathAndState()
        }
    }

    // Find out which plugins are in the search path but not in the
    // metadata. We only check on the plugins in the project's "plugins"
    // directory and the global "plugins" dir. Plugins loaded via an
    // explicit path should be left alone.
    def pluginDirs = pluginSettings.implicitPluginDirectories
    def pluginsToUninstall = pluginDirs.findAll { Resource r -> !plugins.find { plugin ->
        r.filename ==~ "$plugin.name-.+" 
    }}

    for(Resource pluginDir in pluginsToUninstall) {
        if(pluginSettings.isGlobalPluginLocation(pluginDir)) {
            registerMetadataForPluginLocation(pluginDir)
        }
        else {
            if(!isInteractive || confirmInput("Plugin [${pluginDir.filename}] is installed, but was not found in the application's metadata, do you want to uninstall?", "confirm.${pluginDir.filename}.uninstall")) {
                uninstallPluginForName(pluginDir.filename)
            }
            else {
                registerMetadataForPluginLocation(pluginDir)
            }
        }
    }
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
                        encoding:"UTF-8") {
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

private registerMetadataForPluginLocation(Resource pluginDir) {
    def plugin = pluginSettings.getMetadataForPlugin(pluginDir.filename)
    registerPluginWithMetadata(plugin.@name.text(), plugin.@version.text())
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




def resetClasspathAndState() {

    // Update the cached dependencies in grailsSettings, and add new jars to the root loader
    for(type in ['compile', 'build', 'test', 'runtime']) {
        def existing = grailsSettings."${type}Dependencies"
        def all = grailsSettings.dependencyManager.resolveDependencies(IvyDependencyManager."${type.toUpperCase()}_CONFIGURATION").allArtifactsReports.localFile
        def toAdd = all - existing
        if (toAdd) {
            existing.addAll(toAdd)
            if (type in ['build', 'test']) {
                toAdd.each {
                    grailsSettings.rootLoader.addURL(it.toURL())
                }
            }
        }
    }

    pluginSettings.clearCache()
    classpathSet = false
    classpath()
    PluginManagerHolder.pluginManager = null
}

// Utility Closures

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
    def zipFile = new ZipFile(zipLocation)

    ZipEntry entry = zipFile.entries().find {ZipEntry entry -> entry.name == 'plugin.xml'}

    if (entry) {
        pluginXml = new XmlSlurper().parse(zipFile.getInputStream(entry))
        currentPluginName = pluginXml.'@name'.text()
        currentPluginRelease = pluginXml.'@version'.text()
        fullPluginName = "$currentPluginName-$currentPluginRelease"
    }
    else {
        cleanupPluginInstallAndExit("Plug-in $pluginFile is not a valid Grails plugin. No plugin.xml descriptor found!")
    }
}


cleanupPluginInstallAndExit = { message ->
  event("StatusError", [message])
  for(pluginDir in installedPlugins) {
    if (checkPluginPath(pluginDir)) {
        ant.delete(dir:pluginDir, failonerror:false)
    }
  }
  exit(1)
}

/**
 * Uninstalls a plugin for the given name and version
 */
uninstallPluginForName = { name, version=null ->

    String pluginKey = "plugins.$name"
    metadata.remove(pluginKey)
    metadata.persist()


    def pluginDir
    if(name && version) {
        pluginDir = new File("${pluginsHome}/$name-$version")
    }
    else {
        pluginDir = getPluginDirForName(name)?.file
    }
    if(pluginDir?.exists()) {

        def uninstallScript = new File("${pluginDir}/scripts/_Uninstall.groovy")
        runPluginScript(uninstallScript, pluginDir.name, "uninstall script")
        if (checkPluginPath(pluginDir)) {
            ant.delete(dir:pluginDir, failonerror:true)
        }
        resetClasspathAndState()
    }
    else {
        event("StatusError", ["No plugin [$name${version ? '-' + version : ''}] installed, cannot uninstall"])
    }
}
/**
 * Installs a plugin for the given name and optional version
 */
installPluginForName = { String fullPluginName ->

    if (fullPluginName) {
        event("InstallPluginStart", [fullPluginName])
        def pluginInstallPath = "${globalInstall ? globalPluginsDirPath : pluginsHome}/${fullPluginName}"


        def pluginReference = grailsSettings.config.grails.plugin.location[currentPluginName]
        if(pluginReference) {
           cleanupPluginInstallAndExit("""\
Plugin [$currentPluginName] is aliased as [grails.plugin.location.$currentPluginName] to the location [$pluginReference] in grails-app/conf/BuildConfig.groovy.
You cannot upgrade a plugin that is configured via BuildConfig.groovy, remove the configuration to continue.""" ); 
        }

        Resource currentInstall = getPluginDirForName(currentPluginName)
        PluginBuildSettings pluginSettings = pluginSettings
        if(currentInstall?.exists()) {
			if(pluginSettings.isInlinePluginLocation(currentInstall)) {
				cleanupPluginInstallAndExit("The plugin you are trying to install [${fullPluginName}] is already configured as an inplace plugin in grails-app/conf/BuildConfig.groovy. You cannot overwrite inplace plugins." );
			}
			else if(!isInteractive || confirmInput("You currently already have a version of the plugin installed [$currentInstall.filename]. Do you want to upgrade this version?", "upgrade.${fullPluginName}.plugin")) {
                ant.delete(dir:currentInstall.file)
            }
            else {
                cleanupPluginInstallAndExit("Plugin $fullPluginName install aborted" );
            }
        }
        installedPlugins << pluginInstallPath

        if (checkPluginPath(pluginInstallPath)) {
            ant.delete(dir: pluginInstallPath, failonerror: false)
            ant.mkdir(dir: pluginInstallPath)
            ant.unzip(dest: pluginInstallPath, src: "${pluginsBase}/grails-${fullPluginName}.zip")
        }


        def pluginXmlFile = new File("${pluginInstallPath}/plugin.xml")
        if (!pluginXmlFile.exists()) {
            cleanupPluginInstallAndExit("Plugin $fullPluginName is not a valid Grails plugin. No plugin.xml descriptor found!")
        }
        def pluginXml = new XmlSlurper().parse(pluginXmlFile)
        def pluginName = pluginXml.@name.toString()
        def pluginVersion = pluginXml.@version.toString()
        def pluginGrailsVersion = pluginXml.@grailsVersion.toString()
        if(pluginGrailsVersion) {
            if(!GrailsPluginUtils.isValidVersion(GrailsUtil.grailsVersion, pluginGrailsVersion)) {
                cleanupPluginInstallAndExit("Plugin $fullPluginName requires version [${pluginGrailsVersion}] of Grails which your current Grails installation does not meet. Please try install a different version of the plugin or Grails.")
            }
        }

        // Add the plugin's directory to the binding so that any event
        // handlers in the plugin have access to it. Normally, this
        // variable is added in GrailsScriptRunner, but this plugin
        // hasn't been installed by that point.
        binding.setVariable("${GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(pluginName)}PluginDir", new File(pluginInstallPath).absoluteFile)

        def dependencies = [:]

        for (dep in pluginXml.dependencies.plugin) {
            def depName = dep.@name.toString()
            String depVersion = dep.@version.toString()
            if(isCorePlugin(depName)) {
                def grailsVersion = GrailsUtil.getGrailsVersion()
                if(!GrailsPluginUtils.isValidVersion(grailsVersion, depVersion ))
                    cleanupPluginInstallAndExit("Plugin requires version [$depVersion] of Grails core, but installed version is [${grailsVersion}]. Please upgrade your Grails installation and try again.")
            }
            else {
                dependencies[depName] = depVersion
                def depDirName = GrailsNameUtils.getScriptName(depName)
                def depPluginDir = getPluginDirForName(depDirName)?.file
                if (!depPluginDir?.exists()) {
                    event("StatusUpdate", ["Plugin dependency [$depName] not found. Attempting to resolve..."])
                    // recursively install dependent plugins
                    def upperVersion =  GrailsPluginUtils.getUpperVersion(depVersion)
                    def installVersion = upperVersion
                    if(installVersion == '*') {
                        installVersion = grailsSettings.defaultPluginSet.contains(depDirName) ? GrailsUtil.getGrailsVersion() : null
                    }

                    doInstallPlugin(depDirName, installVersion)

                    dependencies.remove(depName)
                }
                else  {
                    def dependency = readPluginXmlMetadata(depDirName)
					def dependencyVersion = dependency.@version.toString()
                    if (!GrailsPluginUtils.isValidVersion(dependencyVersion, depVersion)) {
                        cleanupPluginInstallAndExit("Plugin requires version [$depVersion] of plugin [$depName], but installed version is [${dependencyVersion}]. Please upgrade this plugin and try again.")
                    }
                    else {
                        dependencies.remove(depName)
                    }
                }
            }
        }

        if (dependencies) {
            ant.delete(dir: "${pluginInstallPath}", quiet: true, failOnError: false)
            clean()

            cleanupPluginInstallAndExit("Failed to install plugin [${fullPluginName}]. Missing dependencies: ${dependencies.inspect()}")

        }
        else {
            def pluginDependencyDescriptor = new File("$pluginInstallPath/dependencies.groovy")
            if(pluginDependencyDescriptor.exists()) {
                println "Resolving plugin JAR dependencies"
                BuildSettings settings = grailsSettings
                def callable = settings.pluginDependencyHandler()
                callable.call(new File("$pluginInstallPath"))
                IvyDependencyManager dependencyManager = settings.dependencyManager
                dependencyManager.resetGrailsPluginsResolver()
                def resolveReport = dependencyManager.resolveDependencies(IvyDependencyManager.RUNTIME_CONFIGURATION)
                if(resolveReport.hasError()) {
                    cleanupPluginInstallAndExit("Failed to install plugin [${fullPluginName}]. Plugin has missing JAR dependencies.")
                }
            }
            else {
                def pluginJars = resolveResources("file:${pluginInstallPath}/lib/*.jar")
                for(jar in pluginJars) {
                    rootLoader.addURL(jar.URL)
                }
            }

            // proceed _Install.groovy plugin script if exists
            def installScript = new File("${pluginInstallPath}/scripts/_Install.groovy")
            runPluginScript(installScript, fullPluginName, "post-install script")

            registerPluginWithMetadata(pluginName, pluginVersion)


            def providedScripts = resolveResources("file:${pluginInstallPath}/scripts/*.groovy").findAll { !it.filename.startsWith('_')}
            event("StatusFinal", ["Plugin ${fullPluginName} installed"])
            if (providedScripts) {
                println "Plug-in provides the following new scripts:"
                println "------------------------------------------"
                providedScripts.file.each {file ->
                    def scriptName = GrailsNameUtils.getScriptName(file.name)
                    println "grails ${scriptName}"
                }
            }

            File pluginEvents = new File("${pluginInstallPath}/scripts/_Events.groovy")
            if (pluginEvents.exists()) {
                println "Found events script in plugin ${pluginName}"
                eventListener.loadEventsScript(pluginEvents)
            }
            
            event("PluginInstalled", [fullPluginName])
        }

    }
}



doInstallPluginFromURL = { URL url ->
    withPluginInstall {
        cacheRemotePlugin(url, pluginsBase)
    }
}

doInstallPluginZip = { File file ->
    withPluginInstall {
        cacheLocalPlugin(file)
    }
}

doInstallPlugin = { pluginName, pluginVersion = null ->
    withPluginInstall {
        File pluginZip = resolvePluginZip(pluginName, pluginVersion)
        if(pluginZip) doInstallPluginZip pluginZip
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

resolvePluginZip = {pluginName, pluginVersion ->
        IvyDependencyManager dependencyManager = grailsSettings.dependencyManager
        def (group, name) = pluginName.contains(":") ? pluginName.split(":") : ['org.grails.plugins', pluginName]
        def resolveArgs = [name:name, group:group]
        if(pluginVersion) resolveArgs.version = pluginVersion
        else resolveArgs.version = "latest.integration"

        dependencyManager.parseDependencies {
            plugins {
                runtime resolveArgs
            }
        }

        println "Resolving plugin ${pluginName}. Please wait..."
        println()
        def report = dependencyManager.resolvePluginDependencies("")
        if(report.hasError()) {
            println "Error resolving plugin ${resolveArgs}."
            exit 1
        }
        else {
            def artifactReport = report.allArtifactsReports.find { it.artifact.name == pluginName && (pluginVersion == null || it.artifact.moduleRevisionId.revision == pluginVersion) }
            if(artifactReport) {
                return artifactReport.localFile
            }
            else {
                println "Error resolving plugin ${resolveArgs}. Plugin not found."
                exit 1
            }
        }
}


/**
 * Caches a local plugin into the plugins directory
 */
cacheLocalPlugin = { pluginFile ->
	def fileName = pluginFile.name
    fullPluginName = fileName.startsWith("grails-") ? "${pluginFile.name[7..-5]}" : fileName[0..-5]
    String zipLocation = "${pluginsBase}/grails-${fullPluginName}.zip"
    ant.copy(file: pluginFile, tofile: zipLocation)
    readMetadataFromZip(zipLocation, pluginFile)

    return fullPluginName
}

cacheRemotePlugin = { url, cachePath ->
    def s = url.toString()
    def filename = s[s.lastIndexOf("/")..-1]
    def local = "${cachePath}/$filename"
    ant.get(src: url, dest: local, verbose:"on",usetimestamp:true)
    readMetadataFromZip(local, local)
    return fullPluginName
}

File findZip(String name, String dir) {
    new File(dir).listFiles().find {it.name.startsWith(name)}
}

private withPluginInstall(Closure callable) {
    try {
        fullPluginName = callable.call()
        completePluginInstall(fullPluginName)
    }
    catch (e) {
        logError("Error installing plugin: ${e.message}", e)
        exit(1)
    }
}


private completePluginInstall (fullPluginName) {
    if(fullPluginName) {        
        classpath ()
        println "Installing plugin $fullPluginName"
        installPluginForName(fullPluginName)
    }
}



def registerPluginWithMetadata(String pluginName, pluginVersion) {
    metadata['plugins.' + pluginName] = pluginVersion
    metadata.persist()
}


/**
 * Check to see if the plugin directory is in plugins home.
 */
checkPluginPath = { pluginDir ->
  // insure all the directory is in the pluginsHome
  def absPluginsHome = new File(pluginsHome).absolutePath
  if(pluginDir instanceof File) {
	pluginDir.absolutePath.startsWith(absPluginsHome)
  }
  else {
	new File(pluginDir).absolutePath.startsWith(absPluginsHome)
  }
  
}

