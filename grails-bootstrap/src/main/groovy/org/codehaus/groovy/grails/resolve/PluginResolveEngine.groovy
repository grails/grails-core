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

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import groovy.util.slurpersupport.GPathResult

import org.apache.ivy.core.cache.ArtifactOrigin
import org.apache.ivy.plugins.repository.Repository
import org.apache.ivy.plugins.repository.Resource
import org.apache.ivy.plugins.resolver.DependencyResolver
import org.apache.ivy.plugins.resolver.RepositoryResolver

/**
 * Utility methods for resolving plugin zips and information
 * used in conjunction with an IvyDependencyManager instance.
 *
 * @author Graeme Rocher
 * @since 1.3
 */
final class PluginResolveEngine {

    DependencyManager dependencyManager
    BuildSettings settings
    Closure messageReporter = { if (it) GrailsConsole.instance.updateStatus(it)  }

    PluginResolveEngine(DependencyManager dependencyManager, BuildSettings settings) {
        this.dependencyManager = dependencyManager
        this.settings = settings
    }

    DependencyManager createFreshDependencyManager() {
        dependencyManager.createCopy(settings)
    }

    void renderInstallInfo(String pluginName, String version, Writer writer) {

        writer << """
Since Grails 2.3, it is no longer possible to install plugins using the install-plugin command.
Plugins must be declared in the grails-app/conf/BuildConfig.groovy file.
"""

        def pluginXml
        if (dependencyManager instanceof IvyDependencyManager) {
            pluginXml = resolvePluginMetadata(pluginName, version)
            if (!version) {
                version = pluginXml.@version.text()
            }
        }
        else {
            String url = "http://grails.org/api/v1.0/plugin/$pluginName"
            if (version) {
                url += "/$version"
            }
            try {
                final String text = new URL("$url?format=xml").getText(connectTimeout: 500, readTimeout: 3000)
                pluginXml = new XmlSlurper().parseText(text)
                if (!version) {
                    version = pluginXml.version.text()
                }
            }
            catch (FileNotFoundException ignored) {}
        }

        if (!pluginXml) {
            if (version) {
                writer << """
ERROR: Plugin '$pluginName' not found with version '$pluginVersion'
"""
            }
            else {
                writer << """
ERROR: Plugin '$pluginName' not found
"""
            }
            return
        }

        writer << """
Example:
grails.project.dependency.resolution = {
   ...
   plugins {
      compile ":$pluginName:$version"
   }
}
"""

        def repos = []
        def ignored = [
            'http://plugins.grails.org',
            'http://repo.grails.org/grails/plugins/',
            'http://repo.grails.org/grails/core/',
            'http://svn.codehaus.org/grails/trunk/grails-plugins']

        if (pluginXml.repositories.children().size()) {
            for (repo in pluginXml.repositories.repository) {
                String url = repo.@url.text().trim()
                if (url in ignored) {
                    continue
                }
                if (url == 'http://repo1.maven.org/maven2/') {
                    url = 'mavenCentral()'
                }
                repos << url
            }
        }

        if (repos) {
            writer << """
Additionally, add these custom repositories to the "repositories" block:
"""
            for (repo in repos) {
                writer << """\
   $repo
"""
            }
        }
    }

    /**
     * Renders plugin info to the target writer
     *
     * @param pluginName The plugin name
     * @param pluginVersion The plugin version
     * @param output The target writer
     */
    GPathResult renderPluginInfo(String pluginName, String pluginVersion, OutputStream outputStream) {
        renderPluginInfo(pluginName, pluginVersion, new OutputStreamWriter(outputStream))
    }

