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

import grails.util.GrailsNameUtils
import grails.util.GrailsUtil
import groovy.xml.DOMBuilder
import groovy.xml.MarkupBuilder
import groovy.xml.dom.DOMCategory
import java.util.regex.Matcher
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.Transformer
import javax.xml.transform.OutputKeys
import javax.xml.transform.stream.StreamResult
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.springframework.core.io.Resource
import org.w3c.dom.Document
import org.apache.commons.io.FilenameUtils

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

CORE_PLUGIN_DIST = "http://svn.codehaus.org/grails/trunk/grails-plugins"
CORE_PUBLISH_URL = "https://svn.codehaus.org/grails/trunk/grails-plugins"
DEFAULT_PLUGIN_DIST = "http://plugins.grails.org"
DEFAULT_PUBLISH_URL = "https://svn.codehaus.org/grails-plugins"
// TODO: temporary until we refactor Grails core into real plugins
CORE_PLUGINS = ['core', 'i18n','converters','mimeTypes', 'controllers','webflow', 'dataSource', 'domainClass', 'filters','logging', 'groovyPages']


// Properties
pluginsList = null
globalInstall = false
pluginsBase = "${grailsWorkDir}/plugins".toString().replaceAll('\\\\','/')

// setup default plugin respositories for discovery
pluginDiscoveryRepositories = [core:CORE_PLUGIN_DIST, default:DEFAULT_PLUGIN_DIST]
if(grailsSettings?.config?.grails?.plugin?.repos?.discovery) {
    pluginDiscoveryRepositories.putAll(grailsSettings?.config?.grails?.plugin?.repos?.discovery)
}

// setup default plugin respositories for publishing
pluginDistributionRepositories = [core:CORE_PUBLISH_URL, default:DEFAULT_PUBLISH_URL]
if(grailsSettings?.config?.grails?.plugin?.repos?.distribution) {
    pluginDistributionRepositories.putAll(grailsSettings?.config?.grails?.plugin?.repos?.distribution)
}

pluginResolveOrder = grailsSettings?.config?.grails?.plugin?.repos?.resolveOrder
if(!pluginResolveOrder) {
    pluginResolveOrder = pluginDiscoveryRepositories.keySet()
}
else {
    event("StatusUpdate", ["Using explicit plugin resolve order ${pluginResolveOrder}"])
}

installedPlugins = [] // a list of plugins that have been installed

boolean isCorePlugin(name) {
    CORE_PLUGINS.contains(name)
}

configureRepository =  { targetRepoURL, String alias = "default" ->
  repositoryName = alias
  pluginsList = null
  pluginsListFile = new File(grailsSettings.grailsWorkDir, "plugins-list-${alias}.xml")

  def namedPluginSVN = pluginDistributionRepositories.find { it.key == alias }?.value
  if(namedPluginSVN) {
    pluginSVN = namedPluginSVN
  }
  else {
    pluginSVN = DEFAULT_PUBLISH_URL
  }
  pluginDistURL = targetRepoURL
  pluginBinaryDistURL = "$targetRepoURL/dist"
  remotePluginList = "$targetRepoURL/.plugin-meta/plugins-list.xml"
}

configureRepository(DEFAULT_PLUGIN_DIST)

configureRepositoryForName = { String targetRepository, type="discovery" ->
    // Works around a bug in Groovy 1.5.6's DOMCategory that means get on Object returns null. Change to "pluginDiscoveryRepositories.targetRepository" when upgrading
    def targetRepoURL = pluginDiscoveryRepositories.find { it.key == targetRepository }?.value

    if(targetRepoURL) {
      configureRepository(targetRepoURL, targetRepository)
    }
    else {
      println "No repository configured for name ${targetRepository}. Set the 'grails.plugin.repos.${type}.${targetRepository}' variable to the location of the repository."
      exit(1)
    }
}

