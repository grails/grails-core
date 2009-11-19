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

import grails.util.BuildScope
import grails.util.BuildSettingsHolder
import grails.util.Environment
import grails.util.PluginBuildSettings
import groovy.util.slurpersupport.GPathResult
import org.codehaus.groovy.grails.plugins.InvalidVersionException
import org.codehaus.groovy.grails.plugins.PluginInfo
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin

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


    /**
     * Get the name of the a plugin for a particular class
     */
    static String getPluginName(Class clazz) {
        if(clazz!=null) {
            GrailsPlugin plugin = clazz.getAnnotation(GrailsPlugin)
            if(plugin!=null) return plugin.name()
        }
    }

    /**
     * Get the version of the a plugin for a particular class
     */
    static String getPluginVersion(Class clazz) {
        if(clazz!=null) {
            GrailsPlugin plugin = clazz.getAnnotation(GrailsPlugin)
            if(plugin!=null) return plugin.version()
        }
    }
    /**
     * Check if the required version is a valid for the given plugin version
     *
     * @param pluginVersion The plugin version
     * @param requiredVersion The required version
     * @return True if it is valid
     */
    static boolean isValidVersion(String pluginVersion, String requiredVersion) {
    	def vc = new VersionComparator()
        pluginVersion = trimTag(pluginVersion);

       if(requiredVersion.indexOf('>')>-1) {
            def tokens = requiredVersion.split(">")*.trim()
            tokens = tokens.collect { trimTag(it) }
            tokens << pluginVersion
            tokens = tokens.sort(vc)

            if(tokens[1] == pluginVersion) return true

        }
        else if(pluginVersion.equals(trimTag(requiredVersion))) return true;
        return false;
    }

    /**
     * Returns the upper version of a Grails version number expression in a plugin
     */
    static String getUpperVersion(String pluginVersion) {
        return getPluginVersionInternal(pluginVersion,1)
    }

   /**
     * Returns the lower version of a Grails version number expression in a plugin
     */
    static String getLowerVersion(String pluginVersion) {
        return getPluginVersionInternal(pluginVersion,0)
    }

    static boolean supportsAtLeastVersion(String pluginVersion, String requiredVersion) {
        def lowerVersion = GrailsPluginUtils.getLowerVersion(pluginVersion)
        lowerVersion != '*' && GrailsPluginUtils.isValidVersion(lowerVersion, "$requiredVersion > *")
    }

    private static getPluginVersionInternal(String pluginVersion, index) {
        if (pluginVersion.indexOf('>') > -1) {
            def tokens = pluginVersion.split(">")*.trim()
            return tokens[index].trim()
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



    private static INSTANCE = null
    static synchronized grails.util.PluginBuildSettings  getPluginBuildSettings() {
        if(!INSTANCE) {
            INSTANCE = new PluginBuildSettings(BuildSettingsHolder.settings, PluginManagerHolder.getPluginManager())
        }
        return INSTANCE
    }

    static synchronized setPluginBuildSettings(PluginBuildSettings settings) {
        INSTANCE = settings
    }

    /**
     * Returns an array of PluginInfo objects
     */
    static PluginInfo[] getPluginInfos(String pluginDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        return getPluginBuildSettings().getPluginInfos()
    }

    /**
     * Returns only the PluginInfo objects that support the current Environment and BuildScope
     *
     * @see grails.util.Environment
     * @see grails.util.BuildScope
     */
    static PluginInfo[] getSupportedPluginInfos(String pluginDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        final PluginBuildSettings settings = getPluginBuildSettings()
        if(!settings.pluginManager) {
            settings.pluginManager = PluginManagerHolder.currentPluginManager()
        }
        return settings.getSupportedPluginInfos()
    }


    /**
     * Gets a list of all the known plugin base directories (directories where plugins are installed to)
     */
    static List<String> getPluginBaseDirectories(String pluginDirPath) {
         getPluginBuildSettings().getPluginBaseDirectories()
    }

    /**
     * Gets a list of all the known plugin base directories (directories where plugins are installed to)
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
     * Returns a list of all plugin directories in both the given path
     * and the global "plugins" directory together.
     */
    static List<Resource> getImplicitPluginDirectories(String pluginDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        getPluginBuildSettings().getImplicitPluginDirectories()
    }

    static boolean isGlobalPluginLocation(Resource pluginDir) {
        getPluginBuildSettings().isGlobalPluginLocation(pluginDir)
    }

    /**
     * Obtains a reference to all artefact resources (all Groovy files contained within the grails-app directory of plugins or applications)
     */
    static Resource[] getArtefactResources(String basedir) {
        getPluginBuildSettings().getArtefactResources()
    }

    /**
     * Returns an array of all artefacts in the given application or
     * plugin directory as Spring resources.
     */
    static Resource[] getArtefactResourcesForOne(String projectDir) {
       getPluginBuildSettings().getArtefactResourcesForOne(projectDir)
    }

    /**
     * Obtains a Resource array of the Plugin metadata XML files used to describe the plugins provided resources
     */
    static Resource[] getPluginXmlMetadata( String pluginsDirPath) {
         getPluginBuildSettings().getPluginXmlMetadata()
    }


    /**
     * Obtains an array of all Gant scripts that are availabe for execution in a Grails application
     */
    static Resource[] getAvailableScripts(String grailsHome,
                                          String pluginDirPath,
                                          String basedir) {
        getPluginBuildSettings().getAvailableScripts()
    }
    /**
     * Obtains an array of plug-in provided Gant scripts available to a Grails application
     */
    static Resource[] getPluginScripts(String pluginDirPath) {
        getPluginBuildSettings().getPluginScripts()
    }


    /**
     * Obtains an array of all plugin provided resource bundles
     */
    static Resource[] getPluginResourceBundles(String pluginDirPath) {
        getPluginBuildSettings().getPluginResourceBundles()
    }

    /**
     * Obtains an array of all plug-in provided source files (Java and Groovy)
     */
    static Resource[] getPluginSourceFiles(String pluginsDirPath) {
        getPluginBuildSettings().getPluginSourceFiles()
    }

    /**
     * Obtains an array of all plug-in provided JAR files
     */
    static Resource[] getPluginJarFiles(String pluginsDirPath) {
        getPluginBuildSettings().getPluginJarFiles()
    }

    /**
     * Obtains an array of all plug-in descriptors (the root classes that end with *GrailsPlugin.groovy)
     */
    static Resource[] getPluginDescriptors(String basedir,
                                                        String pluginsDirPath) {
        getPluginBuildSettings().getPluginDescriptors()
    }

    static Resource getBasePluginDescriptor(String basedir) {
        getPluginBuildSettings().getBasePluginDescriptor(basedir)
    }

    /**
     * Returns the descriptor location for the given plugin directory. The descriptor is the Groovy
     * file that ends with *GrailsPlugin.groovy
     */
    public static Resource getDescriptorForPlugin(Resource pluginDir) {
        getPluginBuildSettings().getDescriptorForPlugin(pluginDir)
    }


    /**
     * Obtains an array of all plug-in lib directories
     */
    static Resource[] getPluginLibDirectories(String pluginsDirPath) {
         getPluginBuildSettings().getPluginLibDirectories()
    }



    /**
     * Obtains an array of all plugin i18n directories
     */
    static Resource[] getPluginI18nDirectories(String pluginsDirPath = BuildSettingsHolder.settings?.projectPluginsDir?.path) {
        getPluginBuildSettings().getPluginI18nDirectories()
    }

    /**
     * Obtains the path to the globa plugins directory
     */
    static String getGlobalPluginsPath() {
        getPluginBuildSettings().getGlobalPluginsPath()
    }


    /**
     * Obtains a plugin directory for the given name
     */
    static Resource getPluginDirForName(String pluginName) {
        getPluginBuildSettings().getPluginDirForName(pluginName)
    }


    /**
     * Returns XML about the plugin 
     */
    static GPathResult getMetadataForPlugin(String pluginName) {
        getPluginBuildSettings().getMetadataForPlugin(pluginName)
    }

    /**
     * Returns XML metadata for the plugin
     */
    static GPathResult getMetadataForPlugin(Resource pluginDir) {
        getPluginBuildSettings().getMetadataForPlugin(pluginDir)
    }

    /**
     * Obtains a plugin directory for the given name
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
class VersionComparator implements Comparator{
	int compare( o1, o2 ) {
        def result = 0
        if(o1 == '*') result = 1
        else if(o2 == '*') result = -1
        else {
            def nums1
            try {
                def tokens = o1.split(/\./)
                tokens = tokens.findAll { it.trim() ==~ /\d+/ }
                nums1 = tokens*.toInteger()
            }
            catch (NumberFormatException  e) {
                throw new InvalidVersionException("Cannot compare versions, left side [$o1] is invalid: ${e.message}")
            }
            def nums2
            try {
                def tokens = o2.split(/\./)
                tokens = tokens.findAll { it.trim() ==~ /\d+/ }
                nums2 = tokens*.toInteger()
            }
            catch (java.lang.NumberFormatException e) {
                throw new InvalidVersionException("Cannot compare versions, right side [$o2] is invalid: ${e.message}")
            }
            for(i in 0..<nums1.size()) {
                if(nums2.size() > i) {
                    result = nums1[i].compareTo(nums2[i])
                    if(result != 0)break
                }
            }
        }
        result
    }

    boolean equals(obj) { false }
}

