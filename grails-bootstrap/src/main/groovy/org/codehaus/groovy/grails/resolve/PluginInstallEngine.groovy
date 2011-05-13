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
package org.codehaus.groovy.grails.resolve

import grails.util.BuildSettings
import grails.util.GrailsNameUtils
import grails.util.Metadata
import grails.util.PluginBuildSettings
import groovy.util.slurpersupport.GPathResult

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.regex.Pattern

import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.report.ResolveReport
import org.codehaus.groovy.grails.cli.CommandLineHelper
import org.codehaus.groovy.grails.cli.ScriptExitException
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.springframework.core.io.Resource

/**
 * Manages the installation and uninstallation of plugins from a Grails project.
 *
 * @author Graeme Rocher
 * @since 1.3
 */
class PluginInstallEngine {

    static final CORE_PLUGINS = ['core', 'i18n','converters','mimeTypes', 'controllers','webflow', 'dataSource', 'domainClass', 'filters','logging', 'groovyPages']

    Closure errorHandler = { String msg -> throw new ScriptExitException(msg) }
    Closure eventHandler = { String name, String msg -> println msg }
    Closure pluginScriptRunner
    Closure postInstallEvent
    Closure postUninstallEvent
    /**
     * plugins that were installed in the last execution of installPlugin
     */
    List installedPlugins = []
    def pluginDirVariableStore = [:]
    boolean isInteractive = true
    CommandLineHelper commandLineHelper = new CommandLineHelper(System.out)

    protected Metadata metadata
    protected PluginBuildSettings pluginSettings
    protected BuildSettings settings
    protected applicationPluginsLocation
    protected globalPluginsLocation
    protected ant
    protected PluginResolveEngine resolveEngine

    PluginInstallEngine(grails.util.BuildSettings settings) {
        this(settings, new PluginBuildSettings(settings), Metadata.current, new AntBuilder())
    }

    PluginInstallEngine(grails.util.BuildSettings settings, grails.util.PluginBuildSettings pbs) {
        this(settings, pbs, Metadata.current, new AntBuilder())
    }

    PluginInstallEngine(grails.util.BuildSettings settings, grails.util.PluginBuildSettings pbs, Metadata md) {
        this(settings, pbs, md, new AntBuilder())
    }

    PluginInstallEngine(grails.util.BuildSettings settings, grails.util.PluginBuildSettings pbs, Metadata md, AntBuilder ant) {
        if (settings == null) throw new IllegalArgumentException("Argument [settings] cannot be null")
        if (pbs == null) throw new IllegalArgumentException("Argument [pbs] cannot be null")
        if (md == null) throw new IllegalArgumentException("Argument [md] cannot be null")
        if (ant== null) throw new IllegalArgumentException("Argument [ant] cannot be null")

        globalPluginsLocation = settings.globalPluginsDir
        applicationPluginsLocation = settings.getProjectPluginsDir()
        pluginSettings = pbs
        this.settings = settings
        this.ant = ant
        this.metadata = md
        this.resolveEngine = new PluginResolveEngine(settings.dependencyManager, settings)
    }

    /**
    * This method will resolve the current dependencies and install any missing plugins or upgrades
    * and remove any plugins that aren't present in the metadata but are installed
    */
    void resolvePluginDependencies() {

        IvyDependencyManager dependencyManager = settings.dependencyManager

        // Get the plugin dependency descriptors for the max version of each applicable dependency
        def pluginDescriptors = dependencyManager.effectivePluginDependencyDescriptors
        
        def newPlugins = findMissingOrUpgradePlugins(pluginDescriptors)
        if (newPlugins) {
            eventHandler "StatusUpdate", "Installing ${newPlugins.size} plugins, please wait"
            installPlugins(newPlugins)
        }

        def existingPlugins = pluginDescriptors.findAll { !newPlugins.contains(it) }
        def rootChangingPattern = dependencyManager.chainResolver.changingPattern
        def rootChangingPatternCompiled = rootChangingPattern ? Pattern.compile(rootChangingPattern) : null
        def changingPlugins = existingPlugins.findAll {
            it.changing || rootChangingPatternCompiled?.matcher(it.dependencyRevisionId.revision)?.matches()
        }
        if (changingPlugins) {
            def noChangingPlugins = changingPlugins.size()
            eventHandler "StatusBegin", "Checking ${noChangingPlugins} snapshot plugin${noChangingPlugins > 1 ? 's' : ''} for remote updates, please wait"
            installPlugins(existingPlugins)
            eventHandler "StatusEnd", "Snapshot plugin checking complete"
        }

        checkPluginsToUninstall(pluginDescriptors.collect { it.dependencyRevisionId })
    }