eachRepository =  { Closure callable ->
    for(repoName in pluginResolveOrder) {
  
       configureRepositoryForName(repoName)
       updatePluginsList()
       if(!callable(repoName, pluginSVN)) {
         break
       }
    }
}
// Targets
target(resolveDependencies:"Resolve plugin dependencies") {
    depends(parseArguments)
    def plugins = metadata.findAll { it.key.startsWith("plugins.")}.collect {
       [
        name:it.key[8..-1],
        version: it.value
       ]
    }
    boolean installedPlugins = false


    for(p in plugins) {
        def name = p.name
        def version = p.version
        def fullName = "$name-$version"
        def pluginLoc = getPluginDirForName(name)
        if(!pluginLoc?.exists()) {
            println "Plugin [${fullName}] not installed, resolving.."

            cacheKnownPlugin(name, version)
            installPluginForName(fullName)
            installedPlugins = true
        }
    }
    if(installedPlugins) {
        resetClasspathAndState()
    }

    // Find out which plugins are in the search path but not in the
    // metadata. We only check on the plugins in the project's "plugins"
    // directory and the global "plugins" dir. Plugins loaded via an
    // explicit path should be left alone.
    def pluginDirs = GrailsPluginUtils.getImplicitPluginDirectories(pluginsHome)
    def pluginsToUninstall = pluginDirs.findAll { Resource r -> !plugins.find { plugin -> r.filename == "$plugin.name-$plugin.version" }}

    for(Resource pluginDir in pluginsToUninstall) {
        if(GrailsPluginUtils.isGlobalPluginLocation(pluginDir)) {
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

private registerMetadataForPluginLocation(Resource pluginDir) {
    def plugin = GrailsPluginUtils.getMetadataForPlugin(pluginDir.filename)
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
        pluginClass = classLoader.loadClass(descriptor.name[0..-8])
        plugin = pluginClass.newInstance()
    }
    catch (Throwable t) {
        event("StatusError", [ t.message])
        t.printStackTrace(System.out)
        ant.fail("Cannot instantiate plugin file")
    }

    // Work out what the name of the plugin is from the class name of
    // the descriptor.
    pluginName = GrailsNameUtils.getScriptName(GrailsNameUtils.getLogicalName(pluginClass, "GrailsPlugin"))

    // Remove the existing 'plugin.xml' if there is one.
    def pluginXml = new File(descriptor.parentFile, "plugin.xml")
    pluginXml.delete()

    // Use MarkupBuilder with indenting to generate the file.
    def writer = new IndentPrinter(new PrintWriter(new FileWriter(pluginXml)))
    def xml = new MarkupBuilder(writer)

    // Write the content!
    def props = ['author','authorEmail','title','description','documentation']
    def resourceList = GrailsPluginUtils.getArtefactResourcesForOne(descriptor.parentFile.absolutePath)
    def pluginGrailsVersion = "${GrailsUtil.grailsVersion} > *"
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
        }
    }

    return plugin
}

