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

import grails.util.PluginBuildSettings

import groovy.util.slurpersupport.GPathResult

import org.springframework.core.io.Resource

/**
 * Used mainly by the build system that encapsulates access to information
 * about the underlying plugin by delegating to the methods in GrailsPluginUtils.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class PluginInfo extends GroovyObjectSupport implements GrailsPluginInfo {

    Resource pluginDir
    PluginBuildSettings pluginBuildSettings
    def metadata
    String name
    String version

    PluginInfo(Resource pluginXml, grails.util.PluginBuildSettings pluginBuildSettings) {
        if (pluginXml) {
            try {
                pluginDir = pluginXml.createRelative(".")
            }
            catch(e) {
                // ignore
            }
        }

        metadata = parseMetadata(pluginXml)
        this.pluginBuildSettings = pluginBuildSettings
    }

    GPathResult parseMetadata(Resource pluginXml) {
        InputStream input
        try {
            input = pluginXml.getInputStream()
            return new XmlSlurper().parse(input)
        }
        finally { input?.close() }
    }

    /**
     * Returns the plugin's version.
     */
    String getVersion() {
        if (!version) {
            version = metadata.@version.text()
        }
        return version
    }

    /**
     * Returns the plugin's name.
     */
    String getName() {
        if (!name) {
            name = metadata.@name.text()
        }
        return name
    }

    /**
     * Obtains the plugins directory.
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
        "${getName()}-${getVersion()}"
    }

    Map getProperties() {
        [name:getName(), version:getVersion()]
    }

    def getProperty(String name) {
        try {
            return super.getProperty(name)
        }
        catch (MissingPropertyException mpe) {
            return metadata[name].text()
        }
    }
}
