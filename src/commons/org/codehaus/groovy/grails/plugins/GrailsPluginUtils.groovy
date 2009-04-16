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
package org.codehaus.groovy.grails.plugins

import grails.util.BuildSettingsHolder
import org.apache.commons.lang.ArrayUtils
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.util.concurrent.ConcurrentHashMap
import groovy.util.slurpersupport.GPathResult

/**
 * Utility class containing methods that aid in loading and evaluating plug-ins
 * 
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Nov 29, 2007
 */
public class GrailsPluginUtils {


    static final String WILDCARD = "*";
    public static final GRAILS_HOME
    static {
        try {
            GRAILS_HOME = System.getenv("GRAILS_HOME")
        }
        catch (Throwable t) {
            // probably due to permissions error
            GRAILS_HOME = "UNKNOWN"
        }
    }


    static final COMPARATOR = [compare: { o1, o2 ->
        def result = 0
        if(o1 == '*') result = 1
        else if(o2 == '*') result = -1
        else {
            def nums1 = o1.split(/\./).findAll { it.trim() != ''}*.toInteger()
            def nums2 = o2.split(/\./).findAll { it.trim() != ''}*.toInteger()
            for(i in 0..<nums1.size()) {
                if(nums2.size() > i) {
                    result = nums1[i].compareTo(nums2[i])
                    if(result != 0)break
                }
            }
        }
            result
        },
        equals: { false }] as Comparator

    /**
     * Check if the required version is a valid for the given plugin version
     *
     * @param pluginVersion The plugin version
     * @param requiredVersion The required version
     * @return True if it is valid
     */
    static boolean isValidVersion(String pluginVersion, String requiredVersion) {

        pluginVersion = trimTag(pluginVersion);

       if(requiredVersion.indexOf('>')>-1) {
            def tokens = requiredVersion.split(">")*.trim()
            tokens = tokens.collect { trimTag(it) }
            tokens << pluginVersion
            tokens = tokens.sort(COMPARATOR)

            if(tokens[1] == pluginVersion) return true

        }
        else if(pluginVersion.equals(trimTag(requiredVersion))) return true;
        return false;
    }

    /**
     * Returns the upper version of a Grails version number expression in a plugin
     */
    static String getUpperVersion(String pluginVersion) {
        if(pluginVersion.indexOf('>')>-1) {
            def tokens = pluginVersion.split(">")*.trim()
            return tokens[1].trim()
        }
        else {
            return pluginVersion.trim()
        }
    }

    private static trimTag(pluginVersion) {
        def i = pluginVersion.indexOf('-')
        if(i>-1)
            pluginVersion = pluginVersion[0..i-1]
        pluginVersion
    }


    private static final PathMatchingResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver()

    /**
     * A default resolve used if none is specified to the resource resolving methods in this class
     */
    static final DEFAULT_RESOURCE_RESOLVER = { pattern ->
        try {
                return RESOLVER.getResources(pattern)
            }
            catch(Throwable e) {
                 return []  as Resource[]
            }

    }

    /**
     * Returns an array of PluginInfo objects
     */
    static PluginInfo[] getPluginInfos(String pluginDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        def pluginInfos = []
        for(dir in getPluginDirectories(pluginDirPath)) {
            pluginInfos << new PluginInfo(dir)
        }
        return pluginInfos as PluginInfo[]
    }

    /**
     * Returns only the PluginInfo objects that support the current Environment and BuildScope
     *
     * @see grails.util.Environment
     * @see grails.util.BuildScope
     */
    static PluginInfo[] getSupportedPluginInfos(String pluginDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        GrailsPluginManager pluginManager = PluginManagerHolder.getPluginManager()
        def pluginInfos = GrailsPluginUtils.getPluginInfos(pluginDirPath).findAll {PluginInfo info ->
            GrailsPlugin plugin = pluginManager.getGrailsPlugin(info.getName())
            return plugin?.supportsCurrentScopeAndEnvironment()
        }
        return pluginInfos as PluginInfo[]
    }


    /**
     * Gets a list of all the known plugin base directories (directories where plugins are installed to)
     */
    static List<String> getPluginBaseDirectories(String pluginDirPath) {
         [ pluginDirPath, BuildSettingsHolder.settings?.globalPluginsDir?.path ]
    }

