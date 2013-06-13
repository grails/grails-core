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

import grails.util.GrailsUtil

import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.plugins.publishing.PluginPackager
import org.codehaus.groovy.grails.resolve.AbstractIvyDependencyManager
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

/**
 * Gant script that deals with those tasks required for plugin developers
 * (as opposed to plugin users).
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsPackage")

pluginIncludes = PluginPackager.INCLUDED_RESOURCES
pluginExcludes = PluginPackager.EXCLUDED_RESOURCES

target(packagePlugin: "Implementation target") {
    depends(checkVersion, parseArguments, packageApp)

    def pluginFile
    new File("${basedir}").eachFile {
        if (it.name.endsWith("GrailsPlugin.groovy")) {
            pluginFile = it
        }
    }

    if (!pluginFile) ant.fail("Plugin file not found for plugin project")

    def pluginBaseDir = pluginFile.parentFile.absolutePath
    def resourceList = pluginSettings.getArtefactResourcesForOne(pluginBaseDir)
    pluginInfo = pluginSettings.getPluginInfo(pluginBaseDir)
    def packager = new PluginPackager(grailsSettings, pluginInfo, resourceList, new File(projectWorkDir), eventListener, grailsSettings)
    packager.ant = ant
    packager.resourcesDir = new File(resourcesDirPath)
    packager.hasApplicationDependencies = (grailsSettings.dependencyManager instanceof AbstractIvyDependencyManager) &&
        grailsSettings.dependencyManager.hasApplicationDependencies()

    def descriptor = pluginSettings.getBasePluginDescriptor()
    plugin = packager.generatePluginXml(descriptor.file)

    if (plugin?.hasProperty('pluginExcludes')) {
        pluginInfo.pluginExcludes = plugin.pluginExcludes
    }

    if (argsMap.binary) {
        pluginInfo.packaging = "binary"
        plugin?.packaging = "binary"
    }
    else if (argsMap.source) {
        pluginInfo.packaging = "source"
        plugin?.packaging = "source"
    }
    else if (plugin?.hasProperty('packaging')) {
        pluginInfo.packaging = plugin.packaging
    }

    def pluginGrailsVersion = "${GrailsUtil.grailsVersion} > *"
    def lowerVersion = GrailsPluginUtils.getLowerVersion(pluginGrailsVersion)

    boolean supportsAtLeastVersion
    try {
        supportsAtLeastVersion = GrailsPluginUtils.supportsAtLeastVersion(lowerVersion, "1.2")
    }
    catch (e) {
        grailsConsole.error "Error: Plugin specified an invalid version range: ${pluginGrailsVersion}"
        exit 1
    }

    if (!supportsAtLeastVersion) {
        IvyDependencyManager dependencyManager = grailsSettings.dependencyManager
        def deps = dependencyManager.resolveExportedDependencies()
        if (dependencyManager.resolveErrors) {
            grailsConsole.error "Error: There was an error resolving plugin JAR dependencies"
            exit 1
        }

        if (deps) {
            packager.jarFiles = deps.collect { it.localFile }
        }
    }

    event("PackagePluginStart", [pluginInfo.name])

    // Package plugin's zip or jar distribution
    try {
        pluginZip = packager.packagePlugin(pluginInfo.name, classesDir, grailsSettings.projectTargetDir)
    }
    catch (e) {
        if (e.cause instanceof GrailsTagException) {
            grailsConsole.error "GSP Compilation Error (${e.cause.fileName}:${e.cause.lineNumber}) - $e.cause.message", e.cause
        }
        else {
            grailsConsole.error "Plugin Packaging Error: ${e.message}", e
        }
        exit 1
    }

    grailsConsole.addStatus "Plugin packaged ${new File(pluginZip).name}"

    event("PackagePluginEnd", [pluginInfo.name])
}

private loadBasePlugin() {
    pluginManager?.allPlugins?.find { it.basePlugin }
}
