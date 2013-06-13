/*
 * Copyright 2004-2005 Graeme Rocher
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

import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult

import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import org.codehaus.groovy.grails.io.support.AntPathMatcher
import org.codehaus.groovy.grails.io.support.FileSystemResource
import org.codehaus.groovy.grails.io.support.IOUtils
import org.codehaus.groovy.grails.io.support.PathMatchingResourcePatternResolver
import org.codehaus.groovy.grails.io.support.Resource
import org.codehaus.groovy.grails.plugins.BasicGrailsPluginInfo
import org.codehaus.groovy.grails.plugins.CompositePluginDescriptorReader
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.plugins.PluginInfo
import org.codehaus.groovy.grails.plugins.build.scopes.PluginScopeInfo

/**
 * Uses the project BuildSettings object to discover information about the installed plugin
 * such as the jar files they provide, the plugin descriptors and so on.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
class PluginBuildSettings {

    private static final List<String> PLUGIN_EXCLUDE_PATHS = ["views", "i18n"] // conf gets special handling

    /**
     * Resources to be excluded from the final packaged plugin. Defined as Ant paths.
     */
    public static final EXCLUDED_RESOURCES = [
        "web-app/WEB-INF/**",
        "web-app/plugins/**",
        "grails-app/conf/spring/resources.groovy",
        "grails-app/conf/*DataSource.groovy",
        "grails-app/conf/DataSource.groovy",
        "grails-app/conf/BootStrap.groovy",
        "grails-app/conf/Config.groovy",
        "grails-app/conf/BuildConfig.groovy",
        "grails-app/conf/UrlMappings.groovy",
        "**/.svn/**",
        "test/**",
        "**/CVS/**"
    ]

    private static final PathMatchingResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver()

    /**
     * A default resolver used if none is specified to the resource resolving methods in this class.
     */
    Closure resourceResolver = { pattern ->
        try {
            return RESOLVER.getResources(pattern)
        }
        catch(Throwable e) {
            return [] as Resource[]
        }
    }

    BuildSettings buildSettings
    def pluginManager
    String pluginDirPath
    private Map cache = new ConcurrentHashMap()
    private Map pluginToDirNameMap = new ConcurrentHashMap()
    private Map pluginMetaDataMap = new ConcurrentHashMap()
    private Map<String, PluginInfo> pluginInfosMap = new ConcurrentHashMap<String, PluginInfo>()
    private Map<String, PluginInfo> pluginInfoToSourceMap = new ConcurrentHashMap<String, PluginInfo>()

    private pluginLocations

    PluginScopeInfo compileScopePluginInfo = new PluginScopeInfo("compile")
    PluginScopeInfo buildScopePluginInfo = new PluginScopeInfo("build")
    PluginScopeInfo providedScopePluginInfo = new PluginScopeInfo("provided")
    PluginScopeInfo testScopePluginInfo = new PluginScopeInfo("test")

    private Map<String, PluginScopeInfo> pluginScopeInfoMap = [
        compile: compileScopePluginInfo,
        build: buildScopePluginInfo,
        provided: providedScopePluginInfo,
        test: testScopePluginInfo]

    PluginBuildSettings(BuildSettings buildSettings) {
        this(buildSettings, null)
    }

    PluginBuildSettings(BuildSettings buildSettings, pluginManager) {
        // We use null-safe navigation on buildSettings because otherwise lots of unit tests will fail
        this.buildSettings = buildSettings
        this.pluginManager = pluginManager
        pluginDirPath = buildSettings?.projectPluginsDir?.absolutePath
        pluginLocations = buildSettings?.config?.grails?.plugin?.location

        if (buildSettings == null) {
            return
        }

        populateSourceDirectories(compileScopePluginInfo,  buildSettings.pluginRuntimeDependencies)

        for (pluginDir in getInlinePluginDirectories()) {
            final pluginInfo = getPluginInfo(pluginDir.file.absolutePath)
            if (pluginInfo != null) {
                addPluginScopeInfoForDirAndInfo(compileScopePluginInfo, pluginInfo, pluginDir)
            }
        }

        populateSourceDirectories(buildScopePluginInfo,  buildSettings.pluginBuildDependencies)
        populateSourceDirectories(providedScopePluginInfo,  buildSettings.pluginProvidedDependencies)
        populateSourceDirectories(testScopePluginInfo,  buildSettings.pluginTestDependencies)
        testScopePluginInfo = testScopePluginInfo - compileScopePluginInfo
    }

    @CompileStatic
    void registerNewPluginInstall(File zip) {
         switch(zip) {
             case buildSettings.pluginBuildDependencies:
                 registerPluginZipWithScope(zip, buildScopePluginInfo)
                 break
             case buildSettings.pluginCompileDependencies:
                  registerPluginZipWithScope(zip, compileScopePluginInfo)
                  break
             case buildSettings.pluginRuntimeDependencies:
                  registerPluginZipWithScope(zip, compileScopePluginInfo)
                  break
             case buildSettings.pluginTestDependencies:
                  registerPluginZipWithScope(zip, testScopePluginInfo)
                  break
             case buildSettings.pluginProvidedDependencies:
                  registerPluginZipWithScope(zip, providedScopePluginInfo)
                  break
         }
    }

    @CompileStatic
    private populateSourceDirectories(PluginScopeInfo compileInfo, List<File> pluginDependencies) {
        for (zip in pluginDependencies) {
            registerPluginZipWithScope(zip, compileInfo)
        }
    }

    protected void registerPluginZipWithScope(File pluginZip, PluginScopeInfo scopeInfo) {
        def info = readPluginInfoFromZip(pluginZip.absolutePath)
        if (info == null) {
            return
        }

        def existingInfo = getPluginInfoForName(info.name)
        if (existingInfo != null && existingInfo.version == info.version) {
            addPluginScopeInfoForDirAndInfo(scopeInfo, info, existingInfo.pluginDir)
        }
    }

    @CompileStatic
    protected void addPluginScopeInfoForDirAndInfo(PluginScopeInfo compileInfo, GrailsPluginInfo info, Resource dir) {
        def excludedPaths = PLUGIN_EXCLUDE_PATHS // conf gets special handling
        compileInfo.pluginInfos << info
        compileInfo.pluginNames << info.name
        compileInfo.sourceDirectories.addAll(
                getPluginSourceDirectories(dir.file.canonicalFile).findAll { Resource it ->
                    !excludedPaths.contains(it.file.name) && it.file.isDirectory()
                })
        compileInfo.pluginDescriptors << getPluginDescriptor(dir)
        compileInfo.artefactResources.addAll(getArtefactResourcesForOne(dir.file.absolutePath))
    }

    /**
     * Clears any cached entries.
     */
    void clearCache() {
        cache.clear()
        buildSettings?.clearCache()
        pluginToDirNameMap.clear()
        pluginMetaDataMap.clear()
        pluginInfosMap.clear()
        pluginInfoToSourceMap.clear()
    }

    /**
     * Returns an array of PluginInfo objects
     */
    GrailsPluginInfo[] getPluginInfos() {
        getPluginInfos(this.pluginDirPath)
    }
    GrailsPluginInfo[] getPluginInfos(String pluginDirPath) {
        if (pluginInfosMap) {
            return cache.pluginInfoList
        }

        def pluginInfos = []
        Resource[] pluginDescriptors = getPluginDescriptors()
        CompositePluginDescriptorReader pluginDescriptorReader = new CompositePluginDescriptorReader(this)
        for (desc in pluginDescriptors) {
            try {
                GrailsPluginInfo info = pluginDescriptorReader.readPluginInfo(desc)
                if (info != null) {
                    pluginInfos << info
                    pluginInfosMap.put(info.name, info)
                    pluginInfosMap.put(info.fullName, info)
                }
            }
            catch (e) {
                // ignore, not a valid plugin directory
            }
        }
        cache.pluginInfoList = pluginInfos as GrailsPluginInfo[]
        return pluginInfos as GrailsPluginInfo[]
    }

    /**
     * Returns true if the specified plugin location is an inline location.
     */
    boolean isInlinePluginLocation(Resource pluginLocation) {
        buildSettings?.isInlinePluginLocation(pluginLocation?.getFile())
    }

    /**
     * Returns an array of the inplace plugin locations.
     */
    //@CompileStatic
    Resource[] getInlinePluginDirectories() {
        Resource[] locations = (Resource[])cache['inlinePluginLocations']
        if (locations == null) {

            if (buildSettings) {
                locations = buildSettings.getInlinePluginDirectories().collect { File it -> new FileSystemResource(it) }
            }
            else {
                locations = [] as Resource[]
            }

            cache['inlinePluginLocations'] = locations
        }
        return locations
    }

    /**
     * Obtains a PluginInfo for the installed plugin directory.
     */
    @CompileStatic
    GrailsPluginInfo getPluginInfo(String pluginBaseDir) {
        if (!pluginInfosMap) getPluginInfos() // initialize the infos
        def dir = new FileSystemResource(pluginBaseDir)
        def descLocation = getDescriptorForPlugin(dir)
        if (descLocation) {
            def pluginName = GrailsNameUtils.getPluginName(descLocation.filename)
            return (GrailsPluginInfo)pluginInfosMap[pluginName]
        }
        return null
    }

    /**
     * Obtains a PluginInfo for the installed plugin directory.
     */
    @CompileStatic
    GrailsPluginInfo getPluginInfoForName(String pluginName) {
        if (!pluginInfosMap) getPluginInfos() // initialize the infos
        return (GrailsPluginInfo)pluginInfosMap[pluginName]
    }

    /**
     * Gets a PluginInfo for a particular source file if its contained within that plugin
     */
    GrailsPluginInfo getPluginInfoForSource(String sourceFile) {
        if (pluginInfoToSourceMap[sourceFile]) {
            return pluginInfoToSourceMap[sourceFile]
        }

        for (Resource pluginDir in getPluginDirectories()) {
            def pluginPath = pluginDir.file.canonicalPath + File.separator
            def sourcePath = new File(sourceFile).canonicalPath
            if (sourcePath.startsWith(pluginPath)) {
                // Check the path of the source file relative to the
                // plugin directory. If the source file is in the
                // plugin's "test" directory, we ignore it. It's a
                // bit of a HACK, but not much else we can do without
                // a refactor of the plugin management.
                sourcePath = sourcePath.substring(pluginPath.length())
                if (!sourcePath.startsWith("test" + File.separator)) {
                    GrailsPluginInfo info = getPluginInfo(pluginPath)
                    if (info) {
                        pluginInfoToSourceMap[sourceFile] = info
                    }
                    return info
                }
            }
        }

        def baseDir = buildSettings?.getBaseDir()?.getCanonicalPath()
        if (baseDir != null && sourceFile.startsWith(baseDir)) {
            GrailsPluginInfo basePluginInfo = getPluginInfo(baseDir)
            if (basePluginInfo != null) {
                pluginInfoToSourceMap[sourceFile] = basePluginInfo
                return basePluginInfo
            }
        }

        return null
    }

    /**
     * Obtains a Resource array of the Plugin metadata XML files used to describe the plugins provided resources
     */
    Resource[] getPluginXmlMetadata() {
        resolveResources 'allPluginXmlMetadata', false, { pluginDir ->
            resourceResolver("file:${pluginDir}/plugin.xml")
        }
    }

    /**
     * Returns XML about the plugin.
     */
    GPathResult getMetadataForPlugin(String pluginName) {
        if (pluginMetaDataMap[pluginName]) return pluginMetaDataMap[pluginName]

        Resource pluginDir = getPluginDirForName(pluginName)
        GPathResult result = getMetadataForPlugin(pluginDir)
        if (result != null) {
            pluginMetaDataMap[pluginName] = result
        }
        return result
    }

    /**
     * Returns XML metadata for the plugin.
     */
    GPathResult getMetadataForPlugin(Resource pluginDir) {
        try {
            return new XmlSlurper().parse(new File("$pluginDir.file.absolutePath/plugin.xml"))
        }
        catch (e) {
            return null
        }
    }

    /**
     * Obtains an array of all Gant scripts that are availabe for execution in a Grails application.
     */
    Resource[] getAvailableScripts() {

        def availableScripts = cache.availableScripts
        if (!availableScripts) {

            def scripts = []
            def userHome = System.getProperty("user.home")
            def grailsHome = buildSettings.grailsHome?.absolutePath
            def basedir = buildSettings.baseDir.absolutePath
            if (grailsHome != null) {
                resourceResolver("file:${grailsHome}/scripts/**.groovy").each { if (!it.file.name.startsWith('_')) scripts << it }
            }
            resourceResolver("file:${basedir}/scripts/*.groovy").each { if (!it.file.name.startsWith('_')) scripts << it }
            pluginScripts.each { if (!it.file.name.startsWith('_')) scripts << it }
            resourceResolver("file:${userHome}/.grails/scripts/*.groovy").each { if (!it.file.name.startsWith('_')) scripts << it }
            availableScripts = scripts as Resource[]
            cache.availableScripts = availableScripts
        }
        return availableScripts
    }

    /**
     * Obtains an array of plugin provided Gant scripts available to a Grails application.
     */
    Resource[] getPluginScripts() {
        resolveResources 'pluginScripts', false, { pluginDir ->
            resourceResolver("file:${pluginDir}/scripts/*.groovy")
        }
    }

    /**
     * Obtains an array of all plugin provided resource bundles.
     */
    Resource[] getPluginResourceBundles() {
        resolveResources 'pluginResourceBundles', false, { pluginDir ->
            resourceResolver("file:${pluginDir}/grails-app/i18n/**/*.properties")
        }
    }

    /**
     * Obtains an array of all plugin provided source directories
     *
     * @deprecated Use getPluginSourceDirectories
     */
    Resource[] getPluginSourceFiles() {
        return getPluginSourceDirectories()
    }

    /**
     * Obtains an array of all plugin provided source directories
     *
     */
    //@CompileStatic
    Resource[] getPluginSourceDirectories() {
        def sourceFiles = cache.sourceFiles
        if (!sourceFiles) {
            cache.sourceFilesPerPlugin = [:]
            sourceFiles = new Resource[0]
            sourceFiles = resolvePluginResourcesAndAdd(sourceFiles, true) { pluginDir ->
                Resource[] pluginSourceFiles = resourceResolver("file:${pluginDir}/grails-app/*")
                pluginSourceFiles = (Resource[])IOUtils.addAll(pluginSourceFiles, resourceResolver("file:${pluginDir}/src/java"))
                pluginSourceFiles = (Resource[])IOUtils.addAll(pluginSourceFiles, resourceResolver("file:${pluginDir}/src/groovy"))
                cache['sourceFilesPerPlugin'][pluginDir] = pluginSourceFiles
                return pluginSourceFiles
            }
            cache.sourceFiles = sourceFiles
        }
        return (Resource[])sourceFiles
    }

    /**
     * Gets all the plugin source directories for the given plugin directory
     */
    Resource[] getPluginSourceDirectories(File pluginDir) {
        getPluginSourceDirectories() // initialize cache

        cache['sourceFilesPerPlugin'][pluginDir.absolutePath]
    }

    /**
     * @deprecated Use getPluginSourceDirectories
     */
    Resource[] getPluginSourceFiles(File pluginDir) {
        return getPluginSourceDirectories(pluginDir)
    }

    /**
     * Obtains an array of all plugin provided JAR files
     */
    Resource[] getPluginJarFiles() {
        resolveResources 'jarFiles', false, { pluginDir ->
            resourceResolver("file:${pluginDir}/lib/*.jar")
        }
    }

    /**
     * Obtains an array of all plugin provided JAR files for plugins that don't define
     * a dependencies.groovy.
     */
    Resource[] getUnmanagedPluginJarFiles() {
        resolveResources 'unmanagedPluginJars', false, { pluginDir ->
            if (!new File("${pluginDir}/dependencies.groovy").exists()) {
                return resourceResolver("file:${pluginDir}/lib/*.jar")
            }
        }
    }

    /**
     * Obtains a list of plugin directories for the application
     */
    Resource[] getPluginDirectories() {
        def pluginDirectoryResources = cache.pluginDirectoryResources
        if (!pluginDirectoryResources) {
            if (buildSettings) {
                pluginDirectoryResources = buildSettings.getPluginDirectories().collect { new FileSystemResource(it) } as Resource[]
            }
            else {
                pluginDirectoryResources = [] as Resource[]
            }
            cache.pluginDirectoryResources = pluginDirectoryResources
        }
        return pluginDirectoryResources
    }

    /**
     * Returns only the PluginInfo objects that support the current Environment and BuildScope.
     *
     * @see grails.util.Environment
     * @see grails.util.BuildScope
     */
    GrailsPluginInfo[] getSupportedPluginInfos() {
        if (pluginManager == null) return getPluginInfos()

        def pluginInfos = getPluginInfos().findAll {GrailsPluginInfo info ->
            def plugin = pluginManager.getGrailsPlugin(info.getName())
            return plugin?.supportsCurrentScopeAndEnvironment()
        }
        return pluginInfos as GrailsPluginInfo[]
    }

    /**
     * Returns a list of all plugin directories in both the given path
     * and the global "plugins" directory together.
     */
    List<Resource> getImplicitPluginDirectories() {
        def implicitPluginDirectories = cache.implicitPluginDirectories
        if (implicitPluginDirectories == null) {
            if (buildSettings) {
                implicitPluginDirectories = buildSettings.getImplicitPluginDirectories().collect { new FileSystemResource(it) }
            }
            else {
                implicitPluginDirectories = [] as Resource[]
            }
            cache.implicitPluginDirectories = implicitPluginDirectories
        }
        return implicitPluginDirectories
    }

    /**
     * Gets a list of all the known plugin base directories (directories where plugins are installed to).
     */
    List<String> getPluginBaseDirectories() {
        return buildSettings?.getPluginBaseDirectories() ?: []
    }

    /**
     * Returns true if the specified plugin directory is a global plugin.
     */
    boolean isGlobalPluginLocation(Resource pluginDir) {
        def globalPluginsDir = buildSettings?.globalPluginsDir?.canonicalFile
        def containingDir = pluginDir?.file?.parentFile?.canonicalFile
        if (globalPluginsDir || containingDir) {
            return globalPluginsDir == containingDir
        }
        return false
    }

    /**
     * Obtains a reference to all artefact resources (all Groovy files contained within the
     * grails-app directory of plugins or applications).
     */
    Resource[] getArtefactResources() {
        def basedir = buildSettings.baseDir.absolutePath
        def allArtefactResources = cache.allArtefactResources
        if (!allArtefactResources) {
            def resources = [] as Resource[]

            // first scan plugin sources. These need to be loaded first
            resources = resolvePluginResourcesAndAdd(resources, true) { String pluginDir ->
                getArtefactResourcesForOne(pluginDir)
            }

            // now build of application resources so that these can override plugin resources
            resources = IOUtils.addAll(resources, getArtefactResourcesForOne(new File(basedir).canonicalFile.absolutePath))

            allArtefactResources = resources
            cache.allArtefactResources = resources
        }
        return allArtefactResources
    }

    /**
     * @return A list of plugin infos that are supported and scoped for compile or runtime
     */
    @CompileStatic
    List<Resource> getCompileScopedArtefactResources() {
        List<Resource> artefactResources = (List<Resource>)cache['compileScopedArtefactResources']
        if (artefactResources == null) {
            artefactResources = []
            artefactResources.addAll compileScopePluginInfo.artefactResources
            artefactResources.addAll getArtefactResourcesForOne(buildSettings.baseDir.path)
            cache['compileScopedArtefactResources'] = artefactResources
        }
        return artefactResources
    }

    /**
     *
     * @return A list of plugin infos that are supported and scoped for compile or runtime
     */