target(loadPlugins:"Loads Grails' plugins") {
    if(!PluginManagerHolder.pluginManager) { // plugin manager already loaded?
		compConfig.setTargetDirectory(classesDir)
	    def unit = new CompilationUnit ( compConfig , null , new GroovyClassLoader(classLoader) )
		def pluginFiles = getPluginDescriptors()

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

            // Ensure that the "plugin.xml" is up-to-date for plugins
            // that haven't been installed, i.e. their path is declared
            // explicitly by the project.
            File pluginsDir = grailsSettings.projectPluginsDir.canonicalFile.absoluteFile
            File globalDir = grailsSettings.globalPluginsDir.canonicalFile.absoluteFile

            pluginFiles.findAll { Resource r ->
                File containingDir = r.file.parentFile.parentFile?.canonicalFile?.absoluteFile
                return containingDir == null ||
                        (containingDir != pluginsDir && containingDir != globalDir)
            }.each { Resource r ->
                generatePluginXml(r.file)
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
                    }
                    pluginManager = new DefaultGrailsPluginManager(pluginClasses as Class[], grailsApp)

                    PluginManagerHolder.setPluginManager(pluginManager)
                }
	        }
	        profile("loading plugins") {
				event("PluginLoadStart", [pluginManager])
	            pluginManager.loadPlugins()
                def baseDescriptor = GrailsPluginUtils.getBasePluginDescriptor(grailsSettings.baseDir.absolutePath)
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

target(updatePluginsList:"Updates the plugin list from the remote plugin-list.xml") {
    try {
        println "Reading remote plugin list ..."
        if(!pluginsListFile.exists())
            readRemotePluginList()

        parsePluginList()
        def localRevision = pluginsList ? new Integer(pluginsList.getAttribute('revision')) : -1
        // extract plugins svn repository revision - used for determining cache up-to-date
        def remoteRevision = 0
        new URL(remotePluginList).withReader { Reader reader ->
            def line = reader.readLine()
            while(line) {
                Matcher matcher = line =~ /<plugins revision="(\d+?)">/
                if(matcher) {
                    remoteRevision = matcher.group(1).toInteger()
                    break
                }
                else {
                    line = reader.readLine()
                }

            }
        }
        profile("Updating plugin list if remote list [$remoteRevision] is newer than local ${localRevision}") {
            if (remoteRevision > localRevision) {
                println "Plugin list out-of-date, retrieving.."
                readRemotePluginList()

                // Load up the new plugin list.
                pluginsList = null
                parsePluginList()
            }
        }

    } catch (Exception e) {
        println "Error reading remote plugin list [${e.message}], building locally..."
        updatePluginsListManually()
    }
}


def resetClasspathAndState() {
    GrailsPluginUtils.clearCaches()
    classpathSet = false
    classpath()
    PluginManagerHolder.pluginManager = null
}
def parsePluginList() {
    if(pluginsList == null) {
        profile("Reading local plugin list from $pluginsListFile") {
            def document
            try {
                document = DOMBuilder.parse(new FileReader(pluginsListFile))
            } catch (Exception e) {
                println "Plugin list file corrupt, retrieving again.."
                readRemotePluginList()
                document = DOMBuilder.parse(new FileReader(pluginsListFile))
            }
            pluginsList = document.documentElement
        }
    }
}

def readRemotePluginList() {
    ant.delete(file:pluginsListFile, failonerror:false)
    ant.mkdir(dir:pluginsListFile.parentFile)
    ant.get(src: remotePluginList, dest: pluginsListFile, verbose: "yes",usetimestamp:true)
}

target(updatePluginsListManually: "Updates the plugin list by manually reading each URL, the slow way") {
    depends(configureProxy)
    try {
        def recreateCache = false
        document = null
        if (!pluginsListFile.exists()) {
            println "Plugins list cache doesn't exist creating.."
            recreateCache = true
        }
        else {
            try {
                document = DOMBuilder.parse(new FileReader(pluginsListFile))
            } catch (Exception e) {
                recreateCache = true
                println "Plugins list cache is corrupt [${e.message}]. Re-creating.."
            }
        }
        if (recreateCache) {
            document = DOMBuilder.newInstance().createDocument()
            def root = document.createElement('plugins')
            root.setAttribute('revision', '0')
            document.appendChild(root)
        }

        pluginsList = document.documentElement
        builder = new DOMBuilder(document)

        def localRevision = pluginsList ? new Integer(pluginsList.getAttribute('revision')) : -1
        // extract plugins svn repository revision - used for determining cache up-to-date
        def remoteRevision = 0
        try {
            new URL(pluginDistURL).withReader { Reader reader ->
                def line = reader.readLine()
                line.eachMatch(/Revision (.*):/) {
                     remoteRevision = it[1].toInteger()
                }


                if (remoteRevision > localRevision) {
                //if(true) {
                    // Plugins list cache is expired, need to update
                    event("StatusUpdate", ["Plugins list cache has expired. Updating, please wait"])
                    pluginsList.setAttribute('revision', remoteRevision as String)
                    // for each plugin directory under Grails Plugins SVN in form of 'grails-*'
                    while(line=reader.readLine()) {
                        line.eachMatch(/<li><a href="grails-(.+?)">/) {
                            // extract plugin name
                            def pluginName = it[1][0..-2]


                            // collect information about plugin
                            buildPluginInfo(pluginsList, pluginName)
                        }
                    }
                }
            }

        }
        catch(Exception e) {
            event("StatusError", ["Unable to list plugins, please check you have a valid internet connection: ${e.message}" ])
        }

        try {
          // proceed binary distribution repository (http://plugins.grails.org/dist/
          def binaryPluginsList = new URL(pluginBinaryDistURL).text
          binaryPluginsList.eachMatch(/<a href="grails-(.+?).zip">/) {
              buildBinaryPluginInfo(pluginsList, it[1])
          }
        }
        catch(Exception e) {
           // ignore, binary distributions are supported for backwards compatibility only, so this is ok
        }
        // update plugins list cache file
        writePluginsFile()
    } catch (Exception e) {
        event("StatusError", ["Unable to list plugins, please check you have a valid internet connection: ${e.message}" ])
    }
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
    getPluginXmlMetadata().collect { new XmlSlurper().parse(it.file) }
}

getPluginXmlMetadata = {
    GrailsPluginUtils.getPluginXmlMetadata(pluginsHome, resolveResources)
}
/**
 * Obtains the directory for the given plugin name
 */
getPluginDirForName = { String pluginName ->
    GrailsPluginUtils.getPluginDirForName(pluginsHome, pluginName)
}
/** Obtains all of the plugin directories */
getPluginDirectories = {->
    GrailsPluginUtils.getPluginDirectories(pluginsHome)
}
/**
 * Obtains an array of all plugin source files as Spring Resource objects
 */
getPluginSourceFiles = {
    GrailsPluginUtils.getPluginSourceFiles(pluginsHome, resolveResources)
}
/**
 * Obtains an array of all the plugin provides Gant scripts
 */
getPluginScripts = {
    GrailsPluginUtils.getPluginScripts(pluginsHome,resolveResources)
}
/**
 * Gets a list of all scripts known to the application (excluding private scripts starting with _)
 */
getAllScripts = {
    GrailsPluginUtils.getAvailableScripts(grailsHome,pluginsHome, basedir, resolveResources)
}
/**
 * Obtains a list of all Grails plugin descriptor classes
 */
getPluginDescriptors = {
    GrailsPluginUtils.getPluginDescriptors(basedir,pluginsHome,resolveResources)
}
/**
 * Gets the base plugin descriptor
 */
getBasePluginDescriptor = {
    GrailsPluginUtils.getBasePluginDescriptor(basedir)
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
/**
 * Downloads a remote plugin zip into the plugins dir
 */
downloadRemotePlugin = { url, pluginsBase ->
    def slash = url.file.lastIndexOf('/')
    def fullPluginName = "${url.file[slash + 8..-5]}"
    String zipLocation = "${pluginsBase}/grails-${fullPluginName}.zip"
    ant.get(dest: zipLocation,
            src: "${url}",
            verbose: true,
            usetimestamp: true)

    readMetadataFromZip(zipLocation, url)

    return fullPluginName
}


/**
 * Caches a local plugin into the plugins directory
 */
cacheLocalPlugin = { pluginFile ->
    fullPluginName = "${pluginFile.name[7..-5]}"
    String zipLocation = "${pluginsBase}/grails-${fullPluginName}.zip"
    ant.copy(file: pluginFile, tofile: zipLocation)
    readMetadataFromZip(zipLocation, pluginFile)

    return fullPluginName
}

private readMetadataFromZip(String zipLocation, pluginFile) {
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

/**
 * Searches the downloaded plugin-list.xml files for each repository for a plugin that matches the given name
 */
findPlugin =  { pluginName ->
  pluginName = pluginName?.toLowerCase()
  plugin = null
  if(pluginName) {
    if(!plugin) {
       eachRepository { name, url ->
          plugin = pluginsList.'plugin'.find { it.'@name' == pluginName }
          (!plugin)
       }
    }
    return plugin    
  }
}
/**
 * Stores a plugin from the plugin central repo into the local plugin cache
 */
cacheKnownPlugin = { String pluginName, String pluginRelease ->
    def pluginDistName
    def plugin
    def fullPluginName
    try {
      use(DOMCategory) {
          plugin = findPlugin(pluginName)

          if (plugin) {
              pluginRelease = pluginRelease ? pluginRelease : plugin.'@latest-release'
              if (pluginRelease) {
                  def release = plugin.'release'.find {rel -> rel.'@version' == pluginRelease }
                  if (release) {
                      pluginDistName = release.'file'.text()
                  } else {
                      cleanupPluginInstallAndExit("Release ${pluginRelease} was not found for this plugin. Type 'grails plugin-info ${pluginName}'")
                  }
              } else {
                  cleanupPluginInstallAndExit("Latest release information is not available for plugin '${pluginName}', specify concrete release to install")
              }
          } else {
              cleanupPluginInstallAndExit("Plugin '${pluginName}' was not found in repository. If it is not stored in a configured repository you will need to install it manually. Type 'grails list-plugins' to find out what plugins are available.")
          }


          def pluginCacheFileName = "${pluginsBase}/grails-${plugin.'@name'}-${pluginRelease}.zip"
          if (!new File(pluginCacheFileName).exists() || pluginRelease.endsWith("SNAPSHOT")) {
              ant.mkdir(dir:pluginsBase)
              ant.get(dest: pluginCacheFileName,
                      src: "${pluginDistName}",
                      verbose: true,
                      usetimestamp:true)
          }
          fullPluginName = "$pluginName-$pluginRelease"
          currentPluginName = pluginName
          currentPluginRelease = pluginRelease

          ant.copy(file:"${pluginsBase}/grails-${fullPluginName}.zip",tofile:"${pluginsHome}/grails-${fullPluginName}.zip")
      }
      return fullPluginName
    }
    finally {
       configureRepository(DEFAULT_PLUGIN_DIST)      
    }
}


cleanupPluginInstallAndExit = { message ->
  event("StatusError", [message])
  for(pluginDir in installedPlugins) {
    ant.delete(dir:pluginDir, failonerror:false)
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

        ant.delete(dir:pluginDir, failonerror:true)
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

        Resource currentInstall = GrailsPluginUtils.getPluginDirForName(currentPluginName)

        if(currentInstall?.exists()) {            
            if(!isInteractive || confirmInput("You currently already have a version of the plugin installed [$currentInstall.filename]. Do you want to upgrade this version?", "upgrade.${fullPluginName}.plugin")) {
                ant.delete(dir:currentInstall.file)
            }
            else {
                cleanupPluginInstallAndExit("Plugin $fullPluginName install aborted" );
            }
        }
        installedPlugins << pluginInstallPath
        ant.delete(dir: pluginInstallPath, failonerror: false)
        ant.mkdir(dir: pluginInstallPath)
        ant.unzip(dest: pluginInstallPath, src: "${pluginsBase}/grails-${fullPluginName}.zip")


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
                    def release = cacheKnownPlugin(depDirName, upperVersion == '*' ? null : upperVersion)

                    ant.copy(file:"${pluginsBase}/grails-${release}.zip",tofile:"${pluginsDirPath}/grails-${release}.zip")

                    installPluginForName(release)
                    dependencies.remove(depName)
                }
                else  {
                    def dependency = readPluginXmlMetadata(depDirName)
                    if (!GrailsPluginUtils.isValidVersion(dependency.@version.toString(), depVersion)) {
                        cleanupPluginInstallAndExit("Plug-in requires version [$depVersion] of plugin [$depName], but installed version is [${dependency.version}]. Please upgrade this plugin and try again.")
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
            def pluginJars = resolveResources("file:${pluginInstallPath}/lib/*.jar")
            for(jar in pluginJars) {
                rootLoader.addURL(jar.URL)
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

def registerPluginWithMetadata(String pluginName, pluginVersion) {
    metadata['plugins.' + pluginName] = pluginVersion
    metadata.persist()
}


def buildReleaseInfo(root, pluginName, releasePath, releaseTag ) {
    if (releaseTag == '..' || releaseTag == 'LATEST_RELEASE') return
    def releaseNode = root.'release'.find {it.'@tag' == releaseTag && it.'&type' == 'svn'}
    if (releaseNode) {
        if (releaseTag != 'trunk') return
        else root.removeChild(releaseNode)
    }
    try {
        def properties = ['title', 'author', 'authorEmail', 'description', 'documentation']
        def releaseDescriptor = parseRemoteXML("${releasePath}/${releaseTag}/plugin.xml").documentElement
        def version = releaseDescriptor.'@version'
        if (releaseTag == 'trunk' && !(version.endsWith('SNAPSHOT'))) return
        def releaseContent = new URL("${releasePath}/${releaseTag}/").text
        // we don't want to proceed release if zip distribution for this release is not published
        if (releaseContent.indexOf("grails-${pluginName}-${version}.zip") < 0) return
        releaseNode = builder.createNode('release', [tag: releaseTag, version: version, type: 'svn'])
        root.appendChild(releaseNode)
        properties.each {
            if (releaseDescriptor."${it}") {
                releaseNode.appendChild(builder.createNode(it, releaseDescriptor."${it}".text()))
            }
        }
        releaseNode.appendChild(builder.createNode('file', "${releasePath}/${releaseTag}/grails-${pluginName}-${version}.zip"))
    } catch (Exception e) {
        // no plugin release info available
    }
}



def writePluginsFile() {
    pluginsListFile.parentFile.mkdirs()

    Transformer transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "true")
    transformer.transform(new DOMSource(document), new StreamResult(pluginsListFile))
}

def parseRemoteXML(url) {
    DOMBuilder.parse(new URL(url).openStream().newReader())
}


def buildBinaryPluginInfo(root, pluginName ){
    // split plugin name in form of 'plugin-name-0.1' to name ('plugin-name') and version ('0.1')
    def matcher = (pluginName =~ /^([^\d]+)-(.++)/)
    // convert to new plugin naming convention (MyPlugin -> my-plugin)
    def name = GrailsNameUtils.getScriptName(matcher[0][1])
    def release = matcher[0][2]
    use(DOMCategory) {
        def pluginNode = root.'plugin'.find {it.'@name' == name}
        if (!pluginNode) {
            pluginNode = builder.'plugin'(name: name)
            root.appendChild(pluginNode)
        }
        def releaseNode = pluginNode.'release'.find {it.'@version' == release && it.'@type' == 'zip'}
        // SVN releases have higher precedence than binary releases
        if (pluginNode.'release'.find {it.'@version' == release && it.'@type' == 'svn'}) {
            if (releaseNode) pluginNode.removeChild(releaseNode)
            return
        }
        if (!releaseNode) {
            releaseNode = builder.'release'(type: 'zip', version: release) {
                title("This is a zip release, no info available for it")
                file("${pluginBinaryDistURL}/grails-${pluginName}.zip")
            }
            pluginNode.appendChild(releaseNode)
        }
    }
}


def buildPluginInfo(root, pluginName) {
    use(DOMCategory) {
        def pluginNode = root.'plugin'.find {it.'@name' == pluginName}
        if (!pluginNode) {
            pluginNode = builder.'plugin'(name: pluginName)
            root.appendChild(pluginNode)
        }

        def localRelease = pluginNode.'@latest-release'
        def latestRelease = null
        try {
            new URL("${pluginDistURL}/grails-${pluginName}/tags/LATEST_RELEASE/plugin.xml").withReader {Reader reader ->
                def line = reader.readLine()
                line.eachMatch (/.+?version='(.+?)'.+/) {
                    latestRelease = it[1]
                }
            }
        } catch (Exception e) {
            // ignore
        }

        if(!localRelease || !latestRelease || localRelease != latestRelease) {

            event("StatusUpdate", ["Reading [$pluginName] plugin info"])

            // proceed tagged releases
            try {
                def releaseTagsList = new URL("${pluginDistURL}/grails-${pluginName}/tags/").text
                releaseTagsList.eachMatch(/<li><a href="(.+?)">/) {
                    def releaseTag = it[1][0..-2]
                    buildReleaseInfo(pluginNode, pluginName, "${pluginDistURL}/grails-${pluginName}/tags", releaseTag)
                }
            } catch (Exception e) {
                // no plugin release info available
            }

            // proceed trunk release
            try {
                buildReleaseInfo(pluginNode, pluginName, "${pluginDistURL}/grails-${pluginName}", "trunk")
            } catch (Exception e) {
                // no plugin release info available
            }

            if (latestRelease && pluginNode.'release'.find {it.'@version' == latestRelease}) pluginNode.setAttribute('latest-release', latestRelease as String)
        }
    }
}

