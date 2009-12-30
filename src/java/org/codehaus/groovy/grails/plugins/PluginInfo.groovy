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

import org.springframework.core.io.Resource
import groovy.util.slurpersupport.GPathResult


/**
 * A class used mainly by the build system that encapsulates access to information
 * about the underlying plugin by delegating to the methods in GrailsPluginUtils
 * 
 * @author Graeme Rocher
 * @since 1.1
 */

public class PluginInfo {

    Resource pluginDir
    grails.util.PluginBuildSettings pluginBuildSettings
    def metadata
    String name
    String version

    public PluginInfo(Resource pluginDir, grails.util.PluginBuildSettings pluginBuildSettings) {
        super();
        if(pluginDir)
        this.pluginDir = pluginDir
        this.metadata = parseMetadata(pluginDir)
        this.pluginBuildSettings = pluginBuildSettings
    }

    GPathResult parseMetadata(Resource pluginDir) {
        return new XmlSlurper().parse(new File("$pluginDir.file.absolutePath/plugin.xml"))
    }


    /**
     * Returns the plugin's version
     */
    String getVersion() {
        if(!version) {
            version = metadata.@version.text()
        }
        return version
    }

    /**
     * Returns the plugin's name
     */
    String getName() {
        if(!name) {
            name = metadata.@name.text()
        }
        return name
    }

    /**
     * Obtains the plugins directory
     */
    Resource getPluginDirectory() {
        return pluginDir
    }

    /**
     * Returns the location of the descriptor
     */
    Resource getDescriptor() {
        GrailsPluginUtils.getDescriptorForPlugin(pluginDir)
    }

    String getFullName() {
		"${name}-${version}"
	}
}