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

import groovy.xml.MarkupBuilder
import grails.util.GrailsNameUtils
import grails.util.PluginBuildSettings
import grails.util.GrailsUtil
import org.apache.commons.io.FilenameUtils
import org.apache.ivy.core.report.ArtifactDownloadReport

import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoaderHolder
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.codehaus.groovy.grails.plugins.publishing.PluginPackager

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

target(packagePlugin:"Implementation target") {
    depends(checkVersion, parseArguments, packageApp)

    def pluginFile
    new File("${basedir}").eachFile {
        if (it.name.endsWith("GrailsPlugin.groovy")) {
            pluginFile = it
        }
    }


    if (!pluginFile) ant.fail("Plugin file not found for plugin project")

	def pluginBaseDir = pluginFile.parentFile.absolutePath
	plugin = pluginSettings.getPluginInfo(pluginBaseDir)
    def resourceList = pluginSettings.getArtefactResourcesForOne(pluginBaseDir)
	
	def packager = new PluginPackager(plugin,resourceList, new File(projectWorkDir))
	packager.ant = ant
	packager.resourcesDir = new File(resourcesDirPath)
	packager.hasApplicationDependencies = grailsSettings.dependencyManager.hasApplicationDependencies()

	def pluginGrailsVersion = "${GrailsUtil.grailsVersion} > *"	
	def lowerVersion = GrailsPluginUtils.getLowerVersion(pluginGrailsVersion)

    boolean supportsAtLeastVersion
    try {
        supportsAtLeastVersion = GrailsPluginUtils.supportsAtLeastVersion(lowerVersion, "1.2")
    }
    catch (e) {
        println "Error: Plugin specified an invalid version range: ${pluginGrailsVersion}"
        exit 1
    }

    if (!supportsAtLeastVersion) {
        IvyDependencyManager dependencyManager = grailsSettings.dependencyManager
        def deps = dependencyManager.resolveExportedDependencies()
        if (dependencyManager.resolveErrors) {
            println "Error: There was an error resolving plugin JAR dependencies"
            exit 1
        }

        if (deps) {
			packager.jarFiles = deps.collect { it.localFile }
        }
    }
		
    event("PackagePluginStart", [plugin.name])

    // Package plugin's zip distribution
	if(argsMap.binary) {
	    pluginZip = packager.packageBinary(plugin.name, classesDir, grailsSettings.projectTargetDir)		
	}
	else {
	    pluginZip = packager.packagePlugin(plugin.name, classesDir, grailsSettings.projectTargetDir)
	}


    event("PackagePluginEnd", [plugin.name])
}


private loadBasePlugin() {
    pluginManager?.allPlugins?.find { it.basePlugin }
}
