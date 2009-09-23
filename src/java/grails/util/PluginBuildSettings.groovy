/* Copyright 2004-2005 Graeme Rocher
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

package grails.util

import grails.util.BuildScope
import grails.util.BuildSettings
import grails.util.Environment
import java.util.concurrent.ConcurrentHashMap
import org.apache.commons.lang.ArrayUtils
import org.codehaus.groovy.grails.plugins.GrailsPlugin
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginInfo
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import groovy.util.slurpersupport.GPathResult

/**
 * A class that uses the project BuildSettings object to discover information
 * about the installed plugin such as the jar files they provide, the plugin descriptors
 * and so on.
 *
 * @author Graeme Rocher
 * @since 1.2
 */

class PluginBuildSettings {

    private static final PathMatchingResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver()

   /**
     * A default resolve used if none is specified to the resource resolving methods in this class
     */
    Closure resourceResolver  = { pattern ->
        try {
                return RESOLVER.getResources(pattern)
            }
            catch(Throwable e) {
                 return []  as Resource[]
            }

    }
    
    BuildSettings buildSettings
    GrailsPluginManager pluginManager
    private String pluginDirPath
    private Map cache = new ConcurrentHashMap()
    private Map pluginToDirNameMap = new ConcurrentHashMap()
    private Map pluginMetaDataMap = new ConcurrentHashMap()
    private pluginLocations
    
    PluginBuildSettings(BuildSettings buildSettings) {
        this(buildSettings, null)
    }

    PluginBuildSettings(BuildSettings buildSettings, GrailsPluginManager pluginManager) {
        this.buildSettings = buildSettings
        this.pluginManager = pluginManager
        this.pluginDirPath = buildSettings.getProjectPluginsDir().absolutePath
        this.pluginLocations = buildSettings?.config?.grails?.plugin?.location
    }



    /**
     * Clears any cached entries
     */
    void clearCache() {
        this.cache.clear()
        this.pluginToDirNameMap.clear()
        this.pluginMetaDataMap.clear()
    }

   /**
     * Returns an array of PluginInfo objects
     */
    PluginInfo[] getPluginInfos(String pluginDirPath=this.pluginDirPath) {
        def pluginInfos = []
        for(dir in getPluginDirectories()) {
            pluginInfos << new PluginInfo(dir, this)
        }
        return pluginInfos as PluginInfo[]
    }


    /**
     * Obtains a Resource array of the Plugin metadata XML files used to describe the plugins provided resources
     */
    Resource[] getPluginXmlMetadata() {
        def allPluginXmlMetadata = cache['allPluginXmlMetadata']
        if(!allPluginXmlMetadata) {
            allPluginXmlMetadata = new Resource[0]
            allPluginXmlMetadata = resolvePluginResourcesAndAdd(allPluginXmlMetadata, pluginDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/plugin.xml")
            }
            cache['allPluginXmlMetadata'] = allPluginXmlMetadata
        }
        return allPluginXmlMetadata
    }

    /**
     * Returns XML about the plugin
     */
    GPathResult getMetadataForPlugin(String pluginName) {
        if(pluginMetaDataMap[pluginName]) return pluginMetaDataMap[pluginName]
        Resource pluginDir = getPluginDirForName(pluginName)
        GPathResult result = getMetadataForPlugin(pluginDir)
        pluginMetaDataMap[pluginName] = result
        return result
    }

    /**
     * Returns XML metadata for the plugin
     */    
    GPathResult getMetadataForPlugin(Resource pluginDir) {
        try {
            GPathResult result = new XmlSlurper().parse(new File("$pluginDir.file.absolutePath/plugin.xml"))
            return result
        }
        catch (e) {
            return null;
        }
    }


