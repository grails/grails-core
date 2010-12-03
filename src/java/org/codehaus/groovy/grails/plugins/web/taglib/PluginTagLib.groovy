/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.web.taglib

import org.codehaus.groovy.grails.plugins.GrailsPluginManager

/**
 * Tags to inspect available plugins.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class PluginTagLib {

    static namespace = "plugin"

    GrailsPluginManager pluginManager

    /**
     * Gets the path to a particular plugin.
     *
     * eg. &lt;plugin:path name="myPlugin" /&gt;
     *
     * @attr name REQUIRED the plugin name
     */
    def path = { attrs, body ->
        out << pluginManager.getPluginPath(attrs.name)
    }

    /**
     * Checks whether a particular plugin exists and executes the body if it does
     *
     * eg. &lt;plugin:isAvailable name="hibernate"&gt;print me&lt;/plugin:isAvailable&gt;
     *
     * @attr name REQUIRED the plugin name
     * @attr version REQUIRED the plugin version
     */
    def isAvailable = { attrs, body ->
        if (checkPluginExists(attrs.version, attrs.name)) {
            out << body()
        }
    }

    /**
     * Checks whether a particular plugin does not exist and executes the body if it does
     *
     * eg. &lt;plugin:isNotAvailable name="hibernate"&gt;print me&lt;/plugin:isNotAvailable&gt;
     *
     * @attr name REQUIRED the plugin name
     * @attr version REQUIRED the plugin version
     */
    def isNotAvailable = { attrs, body ->
        if (!checkPluginExists(attrs.version, attrs.name)) {
            out << body()
        }
    }

    private boolean checkPluginExists(version, name) {
        if (name) {
            if (version && pluginManager.getGrailsPlugin(name, version)) {
                return true
            }
            if (pluginManager.hasGrailsPlugin(name)) {
                return true
            }
        }
        return false
    }
}