    /**
     * Renders plugin info to the target writer
     *
     * @param pluginName The plugin name
     * @param pluginVersion The plugin version
     * @param output The target writer
     */
    GPathResult renderPluginInfo(String pluginName, String pluginVersion, Writer writer) {
        def pluginXml = resolvePluginMetadata(pluginName, pluginVersion)

        if (pluginXml != null) {
            def output = new PrintWriter(writer)
            def line = "Name: ${pluginName}"
            line += "\t| Latest release: ${pluginXml.@version}"
            output.println getPluginInfoHeader()
            output.println line
            printLineSeparator(output)
            def release = pluginXml
            if (release) {
                if (release.'title'.text()) {
                    output.println "${release.'title'.text()}"
                }
                else {
                    output.println "No info about this plugin available"
                }
                printLineSeparator(output)
                if (release.'author'.text()) {
                    output.println "Author: ${release.'author'.text()}"
                    printLineSeparator(output)
                }
                if (release.'authorEmail'.text()) {
                    output.println "Author's e-mail: ${release.'authorEmail'.text()}"
                    printLineSeparator(output)
                }
                if (release.'documentation'.text()) {
                    output.println "Find more info here: ${release.'documentation'.text()}"
                    printLineSeparator(output)
                }
                if (release.'description'.text()) {
                    output.println()
                    output.println "${release.'description'.text()}"
                    output.println()
                }

                printSectionTitle(output, "Dependency Definition")
                output.println("    :${pluginName}:${pluginXml.@version}")
                output.println()
                if (release.repositories.children().size()) {
                    printSectionTitle(output, "Required Repositories")
                    release.repositories.repository.each { repo ->
                        output.println("     ${repo.@url}")
                    }
                }

                if (release.dependencies.children().size()) {
                    printSectionTitle(output, "Transitive Dependencies")
                    printDependencies(output, release.dependencies)
                }

                if (release.plugins.children().size()) {
                    printSectionTitle(output, "Transitive Plugins")
                    printDependencies(output, release.plugins)
                }
            }
            else {
                output.println "<release not found for this plugin>"
                printLineSeparator(output)
            }

            output.println getPluginInfoFooter()
            output.flush()
        }

        return pluginXml
    }

    protected void printDependencies(output, dependencies) {
        dependencies.children().each { scope ->
            def scopeName = scope.name()
            scope.dependency.each { dep ->
                output.println("     ${dep.@group}:${dep.@name}:${dep.@version} ($scopeName)")
            }
        }
    }

    protected void printSectionTitle(PrintWriter output, String title) {
        output.println()
        output.println title
        printLineSeparator(output)
    }

    protected void printLineSeparator(PrintWriter output) {
        output.println '--------------------------------------------------------------------------'
    }

    protected String getPluginInfoHeader() {
    '''
--------------------------------------------------------------------------
Information about Grails plugin
--------------------------------------------------------------------------\
'''
    }

    protected String getPluginInfoFooter() {
'''
To get info about specific release of plugin 'grails plugin-info [NAME] [VERSION]'

To get list of all plugins type 'grails list-plugins'

For further info visit http://grails.org/plugins
'''
    }

    /**
     * Resolve a Plugin zip for for the given name and plugin version
     * @param pluginName The plugin name
     * @param pluginVersion The plugin version
     * @return The location of the local file or null if an error occured
     */
    File resolvePluginZip(String pluginName, String pluginVersion, String scope = "", Map args = [:]) {
        DependencyManager dependencyManager = createFreshDependencyManager()
        def resolveArgs = createResolveArguments(pluginName, pluginVersion)

        dependencyManager.parseDependencies {
            plugins {
                runtime resolveArgs
            }
        }

        messageReporter "Resolving plugin ${pluginName}. Please wait..."
        messageReporter()
        def report = dependencyManager.resolve(scope)

        try {
            def reports = report
            if (report.pluginZips) {
                return report.pluginZips[0]
            }
            messageReporter "Error resolving plugin ${resolveArgs}. Plugin not found."

        } catch (e) {
            messageReporter "Error resolving plugin ${resolveArgs}. ${e.message}"
        }

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
                runtime(resolveArgs) {
                    transitive = false
                }
            }
        }

        def report = dependencyManager.resolveDependencies("runtime", [download:false])
        if (report.getArtifactsReports(null, false)) {
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
