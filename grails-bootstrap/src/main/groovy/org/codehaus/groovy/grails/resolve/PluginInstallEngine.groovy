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

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.GrailsNameUtils
import grails.util.Metadata
import grails.util.PluginBuildSettings
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult

import org.apache.ivy.core.module.descriptor.Configuration
import org.apache.ivy.plugins.latest.LatestTimeStrategy
import org.apache.ivy.plugins.resolver.ChainResolver
import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.codehaus.groovy.grails.cli.ScriptExitException
import org.codehaus.groovy.grails.io.support.FileSystemResource
import org.codehaus.groovy.grails.io.support.Resource
import org.codehaus.groovy.grails.plugins.BasicGrailsPluginInfo
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.plugins.GrailsVersionUtils
import org.codehaus.groovy.grails.cli.interactive.InteractiveMode

/**
 * Manages the installation and uninstallation of plugins from a Grails project.
 *
 * @author Graeme Rocher
 * @since 1.3
 */
class PluginInstallEngine {

    static final List CORE_PLUGINS = [
        'codecs', 'controllers', 'converters', 'core', 'dataSource', 'domainClass',
        'filters', 'groovyPages', 'i18n', 'logging', 'mimeTypes',
        'services', 'servlets', 'urlMappings', 'validation']

    Closure errorHandler = { String msg -> throw new ScriptExitException(msg) }
    Closure eventHandler = { String name, String msg -> GrailsConsole.instance.updateStatus msg }
    Closure pluginScriptRunner
    Closure postInstallEvent
    Closure postUninstallEvent
    /**
     * plugins that were installed in the last  execution of installPlugin
     */
    List installedPlugins = []
    def pluginDirVariableStore = [:]
    boolean isInteractive = true

    protected Metadata metadata
    protected PluginBuildSettings pluginSettings
    protected BuildSettings settings
    protected applicationPluginsLocation
    protected ant
    protected PluginResolveEngine resolveEngine

    PluginInstallEngine(BuildSettings settings) {
        this(settings, GrailsPluginUtils.getPluginBuildSettings(settings), Metadata.current, new AntBuilder())
    }

    PluginInstallEngine(BuildSettings settings, PluginBuildSettings pbs) {
        this(settings, pbs, Metadata.current, new AntBuilder())
    }

    PluginInstallEngine(BuildSettings settings, PluginBuildSettings pbs, Metadata md) {
        this(settings, pbs, md, new AntBuilder())
    }

    PluginInstallEngine(BuildSettings settings, PluginBuildSettings pbs, Metadata md, AntBuilder ant) {
        if (settings == null) throw new IllegalArgumentException("Argument [settings] cannot be null")
        if (pbs == null) throw new IllegalArgumentException("Argument [pbs] cannot be null")
        if (md == null) throw new IllegalArgumentException("Argument [md] cannot be null")
        if (ant== null) throw new IllegalArgumentException("Argument [ant] cannot be null")

        applicationPluginsLocation = settings.getProjectPluginsDir()
        pluginSettings = pbs
        this.settings = settings
        this.ant = ant
        metadata = md
        resolveEngine = new PluginResolveEngine(settings.dependencyManager, settings)
    }

    @CompileStatic
    void resolveAndInstallDepdendencies() {
        // we get the 'build' and 'test' dependencies because that is the scope that
        // includes all possible plugins in all scopes
        def pluginZips = (settings.pluginTestDependencies + settings.pluginBuildDependencies)
        installResolvePlugins(pluginZips)
        checkPluginsToUninstall(pluginZips.toList())
    }
    @CompileStatic
    void installedResolvedPlugins() {
        def pluginZips = (settings.pluginTestDependencies + settings.pluginBuildDependencies)
        installResolvePlugins(pluginZips)
    }
    @CompileStatic
    protected void installResolvePlugins(Collection<File> pluginZips) {
        try {
            for (zip in pluginZips) {
                installResolvedPlugin(zip)
            }
        } finally {
            final im = InteractiveMode.current
            if(im) {
                im.refresh()
            }
        }
    }

