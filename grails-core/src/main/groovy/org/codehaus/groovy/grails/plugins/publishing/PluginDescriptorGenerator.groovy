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
import grails.util.BuildSettings
import org.apache.ivy.core.module.descriptor.DependencyDescriptor
import org.apache.ivy.plugins.resolver.URLResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.codehaus.groovy.grails.resolve.GrailsRepoResolver
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

    /* the pluginExcludes are Ant matched from the base of the application, but since the
       pluginProps is not necessarily a class and we don't actually know where it is, we need
       to go through the resources figuring out what the common resource base is. We are going to assume
       a common Grails application layout to fudge this.
    */
    private String resourceBaseMatchDirs = ['grails-app', 'web-app', 'scripts', 'test', 'src']
    private File findCommonResourceBase() {
        if (!resourceList) return null // no resources, won't loop

        for (Resource r in resourceList) {
            File f = r.file

            while (f != null && !resourceBaseMatchDirs.contains(f.name)) {
                f = f.parentFile
            }

            if (f) {
                  if (f.parentFile == null) { // wonderful, thanks Resource
                    return new File(f.absolutePath.substring(0, f.absolutePath.lastIndexOf(File.separator)))
                } else {
                    return f.parentFile
                }
            }
        }

        GrailsUtil.warn("Unable to determine common resource base when generating plugin.xml")

        return null
    }

    private boolean matchesPluginExcludes(List<String> pluginExcludes, File commonResourceBase, Resource r) {

        // if we have no excludes or no common resource base, we don't match
        if (!pluginExcludes) return false
        if (!commonResourceBase) return false

        if (r.file.absolutePath.indexOf(commonResourceBase.absolutePath) == 0) {
            String path = r.file.absolutePath.substring(commonResourceBase.absolutePath.length()+1)
            for(String pattern : pluginExcludes) {
                if (antPathMatcher.match(pattern, path)) return true
            }

        }

        return false
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
                    File commonResourceBase = findCommonResourceBase()

                    for (r in resourceList) {
                        def matcher = r.URL.toString() =~ ARTEFACT_PATTERN
                        def name = matcher[0][1].replaceAll('/', /\./)
                        if (!excludes.contains(name) && !matchesPluginExcludes(pluginExcludes, commonResourceBase, r)) {
                            xml.resource(name)
                        }
                    }
                }
                final dependencyManager = buildSettings?.dependencyManager
                if(dependencyManager) {
                    repositories {
                        final resolvers = dependencyManager.chainResolver.resolvers
                        for(r in resolvers) {
                            if(r instanceof IBiblioResolver) {
                                xml.repository(name:r.name, url:r.root )
                            }
                            else if(r instanceof GrailsRepoResolver) {
                                xml.repository(name:r.name, url:r.repositoryRoot.toString() )
                            }
                        }
                    }
                    final scopes = dependencyManager.configurationNames
                    dependencies {
                        for(scope in scopes) {

                            final jarDependencies = dependencyManager.getApplicationDependencyDescriptors(scope)

                            if(jarDependencies) {
                                xml."$scope" {
                                    for(DependencyDescriptor dd in jarDependencies) {
                                        final mrid = dd.dependencyRevisionId
                                        xml.dependency(group:mrid.organisation, name:mrid.name, version:mrid.revision)
                                    }
                                }
                            }

                        }
                    }
                    plugins {
                        for(scope in scopes) {

                            final pluginDependencies = dependencyManager.getApplicationPluginDependencyDescriptors(scope)
                            if(pluginDependencies) {
                                xml."$scope" {
                                    for(DependencyDescriptor dd in pluginDependencies) {
                                        final mrid = dd.dependencyRevisionId
                                        xml.plugin(group:mrid.organisation, name:mrid.name, version:mrid.revision)
                                    }
                                }
                            }

                        }

                    }
                }
                runtimePluginRequirements {
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