    /**
     * Obtains an array of all Gant scripts that are availabe for execution in a Grails application
     */
    Resource[] getAvailableScripts() {

        def availableScripts = cache['availableScripts']
        if(!availableScripts) {

            def scripts = []
            def userHome = System.getProperty("user.home")
            def grailsHome = buildSettings.grailsHome.absolutePath
            def basedir = buildSettings.baseDir.absolutePath
            resourceResolver("file:${grailsHome}/scripts/**.groovy").each { if (!it.file.name.startsWith('_')) scripts << it }
            resourceResolver("file:${basedir}/scripts/*.groovy").each { if (!it.file.name.startsWith('_')) scripts << it }
            pluginScripts.each { if (!it.file.name.startsWith('_')) scripts << it }
            resourceResolver("file:${userHome}/.grails/scripts/*.groovy").each { if (!it.file.name.startsWith('_')) scripts << it }
            availableScripts = scripts as Resource[]
            cache['availableScripts'] = availableScripts
        }
        return availableScripts
    }

    /**
     * Obtains an array of plug-in provided Gant scripts available to a Grails application
     */
    Resource[] getPluginScripts() {
        def pluginScripts = cache['pluginScripts']
        if(!pluginScripts) {
            pluginScripts = new Resource[0]
            pluginScripts = resolvePluginResourcesAndAdd(pluginScripts, pluginDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/scripts/*.groovy")
            }
            cache['pluginScripts'] = pluginScripts
        }
        return pluginScripts
    }

    /**
     * Obtains an array of all plugin provided resource bundles
     */
    Resource[] getPluginResourceBundles() {
        def pluginResourceBundles = cache['pluginResourceBundles']
        if(!pluginResourceBundles) {
            pluginResourceBundles = new Resource[0]
            pluginResourceBundles = resolvePluginResourcesAndAdd(pluginResourceBundles,pluginDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/grails-app/i18n/**/*.properties")
            }

            cache['pluginResourceBundles'] = pluginResourceBundles
        }
        return pluginResourceBundles
    }

    /**
     * Obtains an array of all plug-in provided source files (Java and Groovy)
     */

    Resource[] getPluginSourceFiles() {
        def sourceFiles = cache['sourceFiles']
        if(!sourceFiles) {
            sourceFiles = new Resource[0]
            sourceFiles = resolvePluginResourcesAndAdd(sourceFiles, pluginDirPath) { pluginDir ->
                Resource[] pluginSourceFiles = resourceResolver("file:${pluginDir}/grails-app/*")
                pluginSourceFiles = ArrayUtils.addAll(pluginSourceFiles,resourceResolver("file:${pluginDir}/src/java"))
                pluginSourceFiles = ArrayUtils.addAll(pluginSourceFiles,resourceResolver("file:${pluginDir}/src/groovy"))
                return pluginSourceFiles
            }
            cache['sourceFiles'] = sourceFiles
        }
        return sourceFiles
    }


    /**
     * Obtains an array of all plug-in provided JAR files
     */
    Resource[] getPluginJarFiles() {
        def jarFiles = cache['jarFiles']
        if(!jarFiles) {
            jarFiles = new Resource[0]
            jarFiles = resolvePluginResourcesAndAdd(jarFiles, pluginDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/lib/*.jar")
            }
            cache['jarFiles'] = jarFiles
        }
        return jarFiles
    }


    /**
     * Obtains a list of plugin directories for the application
     */
    Resource[] getPluginDirectories() {
        def pluginDirectoryResources = cache['pluginDirectoryResources']
        if(!pluginDirectoryResources)  {
            def dirList = getImplicitPluginDirectories()

            // Also add any explicit plugin locations specified by the
            // BuildConfig setting "grails.plugin.location.<name>"
            def pluginLocations = buildSettings?.config?.grails.plugin.location
            if (pluginLocations) {
                dirList.addAll(pluginLocations.collect { key, value -> new FileSystemResource(value) })
            }

            pluginDirectoryResources = dirList as Resource[]
            cache['pluginDirectoryResources'] = pluginDirectoryResources
        }
        return pluginDirectoryResources
    }

    /**
     * Returns only the PluginInfo objects that support the current Environment and BuildScope
     *
     * @see grails.util.Environment
     * @see grails.util.BuildScope
     */
    PluginInfo[] getSupportedPluginInfos() {
        if(pluginManager == null) return getPluginInfos()
        else {
            def pluginInfos = getPluginInfos().findAll {PluginInfo info ->
                GrailsPlugin plugin = pluginManager.getGrailsPlugin(info.getName())
                return plugin?.supportsCurrentScopeAndEnvironment()
            }
            return pluginInfos as PluginInfo[]
        }
    }


    /**
     * Returns a list of all plugin directories in both the given path
     * and the global "plugins" directory together.
     */    
    List<Resource> getImplicitPluginDirectories() {
        def dirList = []
        def directoryNamePredicate = {
            it.isDirectory() && (!it.name.startsWith(".") && it.name.indexOf('-')>-1)
        }

        for(pluginBase in getPluginBaseDirectories()) {
            List pluginDirs = new File(pluginBase).listFiles().findAll(directoryNamePredicate).collect { new FileSystemResource(it) }
            dirList.addAll( pluginDirs )
        }

        return dirList
    }

    /**
     * Gets a list of all the known plugin base directories (directories where plugins are installed to)
     */
    List<String> getPluginBaseDirectories() {
         [ pluginDirPath, buildSettings.globalPluginsDir.path ]
    }

    /**
     * Returns true if the specified plugin directory is a global plugin 
     */
    boolean isGlobalPluginLocation(Resource pluginDir) {
        def globalPluginsDir = buildSettings?.globalPluginsDir?.canonicalFile
        def containingDir = pluginDir?.file?.parentFile?.canonicalFile
        if(globalPluginsDir || containingDir) {
            return globalPluginsDir == containingDir
        }
        return false
    }

    /**
     * Obtains a reference to all artefact resources (all Groovy files contained within the grails-app directory of plugins or applications)
     */
    Resource[] getArtefactResources() {
        def basedir = this.buildSettings.baseDir.absolutePath
        def allArtefactResources = cache['allArtefactResources']
        if(!allArtefactResources) {
            def resources = getArtefactResourcesForOne(new File(basedir).canonicalFile.absolutePath)

            resources = resolvePluginResourcesAndAdd(resources) { String pluginDir ->
                getArtefactResourcesForOne(pluginDir)
            }

            allArtefactResources = resources
            cache['allArtefactResources'] = resources
        }
        return allArtefactResources
    }

    /**
     * Returns an array of all artefacts in the given application or
     * plugin directory as Spring resources.
     */
    Resource[] getArtefactResourcesForOne(String projectDir) {
        return resourceResolver("file:${projectDir}/grails-app/**/*.groovy")
    }

    /**
     * Obtains an array of all plug-in descriptors (the root classes that end with *GrailsPlugin.groovy)
     */
    Resource[] getPluginDescriptors() {
        def pluginDescriptors = cache['pluginDescriptors']
        if(!pluginDescriptors) {
            def pluginDirs = getPluginDirectories() as List
            pluginDirs << new FileSystemResource(this.buildSettings.baseDir)

            def descriptors = []
            pluginDirs.each {
                descriptors += resourceResolver("file:${it.file}/*GrailsPlugin.groovy") as List
            }
            pluginDescriptors = descriptors as Resource[]
            cache['pluginDescriptors'] = pluginDescriptors
        }
        return pluginDescriptors
    }

    /**
     * Obtains an array of all plug-in lib directories
     */
    Resource[] getPluginLibDirectories() {
        def pluginLibs = cache['pluginLibs']
        if(!pluginLibs) {
            pluginLibs = new Resource[0]
            pluginLibs = resolvePluginResourcesAndAdd(pluginLibs, pluginDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/lib")
            }
            cache['pluginLibs'] = pluginLibs
        }
        return pluginLibs
    }

    /**
     * Obtains an array of all plugin i18n directories
     */
    Resource[] getPluginI18nDirectories() {
        def plugin18nDirectories = cache['plugin18nDirectories']
        if(!plugin18nDirectories) {
            plugin18nDirectories = new Resource[0]
            plugin18nDirectories = resolvePluginResourcesAndAdd(plugin18nDirectories, pluginDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/grails-app/i18n")
            }
            cache['plugin18nDirectories'] = plugin18nDirectories
        }
        return plugin18nDirectories
    }

    /**
     * Obtains the path to the global plugins directory
     */
    String getGlobalPluginsPath() { buildSettings?.globalPluginsDir?.path }

    /**
     * Obtains a plugin directory for the given name
     */
    Resource getPluginDirForName(String pluginName) {
        Resource pluginResource = pluginToDirNameMap[pluginName]
        if(!pluginResource) {

            try {
                def directoryNamePredicate = {
                    it.isDirectory() && (it.name == pluginName || it.name.startsWith("$pluginName-"))
                }

                List<String> pluginDirs = getPluginBaseDirectories()
                File pluginFile
                for(pluginDir in pluginDirs) {
                    pluginFile = new File("${pluginDir}").listFiles().find(directoryNamePredicate)
                    if(pluginFile) break
                }

                // If the plugin can't be found in one of the standard
                // locations, check whether it's an in-place plugin.

                if (!pluginFile && pluginLocations) {
                    def pluginLoc = pluginLocations.find { key, value -> pluginName == key }
                    // maybe the plugin name includes a version suffix so attempt startsWith
                    if(!pluginLoc) {
                       pluginLoc = pluginLocations.find { key, value -> pluginName.startsWith(key)  }
                    }
                    if (pluginLoc) pluginFile = new File(pluginLoc.value)
                }

                pluginResource =  pluginFile ? new FileSystemResource(pluginFile) : null
                if(pluginResource) {
                    pluginToDirNameMap[pluginName] = pluginResource
                }
            } catch (IOException e) {
                // ignore
                return null
            }
        }
        return pluginResource
    }

    /**
     * Obtains the 'base' plugin descriptor, which is the plugin descriptor of the current plugin project
     */
    Resource getBasePluginDescriptor() {
        def basePluginDescriptor = cache['basePluginDescriptor']
        if(!basePluginDescriptor) {
            basePluginDescriptor = getDescriptorForPlugin(
                    new FileSystemResource(this.buildSettings.baseDir.absolutePath))
            if(basePluginDescriptor) {                
                cache['basePluginDescriptor'] = basePluginDescriptor
            }
        }
        return basePluginDescriptor
    }

    /**
     * Returns the descriptor location for the given plugin directory. The descriptor is the Groovy
     * file that ends with *GrailsPlugin.groovy
     */
    Resource getDescriptorForPlugin(Resource pluginDir) {
        FileSystemResource descriptor = null
        File baseFile = pluginDir.file.canonicalFile
        File basePluginFile = baseFile.listFiles().find { it.name.endsWith("GrailsPlugin.groovy")}

        if (basePluginFile?.exists()) {
            descriptor = new FileSystemResource(basePluginFile)
        }
        return descriptor
    }

   /**
     * Takes a Resource[] and optional pluginsDirPath and goes through each plugin directory. It will then used the provided
     * resolving resolving closures to attempt to resolve a new set of resources to add to the original passed array.
     *
     * A new array is then returned that contains any additiona plugin resources that were resolved by the expression passed
     * in the closure
     */
    private resolvePluginResourcesAndAdd(Resource[] originalResources, String pluginsDirPath = this.pluginDirPath, Closure resolver) {
        Resource[] pluginDirs = getPluginDirectories()
        for (dir in pluginDirs) {
            def newResources = dir ? resolver(dir.file.absolutePath) : null
            if (newResources) {
                originalResources = ArrayUtils.addAll(originalResources, newResources)
            }
        }
        return originalResources
    }
}
