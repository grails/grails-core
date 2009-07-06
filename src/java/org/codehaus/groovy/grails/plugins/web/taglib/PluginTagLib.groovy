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
 * WITHOUT c;pWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.web.taglib

import org.codehaus.groovy.grails.plugins.GrailsPluginManager

/**
 *
 * A tag library that provides tags to inspect available plugins
 *
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Feb 6, 2009
 */

public class PluginTagLib {

    static namespace = "plugin"

    GrailsPluginManager pluginManager

    /**
     * Checks whether a particular plugin exists and executes the body if it does
     *
     * eg. <plugin:isAvailable name="hibernate">print me</plugin:isAvailable>
     */
    def isAvailable = { attrs, body ->
        def name = attrs.name
        def version = attrs.version

        if(checkPluginExists(version, name)) {
            out << body()
        }
    }

    /**
     * Checks whether a particular plugin does not exist and executes the body if it does
     *
     * eg. <plugin:isNotAvailable name="hibernate">print me</plugin:isNotAvailable>
     */

    def isNotAvailable = { attrs, body ->
        def name = attrs.name
        def version = attrs.version

        if(!checkPluginExists(version, name)) {
            out << body()
        }
    }

    private boolean checkPluginExists(version, name) {
        if (name) {
            if (version && pluginManager.getGrailsPlugin(name, version)) {
                return true
            }
            else if (pluginManager.hasGrailsPlugin(name)) {
                return true
            }
        }
        return false
    }
}