    /**
     * Installs a list of plugins
     *
     * @param params A list of plugins defined each by a ModuleRevisionId
     */
    void installPlugins(Collection<EnhancedDefaultDependencyDescriptor> plugins) {
        if (plugins) {
            ResolveReport report = resolveEngine.resolvePlugins(plugins)
            if (report.hasError()) {
                errorHandler "Failed to resolve plugins."
            }
            else {
                for (ArtifactDownloadReport ar in report.getArtifactsReports(null, false)) {
                    def arName = ar.artifact.moduleRevisionId.name
                    if (plugins.any { it.dependencyRevisionId.name == arName }) {
                        installPlugin ar.localFile
                    }
                }
            }
        }
    }

    /**
     * Installs a plugin for the given name and optional version
     *
     * @param name The plugin name
     * @param version The plugin version (optional)
     * @param globalInstall Whether to install globally or not (optional)
     */
    void installPlugin(String name, String version = null, boolean globalInstall = false) {

        installedPlugins.clear()
        def pluginZip = resolveEngine.resolvePluginZip(name, version)

        if (!pluginZip) {
            errorHandler "Plugin not found for name [$name] and version [${version ?: 'not specified'}]"
        }

        try {
            (name, version) = readMetadataFromZip(pluginZip.absolutePath)
            installPluginZipInternal(name, version, pluginZip, globalInstall)
        }
        catch (e) {
            errorHandler "Error installing plugin: ${e.message}"
        }
    }

    /**
     * Installs a plugin from the given ZIP file
     *
     * @param zipFile The plugin zip file
     * @param globalInstall Whether it is a global install or not (optional)
     */
    void installPlugin(File zipFile, boolean globalInstall = false, boolean overwrite = false) {

        if (zipFile.exists()) {
            def (name, version) = readMetadataFromZip(zipFile.absolutePath)
            installPluginZipInternal name, version, zipFile, globalInstall, overwrite
        }
        else {
            errorHandler "Plugin zip not found at location: ${zipFile.absolutePath}"
        }
    }

    /**
     * Installs a plugin from the given URL
     *
     * @param zipURL The zip URL
     * @param globalInstall Whether it is a global install or not (optional)
     */
    void installPlugin(URL zipURL, boolean globalInstall = false) {
        def s = zipURL.toString()
        def filename = s[s.lastIndexOf("/")..-1]
        def file = File.createTempFile(filename[0..-4], ".zip")
        file.deleteOnExit()
        eventHandler "StatusUpdate", "Downloading zip ${zipURL}. Please wait..."
        try {
            ant.get(src: zipURL, dest: file, verbose:"on")
        }
        catch (e) {
            errorHandler "Error downloading plugin ${zipURL}: ${e.message}"
        }
        installPlugin(file, globalInstall, true)
    }

