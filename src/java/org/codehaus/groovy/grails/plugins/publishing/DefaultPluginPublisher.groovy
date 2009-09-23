/*
 * Copyright 2008 the original author or authors.
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

package org.codehaus.groovy.grails.plugins.publishing

import groovy.util.slurpersupport.GPathResult
import groovy.xml.MarkupBuilder
import org.springframework.core.io.Resource
import grails.util.BuildSettingsHolder
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.OutputKeys

/**
 * Utility methods for manipulating the plugin-list.xml file used
 * when publishing plugins to a Grails plugin repository
 *
 * @author Graeme Rocher
 * @since 1.2
 */

public class DefaultPluginPublisher {

    String revision = "0"
    String repositoryURL
    DefaultPluginPublisher(String revNumber, String repositoryURL) {
        if(revNumber)
            this.revision = revNumber
        if(!repositoryURL) throw new IllegalArgumentException("Argument [repositoryURL] must be specified!")
        this.repositoryURL = repositoryURL
    }

    /**
     * Writes the given plugin list to the given writer
     */
    void writePluginList(GPathResult pluginList, Writer targetWriter) {
        def stringWriter = new StringWriter()
        stringWriter << new groovy.xml.StreamingMarkupBuilder().bind {
              mkp.yield pluginList
        }
        Source xmlInput = new StreamSource(new StringReader(stringWriter.toString()));

        StreamResult xmlOutput = new StreamResult(targetWriter);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(4));
        transformer.transform(xmlInput, xmlOutput)
    }

    /**
     * Publishes a plugin release to the given plugin list
     *
     * @param pluginName the name of the plugin
     * @param pluginsListFile The plugin list file
     * @param makeLatest Whether to make the release the latest release
     *
     * @return the updated plugin list
     */
    GPathResult publishRelease(String pluginName, Resource pluginsList, boolean makeLatest) {
        def xml = parsePluginList(pluginsList)

        xml.@revision = revision
        
        def releaseMetadata = getPluginMetadata(pluginName)
        def pluginVersion = releaseMetadata.@version.toString().trim()
        def releaseTag = "RELEASE_${pluginVersion.replaceAll('\\.','_')}"

        def props = ['title', 'author', 'authorEmail', 'description', 'documentation']
        def releaseInfo = {
            release([tag:releaseTag,version:pluginVersion, type:'svn']) {
                for(p in props) {
                    "$p"(releaseMetadata."$p".text())
                }
                file "$repositoryURL/grails-$pluginName/tags/$releaseTag/grails-$pluginName-${pluginVersion}.zip"
            }
        }

        // build argument, if makeLatest is true make the plugin the latest release
        def pluginArgs = [name:pluginName]
        if(makeLatest) {
            pluginArgs.'latest-release' = pluginVersion
        }

        def pluginInfo = {
            plugin(pluginArgs, releaseInfo)
        }


        // find plugin
        def allPlugins = xml.plugin
        if(allPlugins.size()==0) {
            // create new plugin list
            xml << pluginInfo
        }
        else {
            def existingEntry = xml.plugin.find { it.@name == pluginName }

            if(existingEntry.size()==0) {
                // plugin doesn't exist, create new entry
                def lastPlugin = allPlugins[allPlugins.size()-1]
                lastPlugin + pluginInfo
            }
            else {
                // plugin exists, add release info and make latest is appropriate
                if(makeLatest) {
                    existingEntry.'@latest-release' = pluginVersion
                }
                def existingRelease = existingEntry.release.find { it.@version == pluginVersion }
                if(existingRelease.size()==0) {                    
                    existingEntry << releaseInfo
                }
            }
        }

        return xml

    }

    protected GPathResult parsePluginList(Resource pluginsListFile) {
        if(pluginsListFile.exists()) {
            InputStream stream = pluginsListFile.getInputStream()
            try {
                return new XmlSlurper().parse(stream)
            }
            finally {
                stream?.close()
            }
        }
        else {
            return new XmlSlurper().parseText('<?xml version="1.0" encoding="UTF-8"?><plugins revision="0" />')
        }
    }

    GPathResult publishRelease(String pluginName, Resource pluginsList) {
         publishRelease(pluginName, pluginsList, true)
    }

    protected GPathResult getPluginMetadata(String pluginName) {
        def basedir = BuildSettingsHolder.settings?.baseDir ?: new File(".")
        return new XmlSlurper().parse(new File("${basedir.absolutePath}/plugin.xml"))
    }
}
