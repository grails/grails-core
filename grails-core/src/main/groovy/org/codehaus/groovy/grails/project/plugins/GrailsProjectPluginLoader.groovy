/*
 * Copyright 2012 the original author or authors.
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
package org.codehaus.groovy.grails.project.plugins

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.Holders
import groovy.transform.CompileStatic
import org.apache.commons.io.FilenameUtils
import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.io.support.Resource
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPlugin
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils

/**
 * Loads the PluginManager and sets appropriate state
 *
 * @author Graeme Rocher
 * @since 2.2
 */
@CompileStatic
class GrailsProjectPluginLoader extends BaseSettingsApi {

    private static final GrailsConsole grailsConsole = GrailsConsole.getInstance()
    GrailsApplication grailsApplication
    ClassLoader classLoader

    GrailsProjectPluginLoader(GrailsApplication grailsApplication, ClassLoader classLoader, BuildSettings buildSettings, GrailsBuildEventListener buildEventListener) {
        super(buildSettings, buildEventListener,false)
        this.grailsApplication = grailsApplication
        this.classLoader = classLoader
    }

    @CompileStatic
    GrailsPluginManager loadPlugins() {
        if (Holders.pluginManager) {
            // Add the plugin manager to the binding so that it can be accessed from any target.
            return Holders.pluginManager
        }

        GrailsPluginManager pluginManager
        def pluginFiles = pluginSettings.getPluginDescriptorsForCurrentEnvironment()

        try {

            def application
            def pluginClasses = []
            profile("construct plugin manager with ${pluginFiles.inspect()}") {
                for (Resource plugin in pluginFiles) {
                    if (plugin && plugin.file) {
                        def className = plugin.file.name - '.groovy'
                        pluginClasses << classLoader.loadClass(className)
                    }
                }

                profile("creating plugin manager with classes ${pluginClasses}") {
                    if (grailsApplication == null) {
                        grailsApplication = new DefaultGrailsApplication()
                        Holders.grailsApplication = grailsApplication
                    }

//                    if (isEnableProfile()) {
//                        pluginManager = new ProfilingGrailsPluginManager(pluginClasses as Class[], grailsApplication)
//                    }
//                    else {
                        pluginManager = new DefaultGrailsPluginManager(pluginClasses as Class[], grailsApplication)
//                    }

                    pluginSettings.pluginManager = pluginManager
                }
            }

            profile("loading plugins") {
                if (buildEventListener != null) {
                    buildEventListener.triggerEvent("PluginLoadStart", pluginManager)
                }
                pluginManager.loadPlugins()
                Holders.setPluginManager(pluginManager)
                def baseDescriptor = pluginSettings.basePluginDescriptor
                if (baseDescriptor) {
                    def baseName = FilenameUtils.getBaseName(baseDescriptor.filename)
                    def plugin = pluginManager.getGrailsPluginForClassName(baseName)
                    if (plugin) {
                        plugin.basePlugin = true
                    }
                }
                if (pluginManager.failedLoadPlugins) {
                    List<String> pluginNames = pluginManager.failedLoadPlugins.collect { GrailsPlugin plugin -> plugin.getName() }
                    if (buildEventListener != null) {
                        buildEventListener.triggerEvent("StatusError", "Error: The following plugins failed to load due to missing dependencies: ${pluginNames}")
                    }
                    for (GrailsPlugin p in pluginManager.failedLoadPlugins) {
                        println "- Plugin: ${p.getName()}"
                        println "   - Dependencies:"
                        for (depName in p.dependencyNames) {
                            GrailsPlugin depInfo = pluginManager.getGrailsPlugin(depName)
                            def specifiedVersion = p.getDependentVersion(depName)
                            def invalid = depInfo && GrailsPluginUtils.isValidVersion(depInfo.getVersion(), specifiedVersion) ? '' : '[INVALID]'
                            println "       ${invalid ? '!' :'-' } ${depName} (Required: ${specifiedVersion}, Found: ${depInfo?.getVersion() ?: 'Not Installed'}) ${invalid}"
                        }
                    }
                    exit(1)
                }

                pluginManager.doArtefactConfiguration()
                grailsApplication.initialise()

                if (buildEventListener != null) {
                    buildEventListener.triggerEvent("PluginLoadEnd", [pluginManager])
                }
            }
            return pluginManager
        }
        catch (Exception e) {
            grailsConsole.error "Error loading plugin manager: " + e.message , e
            exit(1)
        }
    }
}
