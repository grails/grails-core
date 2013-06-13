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
package org.codehaus.groovy.grails.plugins

import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.PluginBuildSettings
import groovy.util.slurpersupport.GPathResult

import org.codehaus.groovy.grails.io.support.Resource

/**
 * Utility class containing methods that aid in loading and evaluating plug-ins.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class GrailsPluginUtils {

    static final String WILDCARD = "*"
    static final String GRAILS_HOME
    static {
        try {
            GRAILS_HOME = System.getenv("GRAILS_HOME")
        }
        catch (Throwable t) {
            // probably due to permissions error
            GRAILS_HOME = "UNKNOWN"
        }
    }

    /**
     * Get the name of the a plugin for a particular class.
     *
     * @deprecated Use {@link GrailsVersionUtils}
     */
    static String getPluginName(Class clazz) {
        GrailsVersionUtils.getPluginName(clazz)
    }

    /**
     * Get the version of the a plugin for a particular class.
     *
     * @deprecated Use {@link GrailsVersionUtils}
     */
    static String getPluginVersion(Class clazz) {
        GrailsVersionUtils.getPluginVersion(clazz)
    }

    /**
     * Check if the required version is a valid for the given plugin version.
     *
     * @param pluginVersion The plugin version
     * @param requiredVersion The required version
     * @return true if it is valid
     *
     * @deprecated Use {@link GrailsVersionUtils}
     */
    static boolean isValidVersion(String pluginVersion, String requiredVersion) {
        GrailsVersionUtils.isValidVersion(pluginVersion,requiredVersion)
    }

    /**
     * Returns true if rightVersion is greater than leftVersion
     * @param leftVersion
     * @param rightVersion
     * @return
     *
     * @deprecated Use {@link GrailsVersionUtils}
     */
    static boolean isVersionGreaterThan(String leftVersion, String rightVersion) {
       GrailsVersionUtils.isVersionGreaterThan(leftVersion, rightVersion)
    }
    /**
     * Returns the upper version of a Grails version number expression in a plugin
     *
     * @deprecated Use {@link GrailsVersionUtils}
     */
    static String getUpperVersion(String pluginVersion) {
        GrailsVersionUtils.getUpperVersion(pluginVersion)
    }

    /**
     * Returns the lower version of a Grails version number expression in a plugin
     *
     * @deprecated Use {@link GrailsVersionUtils}
     */
    static String getLowerVersion(String pluginVersion) {
        GrailsVersionUtils.getLowerVersion(pluginVersion)
    }

    /*
    * @deprecated Use {@link GrailsVersionUtils}
     */
    static boolean supportsAtLeastVersion(String pluginVersion, String requiredVersion) {
        GrailsVersionUtils.supportsAtLeastVersion(pluginVersion, requiredVersion)
    }

   /**
     * Returns a new PluginBuildSettings instance
     */
    static PluginBuildSettings newPluginBuildSettings(BuildSettings buildSettings = null) {
        new PluginBuildSettings(buildSettings ?: BuildSettingsHolder.settings)
    }

    private static PluginBuildSettings INSTANCE
    /**
     * Returns a cached PluginBuildSettings instance.
     */
    static synchronized PluginBuildSettings getPluginBuildSettings(BuildSettings buildSettings = null) {
        if (INSTANCE == null || (buildSettings != null && !buildSettings.is(INSTANCE.buildSettings))) {
            INSTANCE = newPluginBuildSettings(buildSettings)
        }
        return INSTANCE
    }

    static synchronized setPluginBuildSettings(PluginBuildSettings settings) {
        INSTANCE = settings
    }

    /**
     * Returns an array of PluginInfo objects
     */
    static GrailsPluginInfo[] getPluginInfos(String pluginDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        return getPluginBuildSettings().getPluginInfos()
    }

    /**
     * Returns only the PluginInfo objects that support the current Environment and BuildScope
     *
     * @see grails.util.Environment
     * @see grails.util.BuildScope
     */
    static GrailsPluginInfo[] getSupportedPluginInfos(String pluginDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        final PluginBuildSettings settings = getPluginBuildSettings()
        return settings.getSupportedPluginInfos()
    }

    /**
     * All the known plugin base directories (directories where plugins are installed to).
     */
    static List<String> getPluginBaseDirectories(String pluginDirPath) {
        getPluginBuildSettings().getPluginBaseDirectories()
    }

    /**
     * All the known plugin base directories (directories where plugins are installed to).
     */
    static List<String> getPluginBaseDirectories() {
        getPluginBuildSettings().getPluginBaseDirectories()
    }

    static Resource[] getPluginDirectories() {
        getPluginBuildSettings().getPluginDirectories()
    }

    static Resource[] getPluginDirectories(String pluginDirPath) {
        getPluginBuildSettings().getPluginDirectories()
    }

    /**
     * All plugin directories in both the given path and the global "plugins" directory together.
     */
    static List<Resource> getImplicitPluginDirectories(String pluginDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        getPluginBuildSettings().getImplicitPluginDirectories()
    }

    static boolean isGlobalPluginLocation(Resource pluginDir) {
        getPluginBuildSettings().isGlobalPluginLocation(pluginDir)
    }

    /**
     * All artefact resources (all Groovy files contained within the grails-app directory of plugins or applications).
     */
    static Resource[] getArtefactResources(String basedir) {
        getPluginBuildSettings().getArtefactResources()
    }

    /**
     * All artefacts in the given application or plugin directory as Spring resources.
     */
    static Resource[] getArtefactResourcesForOne(String projectDir) {
        getPluginBuildSettings().getArtefactResourcesForOne(projectDir)
    }

    /**
     * The Plugin metadata XML files used to describe the plugins provided resources.
     */
    static Resource[] getPluginXmlMetadata(String pluginsDirPath) {
        getPluginBuildSettings().getPluginXmlMetadata()
    }

    /**
     * All Gant scripts that are availabe for execution in a Grails application.
     */
    static Resource[] getAvailableScripts(String grailsHome, String pluginDirPath, String basedir) {
        getPluginBuildSettings().getAvailableScripts()
    }

    /**
     * Plug-in provided Gant scripts available to a Grails application.
     */
    static Resource[] getPluginScripts(String pluginDirPath) {
        getPluginBuildSettings().getPluginScripts()
    }

    /**
     * All plugin provided resource bundles.
     */
    static Resource[] getPluginResourceBundles(String pluginDirPath) {
        getPluginBuildSettings().getPluginResourceBundles()
    }

    /**
     * All plug-in provided source files (Java and Groovy).
     */
    static Resource[] getPluginSourceFiles(String pluginsDirPath) {
        getPluginBuildSettings().getPluginSourceFiles()
    }

    /**
     * All plug-in provided JAR files.
     */
    static Resource[] getPluginJarFiles(String pluginsDirPath) {
        getPluginBuildSettings().getPluginJarFiles()
    }

    /**
     * All plug-in descriptors (the root classes that end with *GrailsPlugin.groovy).
     */
    static Resource[] getPluginDescriptors(String basedir, String pluginsDirPath) {
        getPluginBuildSettings().getPluginDescriptors()
    }

    static Resource getBasePluginDescriptor(String basedir) {
        getPluginBuildSettings().getBasePluginDescriptor(basedir)
    }

    /**
     * Returns the descriptor location for the given plugin directory. The descriptor is the Groovy
     * file that ends with *GrailsPlugin.groovy.
     */
    static Resource getDescriptorForPlugin(Resource pluginDir) {
        getPluginBuildSettings().getDescriptorForPlugin(pluginDir)
    }

    /**
     * All plug-in lib directories.
     */
    static Resource[] getPluginLibDirectories(String pluginsDirPath) {
        getPluginBuildSettings().getPluginLibDirectories()
    }

    /**
     * All plugin i18n directories.
     */
    static Resource[] getPluginI18nDirectories(String pluginsDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        getPluginBuildSettings().getPluginI18nDirectories()
    }

    /**
     * The path to the global plugins directory.
     */
    static String getGlobalPluginsPath() {
        getPluginBuildSettings().getGlobalPluginsPath()
    }

    /**
     * Obtains a plugin directory for the given name.
     */
    static Resource getPluginDirForName(String pluginName) {
        getPluginBuildSettings().getPluginDirForName(pluginName)
    }

    /**
     * Returns XML about the plugin.
     */
    static GPathResult getMetadataForPlugin(String pluginName) {
        getPluginBuildSettings().getMetadataForPlugin(pluginName)
    }

    /**
     * Returns XML metadata for the plugin.
     */
    static GPathResult getMetadataForPlugin(Resource pluginDir) {
        getPluginBuildSettings().getMetadataForPlugin(pluginDir)
    }

    /**
     * Obtains a plugin directory for the given name.
     */
    static Resource getPluginDirForName(String pluginsDirPath, String pluginName) {
        getPluginBuildSettings().getPluginDirForName(pluginName)
    }

    /**
     * Clears cached resolved resources
     */
    static synchronized clearCaches() {
        getPluginBuildSettings().clearCache()
        INSTANCE = null
    }
}