//    @CompileStatic
    List<GrailsPluginInfo> getCompileScopedSupportedPluginInfos() {
        Collection<GrailsPluginInfo> compileScopePluginInfos = (Collection<GrailsPluginInfo>)cache.compileScopePluginInfos
        if (compileScopePluginInfos == null) {
            def pluginInfos = supportedPluginInfos
            compileScopePluginInfos = []
            compileScopePluginInfos.addAll compileScopePluginInfo.pluginInfos
            compileScopePluginInfos = compileScopePluginInfos.findAll { GrailsPluginInfo info -> pluginInfos.any { GrailsPluginInfo it -> it.name == info.name } }
            cache.compileScopePluginInfos = compileScopePluginInfos
        }
        return compileScopePluginInfos
    }

    /**
     * Returns all the artefact resources for the current environment where the environment is to be executed using the embedded container (not WAR deployed)
     *
     * @return An array of all artefact resources
     */
    //@CompileStatic
    Resource[] getArtefactResourcesForCurrentEnvironment() {
        Resource[] artefactResources = (Resource[])cache['allArtefactResourcesForEnvironment']
        if (artefactResources == null) {

            List<Resource> artefactResourcesList = []
            artefactResourcesList.addAll compileScopePluginInfo.getArtefactResources()
            artefactResourcesList.addAll providedScopePluginInfo.getArtefactResources()
            artefactResourcesList.addAll getArtefactResourcesForOne(buildSettings.baseDir.path)
            if (Environment.getCurrent() == Environment.TEST) {
                artefactResourcesList.addAll testScopePluginInfo.getArtefactResources()
            }
            def inlineDirectories = getInlinePluginDirectories()
            for (Resource dir in inlineDirectories) {
                artefactResourcesList.addAll getArtefactResourcesForOne(dir.file.absolutePath)
            }

            artefactResources = artefactResourcesList as Resource[]
            cache['allArtefactResourcesForEnvironment'] = artefactResources
        }
        return artefactResources
    }

    /**
     * Returns an array of all artefacts in the given application or
     * plugin directory as Spring resources.
     */
    Resource[] getArtefactResourcesForOne(String projectDir) {
        return resourceResolver("file:${projectDir}/grails-app/**/*.groovy")
    }

    Resource[] getPluginDescriptorsForCurrentEnvironment() {
        def descriptorList  = cache.pluginDescriptorsForCurrentEnvironment
        if (descriptorList  == null) {
            if (Environment.current == Environment.TEST) {
                descriptorList = pluginScopeInfoMap.values()*.pluginDescriptors.flatten()
            }
            else {
                descriptorList = pluginScopeInfoMap.values().findAll { PluginScopeInfo scopeInfo -> scopeInfo.scopeName != "test" }*.pluginDescriptors.flatten()
            }

            def baseDescriptor = getBasePluginDescriptor()
            if (baseDescriptor != null) {
                descriptorList << baseDescriptor
            }
            for (inlinePluginDir in getInlinePluginDirectories()) {
                descriptorList << getPluginDescriptor(inlinePluginDir)
            }

            descriptorList = descriptorList.unique()

            cache.pluginDescriptorsForCurrentEnvironment = descriptorList
        }
        return descriptorList as Resource[]
    }
    /**
     * Obtains an array of all plugin descriptors (the root classes that end with *GrailsPlugin.groovy).
     */
    Resource[] getPluginDescriptors() {
        def pluginDescriptors = cache.pluginDescriptors
        if (!pluginDescriptors) {
            def pluginDirs = getPluginDirectories().toList()
            if (buildSettings?.baseDir) {
                pluginDirs << new FileSystemResource(buildSettings.baseDir)
            }
            def descriptors = []
            for (Resource dir in pluginDirs) {
                def desc = getPluginDescriptor(dir)
                if (desc) {
                    descriptors << desc
                }
            }

            pluginDescriptors = descriptors as Resource[]
            cache.pluginDescriptors = pluginDescriptors
        }
        return pluginDescriptors
    }

    /**
     * Returns the plugin descriptor for the Given plugin directory.
     *
     * @param pluginDir The plugin directory
     * @return The plugin descriptor
     */
    Resource getPluginDescriptor(Resource pluginDir) {
        File f = pluginDir?.file.listFiles()?.find { it.name.endsWith("GrailsPlugin.groovy") }
        if (f) return new FileSystemResource(f)
    }

    /**
     * Obtains an array of all plugin lib directories.
     */
    Resource[] getPluginLibDirectories() {
        resolveResources 'pluginLibs', false, { pluginDir ->
            resourceResolver("file:${pluginDir}/lib")
        }
    }

    /**
     * Obtains an array of all plugin i18n directories.
     */
    Resource[] getPluginI18nDirectories() {
        resolveResources 'plugin18nDirectories', false, { pluginDir ->
            resourceResolver("file:${pluginDir}/grails-app/i18n")
        }
    }

    /**
     * Obtains the path to the global plugins directory.
     */
    String getGlobalPluginsPath() { buildSettings?.globalPluginsDir?.path }

    /**
     * Obtains a plugin directory for the given name.
     */
    Resource getPluginDirForName(String pluginName) {
        Resource pluginResource = (Resource)pluginToDirNameMap[pluginName]
        if (!pluginResource) {
            try {
                GrailsPluginInfo pluginInfo = getPluginInfoForName(pluginName)
                File pluginFile = pluginInfo?.pluginDir?.file

                // If the plugin can't be found in one of the standard
                // locations, check whether it's an in-place plugin.

                if (!pluginFile && pluginLocations) {
                    Map.Entry pluginLoc = (Map.Entry)pluginLocations.find { String key, value -> pluginName == key }
                    // maybe the plugin name includes a version suffix so attempt startsWith
                    if (!pluginLoc) {
                        pluginLoc = (Map.Entry)pluginLocations.find { String key, value -> pluginName.startsWith(key) }
                    }
                    if (pluginLoc?.value) pluginFile = new File(pluginLoc.value.toString())
                }

                if (pluginFile != null) {
                    pluginResource =  new FileSystemResource(pluginFile)
                }
                if (pluginResource) {
                    pluginToDirNameMap[pluginName] = pluginResource
                }
            }
            catch (IOException ignore) {
                return null
            }
        }
        return pluginResource
    }

    /**
     * Reads plugin metadata from a plugin zip file and returns a list containing the plugin name, version and
     * XML metadata. Designed for use with Groovy's multiple assignment operator
     *
     * @param zipLocation The zip location
     * @return A list
     */
    List readMetadataFromZip(String zipLocation) {
        try {
            def zipFile = new ZipFile(zipLocation)
            ZipEntry entry = zipFile.entries().find {ZipEntry entry -> entry.name == 'plugin.xml'}
            if (entry) {
                def pluginXml = new XmlSlurper().parse(zipFile.getInputStream(entry))
                def name = pluginXml.'@name'.text()
                def release = pluginXml.'@version'.text()
                return [name, release, pluginXml]
            }
        }
        catch (e) {
            // ignore
        }
        return null
    }

    /**
     * Reads plugin info from the zip file location
     *
     * @param zipLocation The zip location
     * @return
     */
    GrailsPluginInfo readPluginInfoFromZip(String zipLocation) {
        String key = 'pluginInfo:' + zipLocation
        GrailsPluginInfo info = cache[key]
        if (info == null) {

            def result = readMetadataFromZip(zipLocation)
            if (result != null) {

                def (name, version, xml) = result

                if (name == null || version == null) return null

                def pluginInfo = getPluginInfoForName(name)
                if (pluginInfo != null) {
                    info = new BasicGrailsPluginInfo(pluginInfo.descriptor)
                    info.name = name
                    info.version = version
                    cache[key] = info
                }
            }
        }
        return info
    }
    /**
     * Obtains the 'base' plugin descriptor, which is the plugin descriptor of the current plugin project.
     */
    @CompileStatic
    Resource getBasePluginDescriptor() {
        Resource basePluginDescriptor = (Resource)cache['basePluginDescriptor']
        if (!basePluginDescriptor) {
            basePluginDescriptor = getDescriptorForPlugin(
                    new FileSystemResource(buildSettings.baseDir.absolutePath))
            if (basePluginDescriptor) {
                cache['basePluginDescriptor'] = basePluginDescriptor
            }
        }
        return basePluginDescriptor
    }

    /**
     * Returns the descriptor location for the given plugin directory. The descriptor is the Groovy
     * file that ends with *GrailsPlugin.groovy
     */
