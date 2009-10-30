
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
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoaderHolder
import grails.util.GrailsNameUtils
import org.apache.commons.io.FilenameUtils
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.resolve.IvyDependencyManager
import org.apache.ivy.core.report.ArtifactDownloadReport

/**
 * Gant script that deals with those tasks required for plugin developers
 * (as opposed to plugin users).
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsPackage")

pluginIncludes = [
	metadataFile.name,
	"*GrailsPlugin.groovy",
    "plugin.xml",
    "LICENSE",
    "LICENSE.txt",
    "dependencies.groovy",
	"grails-app/**",
	"lib/**",
    "scripts/**",
	"web-app/**",
	"src/**",
	"docs/api/**",
	"docs/gapi/**"
]

pluginExcludes = [
	"web-app/WEB-INF/**",
	"web-app/plugins/**",
    "grails-app/conf/spring/resources.groovy",
	"grails-app/conf/*DataSource.groovy",
    "grails-app/conf/BootStrap.groovy",
    "grails-app/conf/Config.groovy",
    "grails-app/conf/BuildConfig.groovy",
    "grails-app/conf/UrlMappings.groovy",
	"**/.svn/**",
	"test/**",
	"**/CVS/**"
]

target(packagePlugin:"Implementation target") {
    depends (checkVersion, packageApp)

    def pluginFile
    new File("${basedir}").eachFile {
        if(it.name.endsWith("GrailsPlugin.groovy")) {
            pluginFile = it
        }
    }

    if(!pluginFile) ant.fail("Plugin file not found for plugin project")
    plugin = generatePluginXml(pluginFile)
    generateDependencyDescriptor()

	event("PackagePluginStart", [pluginName])

    // Package plugin's zip distribution
    pluginZip = "${basedir}/grails-${pluginName}-${plugin.version}.zip"
    ant.delete(file:pluginZip)

    def plugin = loadBasePlugin()
    if(plugin?.pluginExcludes) {
        pluginExcludes.addAll(plugin?.pluginExcludes)
    }



    def includesList = pluginIncludes.join(",")
    def excludesList = pluginExcludes.join(",")
    def libsDir = new File("${projectWorkDir}/tmp-libs")
    ant.delete(dir:libsDir, failonerror:false)
    def lowerVersion = GrailsPluginUtils.getLowerVersion(pluginGrailsVersion)

    boolean supportsAtLeastVersion
    try {
        supportsAtLeastVersion = GrailsPluginUtils.supportsAtLeastVersion(lowerVersion, "1.2")
    }
    catch (e) {
        println "Error: Plugin specified an invalid version range: ${pluginGrailsVersion}"
        exit 1 
    }
    if(!supportsAtLeastVersion) {
        IvyDependencyManager dependencyManager = grailsSettings.dependencyManager
        def deps = dependencyManager.resolveApplicationDependencies()

        if(deps) {
            ant.mkdir(dir:"${libsDir}/lib")
            ant.copy(todir:"${libsDir}/lib") {
                for(ArtifactDownloadReport dep in deps) {
                    def file = dep.localFile
                    fileset(dir:file.parentFile, includes:file.name)
                }
            }            
        }
    }

    def dependencyInfoDir = new File("$projectWorkDir/plugin-info")
    ant.zip(destfile:pluginZip, filesonly:true) {
        fileset(dir:basedir, includes:includesList, excludes:excludesList)
        if(dependencyInfoDir.exists())
            fileset(dir:dependencyInfoDir)
        if(libsDir.exists()) {
            fileset(dir:libsDir)
        }
    }

	event("PackagePluginEnd", [pluginName])

}

private generateDependencyDescriptor() {
    ant.delete(dir:"$projectWorkDir/plugin-info", failonerror:false)
    if(grailsSettings.dependencyManager.hasApplicationDependencies()) {
        ant.mkdir(dir:"$projectWorkDir/plugin-info")
        ant.copy(file:"$basedir/grails-app/conf/BuildConfig.groovy", tofile:"$projectWorkDir/plugin-info/dependencies.groovy", failonerror:false)
    }
}
private def loadBasePlugin() {
		pluginManager?.allPlugins?.find { it.basePlugin }
}