    @CompileStatic
    void checkPluginsToUninstall() {
        def pluginZips = (settings.pluginTestDependencies + settings.pluginBuildDependencies).toList()
        checkPluginsToUninstall(pluginZips)
    }

    @CompileStatic
    void checkPluginsToUninstall(List<File> pluginZips) {

        List<GrailsPluginInfo> resolvedPluginInfos = pluginZips.collect { File f -> readPluginInfoFromZip(f.absolutePath) }

        GrailsPluginInfo[] installedPluginInfos = pluginSettings.getPluginInfos()

        def pluginsToUninstall = installedPluginInfos.findAll { GrailsPluginInfo info -> !resolvedPluginInfos.any { GrailsPluginInfo resolvedInfo -> info.fullName == resolvedInfo.fullName}}

        for (GrailsPluginInfo pluginInfo in pluginsToUninstall) {
            Resource pluginDir = pluginInfo.pluginDir
            final pluginDirFile = pluginDir.file.canonicalFile
            if ((pluginDirFile == settings.baseDir) || settings.isInlinePluginLocation(pluginDirFile)) continue

            if (pluginSettings.isGlobalPluginLocation(pluginDir)) {
                uninstallPlugin(pluginInfo.name, pluginInfo.version)
            }
            else {
                uninstallPlugin(pluginInfo.name, pluginInfo.version)
            }
        }
    }

    /**
     * Installs a plugin for the given name and optional version.
     *
     * @param name The plugin name
     * @param version The plugin version (optional)
     * @return true if installed and not cancelled by user
     */
    boolean installPlugin(String name, String version = null) {

        installedPlugins.clear()
        def pluginZip = resolveEngine.resolvePluginZip(name, version)

        if (!pluginZip) {
            errorHandler "Plugin not found for name [$name] and version [${version ?: 'not specified'}]"
        }

        try {
            (name, version) = readMetadataFromZip(pluginZip.absolutePath)
            return installPluginZipInternal(name, version, pluginZip)
        }
        catch (e) {
            errorHandler "Error installing plugin: ${e.message}"
        }
    }

    /**
     * Installs a plugin from the given ZIP file. Differs from #installPlugin(zipFile) in that the plugin to be installed is assumed to already be resolved and
     * hence not placed in the users local cache
     *
     * @param zipFile The plugin zip file
     */
    boolean installResolvedPlugin(File zipFile) {
        def inlinePlugins = settings.config.grails.plugin.location

        if (!zipFile.exists()) {
            errorHandler "Plugin zip not found at location: ${zipFile.absolutePath}. Potential corrupt cache. Try running: grails --refresh-dependencies compile"
        }

        def (name, version) = readMetadataFromZip(zipFile.absolutePath)

        /*
         * Determine if the plugin is currently configured to be used inline.
         * If the plugin is NOT configured to run inline, install it.
         * Otherwise, remove the previously installed ZIP file if present
         * to prevent duplicate class errors during compilation.
         */
        if (!inlinePlugins.find {
            def pluginName = it.key.toString()
            if (pluginName.contains(':')) {
                pluginName = pluginName.split(':')[-1]
            }
            return pluginName.equals(name)
        } ) {
            installPluginZipInternal name, version, zipFile, false, true
        } else {
            // Remove the plugin to prevent duplicate class compile errors with inline version.
            uninstallPlugin name, version
        }
    }

