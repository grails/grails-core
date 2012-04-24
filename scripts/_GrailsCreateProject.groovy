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

import grails.util.GrailsNameUtils
import grails.util.Metadata

/**
 * Handles the creation of Grails applications.
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsPlugins")
includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("IntegrateWith")

grailsAppName = ""
projectType = "app"

target(createApp: "Creates a Grails application for the given name") {
    depends(parseArguments, appName)
    initProject()

    // Create a message bundle to get the user started.
    touch(file: metadataFile)

    // Set the default version number for the application
    updateMetadata("app.version": grailsAppVersion ?: "0.1")

    event("StatusFinal", ["Created Grails Application at $basedir"])
}

def resetBaseDirectory(String basedir) {
    // Update the build settings and reload the build configuration.
    grailsSettings.baseDir = new File(basedir)
    grailsSettings.loadConfig()

    // Reload the application metadata.
    metadataFile = new File("$basedir/${Metadata.FILE}")
    metadata = Metadata.getInstance(metadataFile)

    // Reset the plugin stuff.
    pluginSettings.clearCache()
    pluginsHome = grailsSettings.projectPluginsDir.path
}

target(createPlugin: "The implementation target") {
    depends(parseArguments, appName)
    metadataFile = new File("${basedir}/application.properties")
    projectType = "plugin"
    initProject()

    // Rename the plugin descriptor.
    pluginName = GrailsNameUtils.getNameFromScript(grailsAppName)
    if (!(pluginName ==~ /[a-zA-Z][a-zA-Z0-9-]+/)) {
        grailsConsole.error "Error: Specified plugin name [$grailsAppName] is invalid. Plugin names can only contain word characters and numbers separated by hyphens."
        exit 1
    }

    ant.move(
        file: "${basedir}/GrailsPlugin.groovy",
        tofile: "${basedir}/${pluginName}GrailsPlugin.groovy",
        overwrite: true)

    // The plugin's Grails version should be based on the current minor version,
    // not the patch level. For example, 1.3.6 should be converted to 1.3.
    def m = grailsVersion =~ /^(\d+\.\d+)(?:\.\d+)?(?:.*)/
    def pluginGrailsVersion = m[0][1]

    // Insert the name of the plugin into whatever files need it.
    ant.replace(dir:"${basedir}") {
        include(name: "*GrailsPlugin.groovy")
        include(name: "scripts/*")
        replacefilter(token: "@plugin.name@", value: pluginName)
        replacefilter(token: "@plugin.title@", value: GrailsNameUtils.getNaturalName(pluginName) + " Plugin")
        replacefilter(token: "@plugin.short.name@", value: GrailsNameUtils.getScriptName(pluginName))
        replacefilter(token: "@plugin.version@", value: grailsAppVersion ?: "0.1")
        replacefilter(token: "@grails.version@", value: pluginGrailsVersion)
    }

    event("StatusFinal", [ "Created plugin ${pluginName}" ])
}

target(initProject: "Initialise an application or plugin project") {
    depends(createStructure, updateAppProperties)

    grailsUnpack(dest: basedir, src: "grails-shared-files.jar")
    grailsUnpack(dest: basedir, src: "grails-$projectType-files.jar")
    integrateEclipse()

    // make sure Grails central repo is prepped for default plugin set installation
    grailsSettings.dependencyManager.parseDependencies {
        repositories {
            grailsCentral()
        }
    }
}

target (appName : "Evaluates the application name") {
    if (!argsMap["params"]) {
        String type = scriptName.toLowerCase().indexOf('plugin') > -1 ? 'Plugin' : 'Application'
        ant.input(message:"$type name not specified. Please enter:",
                  addProperty:"grails.app.name")
        grailsAppName = ant.antProject.properties."grails.app.name"
    }
    else {
        grailsAppName = argsMap["params"].join(" ")
    }

    if (!argsMap["inplace"]) {
        basedir = "${basedir}/${grailsAppName}"
        resetBaseDirectory(basedir)
    }

    if (argsMap["appVersion"]) {
        grailsAppVersion = argsMap["appVersion"]
    }

    appClassName = GrailsNameUtils.getClassNameRepresentation(grailsAppName)
}
