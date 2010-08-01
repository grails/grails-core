/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.commons;

import groovy.util.ConfigObject;

import java.util.Collections;
import java.util.Map;

/**
 * Holds a reference to the ConfigObject instance.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
public class ConfigurationHolder {

    private static ConfigObject config;
    @SuppressWarnings("rawtypes")
    private static Map flatConfig;

    /**
     * Sets the ConfigObject. Synchronized to avoid the flatten() method being called concurrently.
     * @param newConfig
     */
    public static synchronized void setConfig(ConfigObject newConfig) {
        config = newConfig;
        // reset flat config
        if (newConfig != null) {
            flatConfig = newConfig.flatten();
        }
    }

    /**
     * Retrieve the ConfigObject. Note unsynchronized access is granted for performance reasons,
     * however typically the ConfigObject is only set on application load or by the plugin scanner
     * during development, so this is not an issue.
     *
     * @return The ConfigObject
     */
    public static ConfigObject getConfig() {
        return config;
    }

    /**
     * Returns the ConfigObject has a flattened map for easy access from Java in a properties file like way.
     *
     * @return The flattened ConfigObject
     */
    @SuppressWarnings("rawtypes")
    public static Map getFlatConfig() {
        return flatConfig != null ? flatConfig : Collections.EMPTY_MAP;
    }
}
