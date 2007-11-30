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
package org.codehaus.groovy.grails.plugins;

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
    public static boolean isValidVersion(String pluginVersion, String requiredVersion) {

        pluginVersion = trimTag(pluginVersion);

       if(requiredVersion.indexOf('>')>-1) {
            def tokens = requiredVersion.split(">")*.trim()
            def newTokens = []
            for(t in tokens) {
                newTokens << trimTag(t)
            }

            tokens << pluginVersion
            tokens = tokens.sort(COMPARATOR)

            if(tokens[1] == pluginVersion) return true

        }
        else if(pluginVersion.equals(trimTag(requiredVersion))) return true;
        return false;
    }

    private static trimTag(pluginVersion) {
        def i = pluginVersion.indexOf('-')
        if(i>-1)
            pluginVersion = pluginVersion[0..i-1]
        pluginVersion
    }

}
