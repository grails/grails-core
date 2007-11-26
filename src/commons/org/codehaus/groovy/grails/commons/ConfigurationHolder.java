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
 * A class that holds a reference to the grails.config.ConfigObject instance
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 *        <p/>
 *        Created: Jun 21, 2007
 *        Time: 3:52:04 PM
 */
public class ConfigurationHolder {

    private static ConfigObject config;
    private static Map flatConfig;

    public static synchronized ConfigObject getConfig() {
        return config;
    }

    public static synchronized void setConfig(ConfigObject newConfig) {
        config = newConfig;
        // reset flat config
        flatConfig = null;
    }

    /**
     * Returns the ConfigObject has a flattened map for easy access from Java in a properties file like way
     *
     * @return The flattened ConfigObject
     */
    public static Map getFlatConfig() {
        if(flatConfig == null) {
            ConfigObject config = getConfig();
            if(config!=null)
                flatConfig = config.flatten();
            else
                flatConfig = Collections.EMPTY_MAP;
        }
        return flatConfig;
    }
}
