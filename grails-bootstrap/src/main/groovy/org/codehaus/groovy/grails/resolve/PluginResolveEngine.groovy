/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.resolve

import grails.util.BuildSettings
import groovy.util.slurpersupport.GPathResult
import org.apache.ivy.core.cache.ArtifactOrigin
import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.plugins.repository.Repository
import org.apache.ivy.plugins.repository.Resource
import org.apache.ivy.plugins.resolver.DependencyResolver
import org.apache.ivy.plugins.resolver.RepositoryResolver
import grails.build.logging.GrailsConsole

/**
 * Utility methods for resolving plugin zips and information
 * used in conjunction with an IvyDependencyManager instance.
 *
 * @author Graeme Rocher
 * @since 1.3
 */
final class PluginResolveEngine {

    IvyDependencyManager dependencyManager
    BuildSettings settings
    Closure messageReporter = { if(it) GrailsConsole.instance.updateStatus(it)  }

    PluginResolveEngine(IvyDependencyManager dependencyManager, BuildSettings settings) {
        this.dependencyManager = dependencyManager
        this.settings = settings
    }

    IvyDependencyManager createFreshDependencyManager() {
        IvyDependencyManager dm = new IvyDependencyManager(dependencyManager.applicationName,
            dependencyManager.applicationVersion ?: "0.1", settings)
        dm.chainResolver = dependencyManager.chainResolver
        if (dependencyManager.logger) {
            dm.logger = dependencyManager.logger
        }
        return dm
    }

    /**
     * Resolves a list of plugins and produces a ResolveReport
     *
     * @param pluginsToInstall The list of plugins
     * @param scope The scope (defaults to runtime)
     */
    ResolveReport resolvePlugins(Collection<EnhancedDefaultDependencyDescriptor> pluginsToInstall, String scope = '') {
        IvyDependencyManager newManager = createFreshDependencyManager()
        pluginsToInstall.each { newManager.registerPluginDependency("runtime", it) }
        return newManager.resolvePluginDependencies(scope)
    }

    /**
     * Resolve a Plugin zip for for the given name and plugin version
     * @param pluginName The plugin name
     * @param pluginVersion The plugin version
     * @return The location of the local file or null if an error occured
     */
    File resolvePluginZip(String pluginName, String pluginVersion, String scope = "", Map args = [:]) {
        IvyDependencyManager dependencyManager = createFreshDependencyManager()
        def resolveArgs = createResolveArguments(pluginName, pluginVersion)

        dependencyManager.parseDependencies {
            plugins {
                runtime resolveArgs
            }
        }

        messageReporter "Resolving plugin ${pluginName}. Please wait..."
        messageReporter()
        def report = dependencyManager.resolvePluginDependencies(scope,args)
        if (report.hasError()) {
            messageReporter "Error resolving plugin ${resolveArgs}."
            return null
        }

        def reports = report.getArtifactsReports(null, false)
        def artifactReport = reports.find { it.artifact.attributes.organisation == resolveArgs.group && it.artifact.name == resolveArgs.name && (pluginVersion == null || it.artifact.moduleRevisionId.revision == pluginVersion) }
        if (artifactReport == null) {
            artifactReport = reports.find { it.artifact.name == pluginName && (pluginVersion == null || it.artifact.moduleRevisionId.revision == pluginVersion) }
        }
        if (artifactReport) {
            return artifactReport.localFile
        }

        messageReporter "Error resolving plugin ${resolveArgs}. Plugin not found."
        return null
    }

    def createResolveArguments(String pluginName, String pluginVersion) {
        def (group, name) = pluginName.contains(":") ? pluginName.split(":") : ['org.grails.plugins', pluginName]
        def resolveArgs = [name: name, group: group]
        if (pluginVersion) resolveArgs.version = pluginVersion
        else resolveArgs.version = "latest.integration"
        return resolveArgs
    }

    /**
     * This method will resolve the plugin.xml file for a given plugin
     * without downloading the plugin zip itself
     */
    GPathResult resolvePluginMetadata(String pluginName, String pluginVersion) {
        IvyDependencyManager dependencyManager = createFreshDependencyManager()

        def resolveArgs = createResolveArguments(pluginName, pluginVersion)

        // first try resolve via plugin.xml that resides next to zip
        dependencyManager.parseDependencies {
            plugins {
                runtime resolveArgs
            }
        }
        def report = dependencyManager.resolvePluginDependencies('runtime', [download:false])
        if (!report.hasError() && report.getArtifactsReports(null, false)) {
            ArtifactOrigin origin = report.getArtifactsReports(null, false).origin.first()
            def location = origin.location
            def parent = location[0..location.lastIndexOf('/')-1]
            for (DependencyResolver dr in dependencyManager.chainResolver.resolvers) {
                if (dr instanceof RepositoryResolver) {
                    Repository r = dr.repository

                    def pluginFile = "$parent/plugin.xml"
                    try {
                        Resource res = r.getResource(pluginFile)
                        def input
                        try {
                            input = res.openStream()
                            return new XmlSlurper().parse(input)
                        }
                        finally {
                            input.close()
                        }
                    }
                    catch(e) {
                        // ignore
                    }
                }
            }
        }

        // if the plugin.xml was never found, try via maven-style attachments using a classifier
        if (!report.hasError()) {
            resolveArgs.classifier = "plugin"
            dependencyManager = createFreshDependencyManager()

            dependencyManager.parseDependencies {
                plugins {
                    runtime resolveArgs
                }
            }

            report = dependencyManager.resolvePluginDependencies()

            if (report.hasError() || !report.getArtifactsReports(null, false)) {
                return null
            }

            return new XmlSlurper().parse(report.getArtifactsReports(null, false).localFile.first())
        }
    }
}