    /**
     * Gets a list of all the known plugin base directories (directories where plugins are installed to)
     */
    static List<String> getPluginBaseDirectories() {
         [ BuildSettingsHolder.settings?.projectPluginsDir?.path, BuildSettingsHolder.settings?.globalPluginsDir?.path ]
    }

    private static Resource[] pluginDirectoryResources = null

    static Resource[] getPluginDirectories() {
        return getPluginDirectories(BuildSettingsHolder.settings?.projectPluginsDir?.path)
    }

    static synchronized Resource[] getPluginDirectories(String pluginDirPath) {
        if(!pluginDirectoryResources) {            
            def dirList = getImplicitPluginDirectories(pluginDirPath)

            // Also add any explicit plugin locations specified by the
            // BuildConfig setting "grails.plugin.location.<name>"
            def pluginLocations = BuildSettingsHolder.settings?.config?.grails.plugin.location
            if (pluginLocations) {
                dirList.addAll(pluginLocations.collect { key, value -> new FileSystemResource(value) })
            }

            pluginDirectoryResources = dirList as Resource[]
        }
        return pluginDirectoryResources
    }

    /**
     * Returns a list of all plugin directories in both the given path
     * and the global "plugins" directory together.
     */
    static synchronized List<Resource> getImplicitPluginDirectories(String pluginDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        def dirList = []
        def directoryNamePredicate = {
            it.isDirectory() && (!it.name.startsWith(".") && it.name.indexOf('-')>-1)
        }

        for(pluginBase in getPluginBaseDirectories(pluginDirPath)) {
            List pluginDirs = new File(pluginBase).listFiles().findAll(directoryNamePredicate).collect { new FileSystemResource(it) }
            dirList.addAll( pluginDirs )
        }

        return dirList
    }

    static boolean isGlobalPluginLocation(Resource pluginDir) {
        def globalPluginsDir = BuildSettingsHolder.settings?.globalPluginsDir
        def containingDir = pluginDir?.file?.parentFile
        if(globalPluginsDir && containingDir) {
            return globalPluginsDir.equals(containingDir)                        
        }
        return false
    }

    private static allArtefactResources = null
    /**
     * Obtains a reference to all artefact resources (all Groovy files contained within the grails-app directory of plugins or applications)
     */
    static synchronized Resource[] getArtefactResources(String basedir, Closure resourceResolver = DEFAULT_RESOURCE_RESOLVER) {
        if(!allArtefactResources) {
            def resources = getArtefactResourcesForOne(new File(basedir).canonicalFile.absolutePath, resourceResolver)

            resources = resolvePluginResourcesAndAdd(resources) { String pluginDir ->
                getArtefactResourcesForOne(pluginDir, resourceResolver)
            }

            allArtefactResources = resources
        }
        return allArtefactResources
    }

    /**
     * Returns an array of all artefacts in the given application or
     * plugin directory as Spring resources.
     */
    static Resource[] getArtefactResourcesForOne(String projectDir, Closure resourceResolver = DEFAULT_RESOURCE_RESOLVER) {
        return resourceResolver("file:${projectDir}/grails-app/**/*.groovy")
    }

    private static allPluginXmlMetadata = null
    /**
     * Obtains a Resource array of the Plugin metadata XML files used to describe the plugins provided resources
     */
    static synchronized Resource[] getPluginXmlMetadata( String pluginsDirPath,
                                            Closure resourceResolver = DEFAULT_RESOURCE_RESOLVER) {
        if(!allPluginXmlMetadata) {
            allPluginXmlMetadata = new Resource[0]
            allPluginXmlMetadata = resolvePluginResourcesAndAdd(allPluginXmlMetadata, pluginsDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/plugin.xml")
            }
        }
        return allPluginXmlMetadata
    }

    /**
     * Takes a Resource[] and optional pluginsDirPath and goes through each plugin directory. It will then used the provided
     * resolving resolving closures to attempt to resolve a new set of resources to add to the original passed array.
     *
     * A new array is then returned that contains any additiona plugin resources that were resolved by the expression passed
     * in the closure
     */
    private static resolvePluginResourcesAndAdd(Resource[] originalResources, String pluginsDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path, Closure resolver) {
        Resource[] pluginDirs = getPluginDirectories(pluginsDirPath)
        for (dir in pluginDirs) {
            def newResources = dir ? resolver(dir.file.absolutePath) : null
            if (newResources) {
                originalResources = ArrayUtils.addAll(originalResources, newResources)
            }
        }
        return originalResources
    }

