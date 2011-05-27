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

import grails.util.GrailsUtil
import groovy.xml.MarkupBuilder
import org.codehaus.groovy.grails.documentation.DocumentationContext
import org.codehaus.groovy.grails.documentation.DocumentedMethod
import org.codehaus.groovy.grails.documentation.DocumentedProperty
import org.springframework.core.io.Resource

/**
 * Generates the plugin.xml descriptor
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class PluginDescriptorGenerator {

    public static final String ARTEFACT_PATTERN = /\S+?\/grails-app\/\S+?\/(\S+?)\.groovy/

    String pluginName
    Resource[] resourceList
    List excludes = ["UrlMappings", "DataSource", "BuildConfig", "Config"]

    PluginDescriptorGenerator(String pluginName, List<Resource> resourceList) {
        this.pluginName = pluginName
        this.resourceList = resourceList.toArray()
    }

    PluginDescriptorGenerator(String pluginName, Resource[] resourceList) {
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

    protected void generatePluginXml(pluginProps, MarkupBuilder xml) {
        // Write the content!
        def props = ['author', 'authorEmail', 'title', 'description', 'documentation', 'type', 'packaging']

        def rcComparator = [compare: {a, b -> a.URI.compareTo(b.URI) }] as Comparator
        Arrays.sort(resourceList, rcComparator)

        def pluginGrailsVersion = "${GrailsUtil.grailsVersion} > *"

        if (pluginProps != null) {
            if (pluginProps["grailsVersion"]) {
                pluginGrailsVersion = pluginProps["grailsVersion"]
            }

            xml.plugin(name: "${pluginName}", version: "${pluginProps.version}", grailsVersion: pluginGrailsVersion) {
                for (p in props) {
                    if (pluginProps[p]) "${p}"(pluginProps[p])
                }
                xml.resources {
                    for (r in resourceList) {
                        def matcher = r.URL.toString() =~ ARTEFACT_PATTERN
                        def name = matcher[0][1].replaceAll('/', /\./)
                        if (!excludes.contains(name)) {
                            xml.resource(name)
                        }
                    }
                }
                dependencies {
                    if (pluginProps["dependsOn"]) {
                        for (d in pluginProps.dependsOn) {
                            delegate.plugin(name: d.key, version: d.value)
                        }
                    }
                }

                def docContext = DocumentationContext.instance
                if (docContext) {
                    behavior {
                        for (DocumentedMethod m in docContext.methods) {
                            method(name: m.name, artefact: m.artefact, type: m.type.name) {
                                description m.text
                                if (m.arguments) {
                                    for (arg in m.arguments) {
                                        argument type: arg.name
                                    }
                                }
                            }
                        }
                        for (DocumentedMethod m in docContext.staticMethods) {
                            'static-method'(name: m.name, artefact: m.artefact, type: m.type.name) {
                                description m.text
                                if (m.arguments) {
                                    for (arg in m.arguments) {
                                        argument type: arg.name
                                    }
                                }
                            }
                        }
                        for (DocumentedProperty p in docContext.properties) {
                            property(name: p.name, type: p.type.name, artefact: p.artefact) {
                                description p.text
                            }
                        }
                    }
                }
            }
        }
    }
}