    /**
     * Installs a plugin from the given ZIP file
     *
     * @param zipFile The plugin zip file
     */
    boolean installPlugin(File zipFile, boolean overwrite = false) {

        if (!zipFile.exists()) {
            errorHandler "Plugin zip not found at location: ${zipFile.absolutePath}. Potential corrupt cache. Try running: grails --refresh-dependencies compile"
        }

        def (name, version) = readMetadataFromZip(zipFile.absolutePath)

        def parentDir = zipFile.canonicalFile.parentFile
        final currentDependencyManager = resolveEngine.dependencyManager
        final ivySettings = currentDependencyManager.ivySettings
        IvyDependencyManager dependencyManager = new IvyDependencyManager(currentDependencyManager.applicationName,
            currentDependencyManager.applicationVersion, settings, Metadata.current, ivySettings)
        dependencyManager.chainResolver = new ChainResolver(name: "chain", settings: ivySettings)
        dependencyManager.parseDependencies {
            log "warn"
            useOrigin true
            cacheDir ivySettings.getDefaultCache().absolutePath
            repositories {
                grailsHome()
                def pluginResolver = new FileSystemResolver(name: "$name plugin install resolver")
                final parentPath = parentDir.canonicalPath
                pluginResolver.addArtifactPattern("${parentPath}/[module]-[revision].[ext]")
                pluginResolver.addArtifactPattern("${parentPath}/grails-[module]-[revision].[ext]")
                pluginResolver.settings = ivySettings
                pluginResolver.latestStrategy = new LatestTimeStrategy()
                pluginResolver.changingPattern = ".*SNAPSHOT"
                pluginResolver.setCheckmodified(true)
                resolver pluginResolver
            }
            plugins {
                compile ":$name:$version"
            }
        }
        final report = dependencyManager.resolveDependencies()
        if (report.hasError()) {
            errorHandler "Could not resolve all dependencies for plugin $name"
        }

        final reports = report.allArtifactsReports
        if (!reports) {
            return
        }

        final localFile = reports[0].localFile
        ant.mkdir(dir:"${settings.grailsWorkDir}/cached-installed-plugins")
        ant.copy(file:localFile, tofile:"${settings.grailsWorkDir}/cached-installed-plugins/$name-${version}.zip")
        installPluginZipInternal name, version, localFile, overwrite
    }

    /**
     * Installs a plugin from the given URL
     *
     * @param zipURL The zip URL
     */
    boolean installPlugin(URL zipURL) {
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
        installPlugin(file, true)
    }

    protected boolean installPluginZipInternal(String name, String version, File pluginZip,
            boolean overwrite = false, boolean isResolve = false) {

        def fullPluginName = "$name-$version"
        def pluginInstallPath = "$applicationPluginsLocation/$fullPluginName"

        assertNoExistingInlinePlugin(name)

        def abort = checkExistingPluginInstall(name, version, pluginZip,isResolve)
        if (abort && !overwrite) {
            return false
        }

        eventHandler "StatusUpdate", "Installing zip ${pluginZip.name}..."

        installedPlugins << pluginInstallPath

        if (isNotInlinePluginLocation(new File(pluginInstallPath))) {
            ant.delete(dir: pluginInstallPath, failonerror: false)
            ant.mkdir(dir: pluginInstallPath)
            ant.unzip(dest: pluginInstallPath, src: pluginZip)
            eventHandler "StatusUpdate", "Installed plugin ${fullPluginName}"
        }
        else {
            errorHandler "Cannot install plugin. Plugin install would override inline plugin configuration which is not allowed."
        }

        def pluginXmlFile = new File(pluginInstallPath, "plugin.xml")
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

        def runtimeDependencies = processPluginDependencies(pluginName,pluginXml)

        // proceed _Install.groovy plugin script if exists
        def installScript = new File(pluginInstallPath, "scripts/_Install.groovy")
        runPluginScript(installScript, fullPluginName, "post-install script")

        pluginSettings.clearCache()
        pluginSettings.registerNewPluginInstall(pluginZip)

        postInstall(pluginInstallPath)
        eventHandler("PluginInstalled", fullPluginName)

        true
    }

    /**
     * Reads plugin metadata from a plugin zip file and returns a list containing the plugin name, version and
     * XML metadata. Designed for use with Groovy's multiple assignment operator
     *
     * @param zipLocation The zip location
     * @return A list
     */
    List readMetadataFromZip(String zipLocation) {
        def list = pluginSettings.readMetadataFromZip(zipLocation)
        if (list == null)  {
           errorHandler "Zip $zipLocation is not a valid plugin"
        }
        return list
    }