//    @CompileStatic
    Resource getDescriptorForPlugin(Resource pluginDir) {
        FileSystemResource descriptor = null
        File baseFile = pluginDir.file.canonicalFile
        File basePluginFile = (File)baseFile.listFiles().find {  File it -> it.name.endsWith("GrailsPlugin.groovy")}

        if (basePluginFile?.exists()) {
            descriptor = new FileSystemResource(basePluginFile)
        }
        return descriptor
    }

    //@CompileStatic
    private Resource[] resolveResources(String key, boolean processExcludes, Closure c) {
        Resource[] resources = (Resource[])cache[key]
        if (!resources) {
            resources = new Resource[0]
            resources = resolvePluginResourcesAndAdd(resources, processExcludes, c)
            cache[key] = resources
        }
        return resources
    }

    /**
     * Takes a Resource[] and optional pluginsDirPath and goes through each plugin directory.
     * It will then used the provided resolving resolving closures to attempt to resolve a new
     * set of resources to add to the original passed array.
     *
     * A new array is then returned that contains any additiona plugin resources that were
     * resolved by the expression passed in the closure.
     */
    @CompileStatic
    private resolvePluginResourcesAndAdd(Resource[] originalResources, boolean processExcludes, Closure resolver) {

        Resource[] pluginDirs = getPluginDirectories()
        AntPathMatcher pathMatcher = new AntPathMatcher()
        for (Resource dir in pluginDirs) {
            def newResources = dir ? resolver(dir.file.absolutePath) : null
            if (newResources) {
                if (processExcludes) {
                    def excludes = EXCLUDED_RESOURCES
                    newResources = newResources.findAll { Resource r ->
                        def relPath = relativePath(dir.file, r.file)
                        !excludes.any { String it ->
                            pathMatcher.match(it, relPath)
                        }
                    }
                }
                originalResources = (Resource[])IOUtils.addAll(originalResources, newResources as Resource[])
            }
        }
        return originalResources
    }

    @CompileStatic
    private String relativePath(File relbase, File file) {
        def pathParts = []
        def currentFile = file
        while (currentFile != null && currentFile != relbase) {
            pathParts += currentFile.name
            currentFile = currentFile.parentFile
        }
        pathParts.reverse().join('/')
    }
}
