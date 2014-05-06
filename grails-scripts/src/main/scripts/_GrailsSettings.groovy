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

import grails.util.BuildScope
import grails.util.Environment

import org.codehaus.groovy.grails.plugins.GrailsPluginUtils

/**
 * Gant script containing build variables.
 *
 * @author Peter Ledbrook
 *
 * @since 1.1
 */

// No point doing this stuff more than once.
if (getBinding().variables.containsKey("_settings_called")) return
_settings_called = true

// Read build properties for Grails into ant properties.
if (grailsSettings.grailsHome) {
    ant.property(file: "${grailsHome}/build.properties")
}
else {
    ant.property(resource: "grails.build.properties")
}

// Set up various build settings. System properties take precedence
// over those defined in BuildSettings, which in turn take precedence
// over the defaults.

// While some code still relies on GrailsPluginUtils, make sure it
// uses the same PluginBuildSettings instance as the scripts.
GrailsPluginUtils.pluginBuildSettings = pluginSettings

// Other useful properties.
grailsApp = null
projectWatcher = null

isPluginProject = baseFile.listFiles().find { it.name.endsWith("GrailsPlugin.groovy") }

config = new ConfigObject()

// Pattern that matches artefacts in the 'grails-app' directory.
// Note that the capturing group matches any package directory structure.
artefactPattern = /\S+?\/grails-app\/\S+?\/(\S+?)\.groovy/

// Set up the Grails environment for this script.
if (!System.getProperty("grails.env.set")) {
    if (grailsSettings.defaultEnv && getBinding().variables.containsKey("scriptEnv")) {
        grailsEnv = scriptEnv
        grailsSettings.grailsEnv = grailsEnv
        configSlurper.environment = grailsEnv
        System.setProperty(Environment.KEY, grailsEnv)
        System.setProperty(Environment.DEFAULT, "")
    }
    grailsConsole.updateStatus "Environment set to $grailsEnv"
    grailsConsole.category << grailsEnv
    System.setProperty("grails.env.set", "true")
}

if (getBinding().variables.containsKey("scriptScope")) {
    buildScope = (scriptScope instanceof BuildScope) ? scriptScope : BuildScope.valueOf(scriptScope.toString().toUpperCase())
    buildScope.enable()
}
else {
    buildScope = BuildScope.ALL
    buildScope.enable()
}

// Ant path based on the class loader for the scripts. This basically
// includes all the Grails JARs, the plugin libraries, and any JARs
// provided by the application. Useful for task definitions.
ant.path(id: "core.classpath") {
    for (url in classLoader.URLs) {
        pathelement(location: url.file)
    }
}

// Closure for unpacking a JAR file that's on the classpath.
grailsUnpack = { Map args ->
    def dir = args["dest"] ?: "."
    def src = args["src"]
    def overwriteOption = args["overwrite"] == null ? true : args["overwrite"]

    // Can't unjar a file from within a JAR, so we copy it to
    // the destination directory first.
    try {
        File templatesDir = new File(grailsSettings.grailsWorkDir, 'app-templates')
        File jar = new File(templatesDir, src)
        if (!args.skipTemplates && jar.exists()) {
            // use the customized templates
            ant.copy(todir: dir, file: jar)
        }
        else {
            // extract from grails-resources-VERSION.jar
            ant.copy(todir: dir) {
                javaresource(name: src)
            }
        }

        // Now unjar it, excluding the META-INF directory.
        ant.unjar(dest: dir, src: "${dir}/${src}", overwrite: overwriteOption) {
            patternset {
                exclude(name: "META-INF/**")
            }
        }
    }
    finally {
        // Don't need the JAR file any more, so remove it.
        ant.delete(file: "${dir}/${src}", failonerror:false)
    }
}