    /**
     * Reads plugin info from the zip file location
     *
     * @param zipLocation The zip location
     * @return
     */
    GrailsPluginInfo readPluginInfoFromZip(String zipLocation) {
        def (name, version, xml) = readMetadataFromZip(zipLocation)

        def info = new BasicGrailsPluginInfo(new FileSystemResource(new File(zipLocation)))
        info.name = name
        info.version = version
        return info
    }

    /**
     * Checks an existing plugin path to install and returns true if the installation should be aborted or false if it should continue
     *
     * If an error occurs the errorHandler is invoked.
     *
     * @param name The plugin name
     * @param version The plugin version
     * @return true if the installation should be aborted
     */
    protected boolean checkExistingPluginInstall(String name, version, File pluginZip, boolean isResolve = true) {
        Resource currentInstall = pluginSettings.getPluginDirForName(name)
        def inlinePlugins = settings.config.grails.plugin.location

        if (!currentInstall?.exists()) {
            return false
        }

        /*
         * If the plugin to be installed is currently configured to be inline,
         * do not install it.  This is because we want to use the inline over
         * the modified dependency artifact.  The comparison to find the inline
         * plugin uses "endsWith", as inline plugins can be declared with a full
         * vector in settings.groovy (i.e. 'com.mycompany:my-plugin")
         */
        if (inlinePlugins.find {
            def pluginName = it.key.toString()
            if (pluginName.contains(':')) {
                pluginName = pluginName.split(':')[-1]
            }
            return pluginName.equals(name)
        } ) {
            return true
        }

        PluginBuildSettings pluginSettings = pluginSettings
        def pluginDir = currentInstall.file.canonicalFile
        def pluginInfo = pluginSettings.getPluginInfo(pluginDir.absolutePath)
        // if the versions are the same no need to continue
        boolean isSnapshotUpdate = pluginDir.lastModified() < pluginZip.lastModified() && version.toString().endsWith("-SNAPSHOT")
        if (version == pluginInfo?.version && !isSnapshotUpdate) {
            if (!isResolve) {
                GrailsConsole.instance.addStatus("Plugin '$name' with version '${pluginInfo?.version}' is already installed")
            }
            return true
        }

        if (pluginSettings.isInlinePluginLocation(currentInstall)) {
            errorHandler("The plugin you are trying to install [$name-${version}] is already configured as an inplace plugin in grails-app/conf/BuildConfig.groovy. You cannot overwrite inplace plugins.")
            return true
        }

        def upgradeOrDowngrade = !pluginInfo || GrailsVersionUtils.isVersionGreaterThan(pluginInfo.version, version) ? 'upgrade' : 'downgrade'
        if (!isInteractive || isSnapshotUpdate || confirmInput("You currently already have a version of the plugin installed [${pluginDir.name}]. Do you want to $upgradeOrDowngrade to [$name-$version]? ")) {
            if (isSnapshotUpdate) {
                GrailsConsole.instance.addStatus("Updating snapshot plugin '$name' with version '${pluginInfo?.version}'")
            }
            ant.delete(dir: currentInstall.file)
            return false
        }

        eventHandler("StatusUpdate", "Plugin $name-$version install aborted")
        return true
    }

    protected void assertNoExistingInlinePlugin(String name) {
        def inlinePlugins = settings.config.grails.plugin.location

        if (inlinePlugins.containsKey(name)) {
            def pluginReference = inlinePlugins[name]
            errorHandler("""\
Plugin [$name] is aliased as [grails.plugin.location.$name] to the location [$pluginReference] in grails-app/conf/BuildConfig.groovy.
You cannot upgrade a plugin that is configured via BuildConfig.groovy, remove the configuration to continue.""")
        }
    }

