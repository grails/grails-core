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
import grails.util.PluginBuildSettings

import groovy.util.slurpersupport.GPathResult

import org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin

import org.springframework.core.io.Resource

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
     */
    static String getPluginName(Class clazz) {
        clazz?.getAnnotation(GrailsPlugin)?.name()
    }

    /**
     * Get the version of the a plugin for a particular class.
     */
    static String getPluginVersion(Class clazz) {
        clazz?.getAnnotation(GrailsPlugin)?.version()
    }

    /**
     * Check if the required version is a valid for the given plugin version.
     *
     * @param pluginVersion The plugin version
     * @param requiredVersion The required version
     * @return true if it is valid
     */
    static boolean isValidVersion(String pluginVersion, String requiredVersion) {
        def vc = new VersionComparator()
        pluginVersion = trimTag(pluginVersion)

        if (requiredVersion.indexOf('>') >- 1) {
            def tokens = requiredVersion.split(">")*.trim()
            tokens = tokens.collect { trimTag(it) }
            tokens << pluginVersion
            tokens = tokens.sort(vc)

            if (tokens[1] == pluginVersion) {
                return true
            }
        }
        else if (pluginVersion.equals(trimTag(requiredVersion))) {
            return true
        }

        return false
    }

    /**
     * Returns true if rightVersion is greater than leftVersion
     * @param leftVersion 
     * @param rightVersion
     * @return
     */
    static boolean isVersionGreaterThan(String leftVersion, String rightVersion) {
        if(leftVersion == rightVersion) return false
        def versions = [leftVersion, rightVersion]
        versions = versions.sort(new VersionComparator())
        return versions[1] == rightVersion
    }
    /**
     * Returns the upper version of a Grails version number expression in a plugin
     */
    static String getUpperVersion(String pluginVersion) {
        return getPluginVersionInternal(pluginVersion, 1)
    }

    /**
     * Returns the lower version of a Grails version number expression in a plugin
     */
    static String getLowerVersion(String pluginVersion) {
        return getPluginVersionInternal(pluginVersion, 0)
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

        return pluginVersion.trim()
    }

    private static trimTag(String pluginVersion) {
        def i = pluginVersion.indexOf('-')
        if (i >- 1) {
            pluginVersion = pluginVersion[0..i-1]
        }
        def tokens = pluginVersion.split(/\./)

        return tokens.findAll { it ==~ /\d+/ || it =='*'}.join(".")
    }
}

class VersionComparator implements Comparator {
    
    static private final SNAPSHOT_SUFFIXES = ["-SNAPSHOT", ".BUILD-SNAPSHOT"].asImmutable()
    
    int compare(o1, o2) {
        int result = 0
        if (o1 == '*') {
            result = 1
        }
        else if (o2 == '*') {
            result = -1
        }
        else {
            def nums1
            try {
                def tokens = deSnapshot(o1).split(/\./)
                tokens = tokens.findAll { it.trim() ==~ /\d+/ }
                nums1 = tokens*.toInteger()
            }
            catch (NumberFormatException e) {
                throw new InvalidVersionException("Cannot compare versions, left side [$o1] is invalid: ${e.message}")
            }
            def nums2
            try {
                def tokens = deSnapshot(o2).split(/\./)
                tokens = tokens.findAll { it.trim() ==~ /\d+/ }
                nums2 = tokens*.toInteger()
            }
            catch (NumberFormatException e) {
                throw new InvalidVersionException("Cannot compare versions, right side [$o2] is invalid: ${e.message}")
            }
            boolean bigRight = nums2.size() > nums1.size()
            boolean bigLeft = nums1.size() > nums2.size()
            for (i in 0..<nums1.size()) {
                if (nums2.size() > i) {
                    result = nums1[i].compareTo(nums2[i])
                    if (result != 0) {
                        break
                    }
                    if(i == (nums1.size()-1) && bigRight) {
                       if(nums2[i+1] != 0)
                           result = -1; break
                    }
                }
                else if(bigLeft){
                   if(nums1[i] != 0)
                        result = 1; break
                }
            }
        }
        
        if (result == 0) {
            // Versions are equal, but one may be a snapshot.
            // A snapshot version is considered less than a non snapshot version
            def o1IsSnapshot = isSnapshot(o1)
            def o2IsSnapshot = isSnapshot(o2)

            if (o1IsSnapshot && !o2IsSnapshot) {
                result = -1
            } else if (!o1IsSnapshot && o2IsSnapshot) {
                result = 1
            }
        }

        result
    }

    boolean equals(obj) { false }

    /**
     * Removes any suffixes that indicate that the version is a kind of snapshot
     */
    protected deSnapshot(String version) {
        def suffix = SNAPSHOT_SUFFIXES.find { version.endsWith(it) }
        if (suffix) {
            version[0..-(suffix.size() + 1)]
        } else {
            version
        }
    }
    
    protected isSnapshot(String version) {
        SNAPSHOT_SUFFIXES.any { version.endsWith(it) }
    }
}
