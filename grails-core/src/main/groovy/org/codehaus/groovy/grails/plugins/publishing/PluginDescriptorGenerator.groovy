/*
 * Copyright 2011 SpringSource
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

import grails.util.BuildSettings
import grails.util.GrailsUtil
import groovy.xml.MarkupBuilder
import org.codehaus.groovy.grails.io.support.Resource
import org.springframework.util.AntPathMatcher

/**
 * Generates the plugin.xml descriptor.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class PluginDescriptorGenerator {

    public static final String ARTEFACT_PATTERN = /\S+?\/grails-app\/\S+?\/(\S+?)\.groovy/

    String pluginName
    Resource[] resourceList
    List excludes = ["UrlMappings", "DataSource", "BuildConfig", "Config"]
    BuildSettings buildSettings
    AntPathMatcher antPathMatcher = new AntPathMatcher()

    PluginDescriptorGenerator(BuildSettings buildSettings, pluginName, List<Resource> resourceList) {
        this.buildSettings = buildSettings
        this.pluginName = pluginName
        this.resourceList = resourceList.toArray()
    }

    PluginDescriptorGenerator(BuildSettings buildSettings, String pluginName, Resource[] resourceList) {
        this.buildSettings = buildSettings
        this.pluginName = pluginName
        this.resourceList = resourceList
    }

    /**
     * Generates the plugin.xml descriptor to the given target writer
     *
     * @param pluginProps The plugin properties object. Typically a map of properties
     * @param target The target writer
     */
    void generatePluginXml(pluginProps, Writer target) {
       // Use MarkupBuilder with indenting to generate the file.
        def targetWriter = new IndentPrinter(new PrintWriter(target))
        def xml = new MarkupBuilder(targetWriter)
        generatePluginXml(pluginProps, xml)
    }

    /**
     * Generates the plugin.xml descriptor to the given target writer
     *
     * @param pluginProps The plugin properties object. Typically a map of properties
     * @param target The target writer
     */
    void generatePluginXml(pluginProps, IndentPrinter target) {
       // Use MarkupBuilder with indenting to generate the file.
        def xml = new MarkupBuilder(target)
        generatePluginXml(pluginProps, xml)
    }

    private boolean matchesPluginExcludes(List<String> pluginExcludes, File commonResourceBase, Resource r) {

        // if we have no excludes or no common resource base, we don't match
        if (!pluginExcludes) return false
        if (!commonResourceBase) return false

        if (r.file.absolutePath.indexOf(commonResourceBase.absolutePath) == 0) {
            String path = r.file.absolutePath.substring(commonResourceBase.absolutePath.length()+1).tr(File.separator, "/")
            for(String pattern : pluginExcludes) {
                if (antPathMatcher.match(pattern.tr(File.separator, "/"), path)) return true
            }

        }

        return false
    }

    // this is needed to pass the test as the resources don't really exist
    private File filterPluginDir(File pluginDir) {
        if (!pluginDir) return null

        if (pluginDir.absolutePath.endsWith(File.separator + ".")) {
            return new File(pluginDir.absolutePath.substring(0, pluginDir.absolutePath.lastIndexOf(File.separator)))
        } else {
            return pluginDir
        }
    }

    protected void generatePluginXml(pluginProps, MarkupBuilder xml) {
        // Write the content!
        def props = ['author', 'authorEmail', 'title', 'description', 'documentation', 'type', 'packaging']

        def rcComparator = [compare: {a, b -> a.URI.compareTo(b.URI) }] as Comparator
        Arrays.sort(resourceList, rcComparator)

        def pluginGrailsVersion = "${GrailsUtil.grailsVersion} > *"

        // check to see if we have the property, grab it if so
        def pluginExcludes
        if (pluginProps['pluginExcludes'])
            pluginExcludes = pluginProps.pluginExcludes
        else
            pluginExcludes = []

        if (pluginProps != null) {
            if (pluginProps["grailsVersion"]) {
                pluginGrailsVersion = pluginProps["grailsVersion"]
            }

            xml.plugin(name: "${pluginName}", version: "${pluginProps.version}", grailsVersion: pluginGrailsVersion) {
                for (p in props) {
                    if (pluginProps[p]) "${p}"(pluginProps[p])
                }
                xml.resources {
                    final pluginDir = pluginProps['pluginDir'] instanceof String ? new File(pluginProps['pluginDir']) : pluginProps['pluginDir']?.file
                    File commonResourceBase = filterPluginDir(pluginDir)

                    for (r in resourceList) {
                        def matcher = r.URL.toString() =~ ARTEFACT_PATTERN
                        def name = matcher[0][1].replaceAll('/', /\./)
                        if (!excludes.contains(name) && !matchesPluginExcludes(pluginExcludes, commonResourceBase, r)) {
                            xml.resource(name)
                        }
                    }
                }
            }
        }
    }
}