    protected addJarsToRootLoader(Configuration dependencyConfiguration, Collection pluginJars) {
        def loader = getClass().classLoader.rootLoader
        for (File jar in pluginJars) {
            loader.addURL(jar.toURI().toURL())
        }

        switch(dependencyConfiguration) {
            case IvyDependencyManager.RUNTIME_CONFIGURATION:
                settings.runtimeDependencies.addAll(pluginJars)
                break
            case IvyDependencyManager.BUILD_CONFIGURATION:
                settings.buildDependencies.addAll(pluginJars)
                break
            case IvyDependencyManager.PROVIDED_CONFIGURATION:
                settings.providedDependencies.addAll(pluginJars)
                break
            case IvyDependencyManager.TEST_CONFIGURATION:
                settings.testDependencies.addAll(pluginJars)
                break
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
                def depDirName = GrailsNameUtils.getScriptName(depName)
                dependencies[depDirName] = depVersion
            }
        }
        return dependencies
    }

    protected readPluginXmlMetadata(String pluginDirName) {
        def pluginDir = pluginSettings.getPluginDirForName(pluginDirName)?.file
        new XmlSlurper().parse(new File(pluginDir, "plugin.xml"))
    }

    private assertGrailsVersionValid(String pluginName, String grailsVersion) {
        if (!grailsVersion || GrailsPluginUtils.isValidVersion(settings.grailsVersion, grailsVersion)) {
            return
        }

        errorHandler("Plugin " + pluginName + " requires version [" + grailsVersion +
                     "] of Grails which your current Grails installation does not meet. " +
                     "Please try install a different version of the plugin or Grails.")
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
            if (metadata.remove(pluginKey)) {
                metadata.persist()
            }
            def pluginDir
            if (name && version) {
                pluginDir = new File(applicationPluginsLocation, "$name-$version")
            }
            else {
                pluginDir = pluginSettings.getPluginDirForName(name)?.file
            }

            if (pluginDir?.exists()) {
                def uninstallScript = new File(pluginDir, "scripts/_Uninstall.groovy")
                runPluginScript(uninstallScript, pluginDir.canonicalFile.name, "uninstall script")
                if (isNotInlinePluginLocation(pluginDir)) {
                    ant.delete(dir:pluginDir, failonerror:true)
                }
                postUninstall()
                eventHandler "PluginUninstalled", "Uninstalled plugin [${name}]."
                GrailsConsole.getInstance().addStatus("Uninstalled plugin [$name]")
            }
            else {
                GrailsConsole.getInstance().warning("No plugin [$name${version ? '-' + version : ''}] installed, cannot uninstall")
            }
        }
        catch (e) {
            errorHandler("An error occurred installing the plugin [$name${version ? '-' + version : ''}]: ${e.message}")
        }
    }

    private void runPluginScript(File scriptFile, fullPluginName, msg) {
        if (pluginScriptRunner == null) {
            return
        }

        if (pluginScriptRunner.maximumNumberOfParameters < 3) {
            throw new IllegalStateException("The [pluginScriptRunner] closure property must accept at least 3 arguments")
        }

        pluginScriptRunner.call(scriptFile, fullPluginName, msg)
    }

    /**
     * Check to see if the plugin directory is in plugins home.
     */
    boolean isNotInlinePluginLocation(File pluginDir) {
        // insure all the directory is in the pluginsHome
        pluginDir.absolutePath.startsWith(applicationPluginsLocation.absolutePath)
    }

    protected postUninstall() {
        // Update the cached dependencies in grailsSettings, and add new jars to the root loader
        pluginSettings.clearCache()
        postUninstallEvent?.call()
    }

    protected postInstall(String pluginInstallPath) {
        // Update the cached dependencies in grailsSettings, and add new jars to the root loader
        pluginSettings.clearCache()
        postInstallEvent?.call(pluginInstallPath)
    }

    private boolean isCorePlugin(name) {
        CORE_PLUGINS.contains(name)
    }

    private boolean confirmInput(String msg) {
        GrailsConsole.getInstance().userInput(msg, ['y','n'] as String[]) == 'y'
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

    /**
     * Checks whether plugin is inline.
     * @todo most probably it is required to search for plugin not just by name but also using its goupdId
     * @param name The plugin name
     * @return true iff plugin is inline one
     */
    private boolean isInlinePlugin(String name) {
        GrailsPluginInfo info = pluginSettings.getPluginInfoForName(name)
        return (info != null) && pluginSettings.getInlinePluginDirectories().find {it == info.getPluginDir()}
    }
}