    protected void installPluginZipInternal(String name, String version, File pluginZip,
            boolean globalInstall = false, boolean overwrite = false) {

        def fullPluginName = "$name-$version"
        def pluginInstallPath = "${globalInstall ? globalPluginsLocation : applicationPluginsLocation}/${fullPluginName}"

        assertNoExistingInlinePlugin(name)

        def abort = checkExistingPluginInstall(name, version)

        if (abort && !overwrite) {
            registerPluginWithMetadata(name, version)
            return
        }

        eventHandler "StatusUpdate", "Installing zip ${pluginZip}..."

        installedPlugins << pluginInstallPath

        if (isNotInlinePluginLocation(new File(pluginInstallPath))) {
            ant.delete(dir: pluginInstallPath, failonerror: false)
            ant.mkdir(dir: pluginInstallPath)
            ant.unzip(dest: pluginInstallPath, src: pluginZip)
            eventHandler "StatusUpdate", "Installed plugin ${fullPluginName} to location ${pluginInstallPath}."
        }
        else {
            errorHandler "Cannot install plugin. Plugin install would override inline plugin configuration which is not allowed."
        }

        def pluginXmlFile = new File("${pluginInstallPath}/plugin.xml")
        if (!pluginXmlFile.exists()) {
            errorHandler("Plugin $fullPluginName is not a valid Grails plugin. No plugin.xml descriptor found!")
        }
        def pluginXml = new XmlSlurper().parse(pluginXmlFile)
        def pluginName = pluginXml.@name.toString()
        def pluginVersion = pluginXml.@version.toString()
        def pluginGrailsVersion = pluginXml.@grailsVersion.toString()
        assertGrailsVersionValid(fullPluginName, pluginGrailsVersion)

        // Add the plugin's directory to the binding so that any event
        // handlers in the plugin have access to it. Normally, this
        // variable is added in GrailsScriptRunner, but this plugin
        // hasn't been installed by that point.
        pluginDirVariableStore["${GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(pluginName)}PluginDir"] = new File(pluginInstallPath).absoluteFile

        def dependencies = processPluginDependencies(pluginName,pluginXml)

        // if there are any unprocessed dependencies, bail out
        if (dependencies) {
            ant.delete(dir: "${pluginInstallPath}", quiet: true, failOnError: false)
            errorHandler("Failed to install plugin [${fullPluginName}]. Missing dependencies: ${dependencies.inspect()}")
        }
        else {
            resolvePluginJarDependencies(fullPluginName, pluginInstallPath)

            // proceed _Install.groovy plugin script if exists
            def installScript = new File("${pluginInstallPath}/scripts/_Install.groovy")
            runPluginScript(installScript, fullPluginName, "post-install script")

            registerPluginWithMetadata(pluginName, pluginVersion)

            displayNewScripts(fullPluginName, pluginInstallPath)

            postInstall(pluginInstallPath)
            eventHandler("PluginInstalled", fullPluginName)
        }
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

            errorHandler("Plugin $zipLocation is not a valid Grails plugin. No plugin.xml descriptor found!")
        }
        catch (e) {
            errorHandler("Error reading plugin zip [$zipLocation]. The plugin zip file may be corrupt.")
        }
    }

    /**
     * Checks an existing plugin path to install and returns true if the installation should be aborted or false if it should continue
     *
     * If an error occurs the errorHandler is invoked.
     *
     * @param name The plugin name
     * @param version The plugin version
     * @return True if the installation should be aborted
     */
    protected boolean checkExistingPluginInstall(String name, version) {
        Resource currentInstall = pluginSettings.getPluginDirForName(name)

        if (currentInstall?.exists()) {

            PluginBuildSettings pluginSettings = pluginSettings
            def pluginDir = currentInstall.file.canonicalFile
            def pluginInfo = pluginSettings.getPluginInfo(pluginDir.absolutePath)
            // if the versions are the same no need to continue
            if (version == pluginInfo?.version) return true

            if (pluginSettings.isInlinePluginLocation(currentInstall)) {
                errorHandler("The plugin you are trying to install [$name-${version}] is already configured as an inplace plugin in grails-app/conf/BuildConfig.groovy. You cannot overwrite inplace plugins.");
                return true
            }
            else if (!isInteractive || confirmInput("You currently already have a version of the plugin installed [$pluginDir.name]. Do you want to upgrade this version?")) {
                ant.delete(dir: currentInstall.file)
            }
            else {
                eventHandler("StatusUpdate", "Plugin $name-$version install aborted");
                return true
            }
        }
        return false
    }

    protected void assertNoExistingInlinePlugin(String name) {
        def inlinePlugins = settings.config.grails.plugin.location
        
        if (inlinePlugins.containsKey(name)) {
            def pluginReference = inlinePlugins[name]
            errorHandler("""\
Plugin [$name] is aliased as [grails.plugin.location.$name] to the location [$pluginReference] in grails-app/conf/BuildConfig.groovy.
You cannot upgrade a plugin that is configured via BuildConfig.groovy, remove the configuration to continue.""");
        }
    }

    protected void displayNewScripts(pluginName, installPath) {
        def providedScripts = new File("${installPath}/scripts").listFiles().findAll { !it.name.startsWith('_') && it.name.endsWith(".groovy")}
        eventHandler("StatusFinal", "Plugin ${pluginName} installed")
        if (providedScripts) {
            println "Plugin provides the following new scripts:"
            println "------------------------------------------"
            providedScripts.each { File file ->
                def scriptName = GrailsNameUtils.getScriptName(file.name)
                println "grails ${scriptName}"
            }
        }
    }

    protected void resolvePluginJarDependencies(pluginName, pluginInstallPath) {
        def pluginDependencyDescriptor = new File("$pluginInstallPath/dependencies.groovy")
        if (pluginDependencyDescriptor.exists()) {
            eventHandler "StatusUpdate", "Resolving plugin JAR dependencies"
            def callable = settings.pluginDependencyHandler()
            callable.call(new File("$pluginInstallPath"))
            IvyDependencyManager dependencyManager = settings.dependencyManager
            dependencyManager.resetGrailsPluginsResolver()

            def dependencyConfigurationsToAdd = [IvyDependencyManager.RUNTIME_CONFIGURATION]
            if (settings.grailsEnv == "test") {
                dependencyConfigurationsToAdd << IvyDependencyManager.TEST_CONFIGURATION
            }

            for (dependencyConfiguration in dependencyConfigurationsToAdd) {
                def resolveReport = dependencyManager.resolveDependencies(dependencyConfiguration)
                if (resolveReport.hasError()) {
                    errorHandler("Failed to install plugin [${pluginName}]. Plugin has missing JAR dependencies.")
                }
                else {
                    addJarsToRootLoader resolveReport.getArtifactsReports(null, false).localFile
                }
            }
        }
        def pluginJars = new File("${pluginInstallPath}/lib").listFiles().findAll { it.name.endsWith(".jar")}
        addJarsToRootLoader(pluginJars)
    }

    protected addJarsToRootLoader(Collection pluginJars) {
        def loader = getClass().classLoader.rootLoader
        for (File jar in pluginJars) {
            loader.addURL(jar.toURI().toURL())
        }
    }

    protected Map processPluginDependencies(String pluginName, GPathResult pluginXml) {
        Map dependencies = [:]
        for (dep in pluginXml.dependencies.plugin) {
            def depName = dep.@name.toString()
            String depVersion = dep.@version.toString()
            if (isCorePlugin(depName)) {
                def grailsVersion = settings.getGrailsVersion()
                if (!GrailsPluginUtils.isValidVersion(grailsVersion, depVersion)) {
                    errorHandler("Plugin requires version [$depVersion] of Grails core, but installed version is [${grailsVersion}]. Please upgrade your Grails installation and try again.")
                }
            }
            else {
                dependencies[depName] = depVersion
                def depDirName = GrailsNameUtils.getScriptName(depName)
                def manager = settings.dependencyManager

                if (manager.isExcludedFromPlugin(pluginName, depDirName)) {
                    dependencies.remove(depName)
                }
                else {
                    def depPluginDir = pluginSettings.getPluginDirForName(depDirName)?.file
                    if (!depPluginDir?.exists()) {
                        eventHandler("StatusUpdate", "Plugin dependency [$depName] not found. Attempting to resolve...")
                        // recursively install dependent plugins
                        def upperVersion = GrailsPluginUtils.getUpperVersion(depVersion)
                        def installVersion = upperVersion
                        if (installVersion == '*') {
                            installVersion = settings.defaultPluginSet.contains(depDirName) ? settings.getGrailsVersion() : null
                        }

                        // recurse
                        installPlugin(depDirName, installVersion)

                        dependencies.remove(depName)
                    }
                    else {
                        def dependency = readPluginXmlMetadata(depDirName)
                        def dependencyVersion = dependency.@version.toString()
                        if (!GrailsPluginUtils.isValidVersion(dependencyVersion, depVersion)) {
                            errorHandler("Plugin requires version [$depVersion] of plugin [$depName], but installed version is [${dependencyVersion}]. Please upgrade this plugin and try again.")
                        }
                        else {
                            dependencies.remove(depName)
                        }
                    }
                }
            }
        }
        return dependencies
    }

    protected readPluginXmlMetadata(String pluginDirName) {
        def pluginDir = pluginSettings.getPluginDirForName(pluginDirName)?.file
        new XmlSlurper().parse(new File("${pluginDir}/plugin.xml"))
    }

    private assertGrailsVersionValid(String pluginName, String grailsVersion) {
        if (grailsVersion) {
            if (!GrailsPluginUtils.isValidVersion(settings.grailsVersion, grailsVersion)) {
                errorHandler("Plugin $pluginName requires version [${grailsVersion}] of Grails which your current Grails installation does not meet. Please try install a different version of the plugin or Grails.")
            }
        }
    }

    /**
     * Uninstalls a plugin for the given name and optional version
     *
     * @param name The plugin name
     * @param version The version
     */
    void uninstallPlugin(String name, String version = null) {
        try {
            String pluginKey = "plugins.$name"
            metadata.remove(pluginKey)
            metadata.persist()
            def pluginDir
            if (name && version) {
                pluginDir = new File("${applicationPluginsLocation}/$name-$version")
            }
            else {
                pluginDir = pluginSettings.getPluginDirForName(name)?.file
            }

            if (pluginDir?.exists()) {
                def uninstallScript = new File("${pluginDir}/scripts/_Uninstall.groovy")
                runPluginScript(uninstallScript, pluginDir.canonicalFile.name, "uninstall script")
                if (isNotInlinePluginLocation(pluginDir)) {
                    ant.delete(dir:pluginDir, failonerror:true)
                }
                postUninstall()
                eventHandler "PluginUninstalled", "Uninstalled plugin [${name}]."
            }
            else {
                errorHandler("No plugin [$name${version ? '-' + version : ''}] installed, cannot uninstall")
            }
        }
        catch (e) {
            e.printStackTrace()
            errorHandler("An error occured installing the plugin [$name${version ? '-' + version : ''}]: ${e.message}")
        }
    }

    /**
     * Registers a plugin name and version as installed according to the plugin metadata
     * @param pluginName the name of the plugin
     * @param pluginVersion the version of the plugin
     */
    void registerPluginWithMetadata(String pluginName, pluginVersion) {
        IvyDependencyManager dependencyManager = settings.getDependencyManager()

        // only register in metadata if not specified in BuildConfig.groovy
        if (dependencyManager.metadataRegisteredPluginNames?.contains(pluginName)) {
            metadata['plugins.' + pluginName] = pluginVersion
            metadata.persist()
        }
        else {
            if (!dependencyManager.pluginDependencyNames?.contains(pluginName)) {
                metadata['plugins.' + pluginName] = pluginVersion
                metadata.persist()
            }
        }
    }

    private void runPluginScript( File scriptFile, fullPluginName, msg ) {
        if (pluginScriptRunner != null) {
            if (pluginScriptRunner.maximumNumberOfParameters < 3) {
                throw new IllegalStateException("The [pluginScriptRunner] closure property must accept at least 3 arguments")
            }
            pluginScriptRunner.call(scriptFile, fullPluginName, msg)
        }
    }

    /**
     * Check to see if the plugin directory is in plugins home.
     */
    boolean isNotInlinePluginLocation(File pluginDir) {
        // insure all the directory is in the pluginsHome
        checkPluginPathWithPluginDir(applicationPluginsLocation, pluginDir) ||
            checkPluginPathWithPluginDir(globalPluginsLocation, pluginDir)
    }

    protected postUninstall() {
        // Update the cached dependencies in grailsSettings, and add new jars to the root loader
        resolveDependenciesAgain()
        pluginSettings.clearCache()
        postUninstallEvent?.call()
    }

    protected postInstall(String pluginInstallPath) {
        // Update the cached dependencies in grailsSettings, and add new jars to the root loader
        pluginSettings.clearCache()
        postInstallEvent?.call(pluginInstallPath)
    }

    private checkPluginPathWithPluginDir (File pluginsHome, File pluginDir) {
        def absPluginsHome = pluginsHome.absolutePath
        pluginDir.absolutePath.startsWith(absPluginsHome)
    }

    private void resolveDependenciesAgain() {
        for (type in ['compile', 'build', 'test', 'runtime']) {
            def existing = settings."${type}Dependencies"
            def all = settings.dependencyManager.resolveDependencies(IvyDependencyManager."${type.toUpperCase()}_CONFIGURATION").getArtifactsReports(null, false).localFile
            def toAdd = all - existing
            if (toAdd) {
                existing.addAll(toAdd)
                if (type in ['build', 'test']) {
                    toAdd.each {
                        if (it) {
                            settings.rootLoader.addURL(it.toURL())
                        }
                    }
                }
            }
        }
    }

    private boolean isCorePlugin(name)  {
        CORE_PLUGINS.contains(name)
    }

    private boolean confirmInput(String msg) {
        commandLineHelper.userInput(msg, ['y','n'] as String[]) == 'y'
    }

    protected void checkPluginsToUninstall(List<DependencyDescriptor> pluginDeps) {
        // Find out which plugins are in the search path but not in the
        // metadata. We only check on the plugins in the project's "plugins"
        // directory and the global "plugins" dir. Plugins loaded via an
        // explicit path should be left alone.
        def pluginDirs = pluginSettings.implicitPluginDirectories
        def pluginsToUninstall = pluginDirs.findAll { Resource r ->
            !pluginDeps.find {  ModuleRevisionId plugin ->
                r.filename ==~ "$plugin.name-.+"
            }
        }

        for (Resource pluginDir in pluginsToUninstall) {
            if (pluginSettings.isGlobalPluginLocation(pluginDir)) {
                registerMetadataForPluginLocation(pluginDir)
            }
            else {
                if (!isInteractive || confirmInput("Plugin [${pluginDir.filename}] is installed, but was not found in the application's metadata, do you want to uninstall?")) {
                    uninstallPlugin(pluginDir.filename)
                }
                else {
                    registerMetadataForPluginLocation(pluginDir)
                }
            }
        }
    }

    protected Collection<EnhancedDefaultDependencyDescriptor> findMissingOrUpgradePlugins(Collection<EnhancedDefaultDependencyDescriptor> descriptors) {
        def pluginsToInstall = []
        for (descriptor in descriptors) {
            def p = descriptor.dependencyRevisionId
            def name = p.name
            def version = p.revision

            def fullName = "$name-$version"
            def pluginInfo = pluginSettings.getPluginInfoForName(name)
            def pluginLoc = pluginInfo?.pluginDir

            if (!pluginLoc?.exists()) {
                eventHandler "StatusUpdate", "Plugin [${fullName}] not installed."
                pluginsToInstall << descriptor
            }
            else if (pluginLoc) {

                if (version.startsWith("latest.")) {
                    pluginsToInstall << descriptor
                }
                else {
                    def dirName = pluginLoc.file.canonicalFile.name
                    PluginBuildSettings settings = pluginSettings
                    if (pluginInfo.version != version &&
                        !settings.isInlinePluginLocation(pluginLoc) &&
                        !pluginsToInstall.contains(p)) {
                        // only print message if the version doesn't start with "latest." since we have
                        // to do a check for a new version when there version is specified as "latest.integration" etc.
                        if (!version.startsWith("latest.")) {
                            def upgrading = GrailsPluginUtils.isVersionGreaterThan(pluginInfo.version, version)
                            def action = upgrading ? "Upgrading" : "Downgrading"
                            eventHandler "StatusUpdate", "$action plugin [$dirName] to [${fullName}]."
                        }

                        pluginsToInstall << descriptor
                    }
                }
            }
        }
        return pluginsToInstall
    }

    private registerMetadataForPluginLocation(Resource pluginDir) {
        def plugin = pluginSettings.getMetadataForPlugin(pluginDir.filename)
        registerPluginWithMetadata(plugin.@name.text(), plugin.@version.text())
    }
}