    private static availableScripts = null

    /**
     * Obtains an array of all Gant scripts that are availabe for execution in a Grails application
     */
    static synchronized Resource[] getAvailableScripts(String grailsHome,
                                          String pluginDirPath,
                                          String basedir,
                                          Closure resourceResolver = DEFAULT_RESOURCE_RESOLVER) {
        if(!availableScripts) {

            def scripts = []
            def userHome = System.getProperty("user.home")
            resourceResolver("file:${grailsHome}/scripts/**.groovy").each { if (!it.file.name.startsWith('_')) scripts << it }
            resourceResolver("file:${basedir}/scripts/*.groovy").each { if (!it.file.name.startsWith('_')) scripts << it }
            getPluginScripts(pluginDirPath).each { if (!it.file.name.startsWith('_')) scripts << it }
            resourceResolver("file:${userHome}/.grails/scripts/*.groovy").each { if (!it.file.name.startsWith('_')) scripts << it }
            availableScripts = scripts as Resource[]
        }
        return availableScripts
    }

    private static pluginScripts = null
    /**
     * Obtains an array of plug-in provided Gant scripts available to a Grails application
     */
    static synchronized Resource[] getPluginScripts(String pluginDirPath,Closure resourceResolver = DEFAULT_RESOURCE_RESOLVER) {
        if(!pluginScripts) {
            pluginScripts = new Resource[0]
            pluginScripts = resolvePluginResourcesAndAdd(pluginScripts, pluginDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/scripts/*.groovy")
            }            
        }
        return pluginScripts
    }

    private static pluginResourceBundles = null;

    /**
     * Obtains an array of all plugin provided resource bundles
     */
    static synchronized Resource[] getPluginResourceBundles(String pluginDirPath,Closure resourceResolver = DEFAULT_RESOURCE_RESOLVER) {
        if(!pluginResourceBundles) {
            pluginResourceBundles = new Resource[0]
            pluginResourceBundles = resolvePluginResourcesAndAdd(pluginResourceBundles,pluginDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/grails-app/i18n/*.properties")
            }

        }
        return pluginResourceBundles
    }

    private static Resource[] sourceFiles = null
    /**
     * Obtains an array of all plug-in provided source files (Java and Groovy)
     */
    static synchronized Resource[] getPluginSourceFiles(String pluginsDirPath,Closure resourceResolver = DEFAULT_RESOURCE_RESOLVER) {
        if(!sourceFiles) {
            sourceFiles = new Resource[0]
            sourceFiles = resolvePluginResourcesAndAdd(sourceFiles, pluginsDirPath) { pluginDir ->
                Resource[] pluginSourceFiles = resourceResolver("file:${pluginDir}/grails-app/*")
                pluginSourceFiles = ArrayUtils.addAll(pluginSourceFiles,resourceResolver("file:${pluginDir}/src/java"))
                pluginSourceFiles = ArrayUtils.addAll(pluginSourceFiles,resourceResolver("file:${pluginDir}/src/groovy"))
                return pluginSourceFiles
            }
        }
        return sourceFiles
    }

    private static Resource[] jarFiles= null
    /**
     * Obtains an array of all plug-in provided JAR files
     */
    static synchronized Resource[] getPluginJarFiles(String pluginsDirPath,Closure resourceResolver = DEFAULT_RESOURCE_RESOLVER) {
        if(!jarFiles) {
            jarFiles = new Resource[0]
            jarFiles = resolvePluginResourcesAndAdd(jarFiles, pluginsDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/lib/*.jar")
            }
        }
        return jarFiles
    }

    private static Resource[] pluginDescriptors = null


    /**
     * Obtains an array of all plug-in descriptors (the root classes that end with *GrailsPlugin.groovy)
     */
    static synchronized Resource[] getPluginDescriptors(String basedir,
                                                        String pluginsDirPath,
                                                        Closure resourceResolver = DEFAULT_RESOURCE_RESOLVER) {
        if(!pluginDescriptors) {
            def pluginDirs = getPluginDirectories(pluginsDirPath) as List
            pluginDirs << new FileSystemResource(basedir)

            def descriptors = []
            pluginDirs.each {
                descriptors += resourceResolver("file:${it.file}/*GrailsPlugin.groovy") as List
            }
            pluginDescriptors = descriptors as Resource[]
        }
        return pluginDescriptors
    }

    private static Resource basePluginDescriptor = null
    static synchronized Resource getBasePluginDescriptor(String basedir) {
        if(!basePluginDescriptor) {
            basePluginDescriptor = getDescriptorForPlugin(new FileSystemResource(basedir))
        }
        return basePluginDescriptor
    }

    /**
     * Returns the descriptor location for the given plugin directory. The descriptor is the Groovy
     * file that ends with *GrailsPlugin.groovy
     */
    public static Resource getDescriptorForPlugin(Resource pluginDir) {
        FileSystemResource descriptor = null
        File baseFile = pluginDir.getFile().getCanonicalFile()
        File basePluginFile = baseFile.listFiles().find { it.name.endsWith("GrailsPlugin.groovy")}

        if (basePluginFile?.exists()) {
            descriptor = new FileSystemResource(basePluginFile)
        }
        return descriptor
    }

    private static Resource[] pluginLibs = null

    /**
     * Obtains an array of all plug-in lib directories
     */
    static synchronized Resource[] getPluginLibDirectories(String pluginsDirPath,
                                                            Closure resourceResolver = DEFAULT_RESOURCE_RESOLVER) {
        if(!pluginLibs) {
            pluginLibs = new Resource[0]
            pluginLibs = resolvePluginResourcesAndAdd(pluginLibs, pluginsDirPath) { pluginDir ->
                resourceResolver("file:${pluginDir}/lib")
            }

        }
        return pluginLibs
    }

    /**
     * Obtains the path to the globa plugins directory
     */
    static String getGlobalPluginsPath() { BuildSettingsHolder.settings?.globalPluginsDir?.path }

    private static Map pluginToDirNameMap = new ConcurrentHashMap()
    
    /**
     * Obtains a plugin directory for the given name
     */
    static Resource getPluginDirForName(String pluginName) {
        getPluginDirForName(BuildSettingsHolder.settings?.projectPluginsDir?.path, pluginName)
    }


    private static Map pluginMetaDataMap = new ConcurrentHashMap()
    /**
     * Returns XML about the plugin 
     */
    static GPathResult getMetadataForPlugin(String pluginName) {
        if(pluginMetaDataMap[pluginName]) return pluginMetaDataMap[pluginName]
        Resource pluginDir = getPluginDirForName(BuildSettingsHolder.settings?.projectPluginsDir?.path, pluginName)
        GPathResult result = getMetadataForPlugin(pluginDir)
        pluginMetaDataMap[pluginName] = result
        return result
    }

    /**
     * Returns XML metadata for the plugin
     */
    static GPathResult getMetadataForPlugin(Resource pluginDir) {
        try {
            GPathResult result = new XmlSlurper().parse(new File("$pluginDir.file.absolutePath/plugin.xml"))
            return result
        }
        catch (e) {
            return null;
        }
    }

    /**
     * Obtains a plugin directory for the given name
     */
    static Resource getPluginDirForName(String pluginsDirPath, String pluginName) {
        Resource pluginResource = pluginToDirNameMap[pluginName]
        if(!pluginResource) {

            try {
                def directoryNamePredicate = {
                    it.isDirectory() && (it.name == pluginName || it.name.startsWith("$pluginName-"))
                }

                List<String> pluginDirs = getPluginBaseDirectories(pluginsDirPath)
                File pluginFile
                for(pluginDir in pluginDirs) {
                    pluginFile = new File("${pluginDir}").listFiles().find(directoryNamePredicate)
                    if(pluginFile) break
                }

                // If the plugin can't be found in one of the standard
                // locations, check whether it's an in-place plugin.
                def pluginLocations = BuildSettingsHolder.settings?.config?.grails?.plugin?.location
                if (!pluginFile && pluginLocations) {
                    def pluginLoc = pluginLocations.find { key, value -> pluginName.startsWith(key) }
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
     * Clears cached resolved resources
     */
    static synchronized clearCaches() {
        pluginToDirNameMap.clear()
        pluginDirectoryResources = null
        pluginDescriptors = null
        pluginLibs = null
        pluginScripts = null
        basePluginDescriptor = null
        jarFiles = null
        sourceFiles = null
        allArtefactResources = null
        availableScripts = null
    }



}

