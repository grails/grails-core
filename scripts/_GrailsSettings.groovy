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
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.GrailsNameUtils
import grails.util.GrailsUtil
import grails.util.Metadata
import grails.util.PluginBuildSettings
import org.codehaus.groovy.grails.cli.ScriptExitException
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.util.FileCopyUtils

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
    ant.property(resource: "build.properties")
}

/**
 * Resolves the value for a given property name. It first looks for a
 * system property, then in the BuildSettings configuration, and finally
 * uses the given default value if other options are exhausted.
 */
getPropertyValue = { String propName, defaultValue ->
    // First check whether we have a system property with the given name.
    def value = System.getProperty(propName)
    if (value != null) return value

    // Now try the BuildSettings settings.
    value = buildProps[propName]

    // Return the BuildSettings value if there is one, otherwise use the
    // default.
    return value != null ? value : defaultValue
}


// Set up various build settings. System properties take precedence
// over those defined in BuildSettings, which in turn take precedence
// over the defaults.
isInteractive = true
buildProps = buildConfig.toProperties()
enableProfile = getPropertyValue("grails.script.profile", false).toBoolean()
pluginsHome = grailsSettings.projectPluginsDir.path

// Used to find out about plugins used by this app. The plugin manager
// is configured later when its created (see _PluginDependencies).
pluginSettings = new PluginBuildSettings(grailsSettings)

// While some code still relies on GrailsPluginUtils, make sure it
// uses the same PluginBuildSettings instance as the scripts.
GrailsPluginUtils.pluginBuildSettings = pluginSettings

// Load the application metadata (application.properties)
grailsAppName = null
grailsAppVersion = null
appGrailsVersion = null
servletVersion = getPropertyValue("servlet.version", "2.5")

// server port options
// these are legacy settings
serverPort = getPropertyValue("server.port", 8080).toInteger()
serverPortHttps = getPropertyValue("server.port.https", 8443).toInteger()
serverHost = getPropertyValue("server.host", "localhost")
// which are superceded by these
serverPort = getPropertyValue("grails.server.port.http", serverPort)?.toInteger()
serverPortHttps = getPropertyValue("grails.server.port.https", serverPortHttps)?.toInteger()
serverHost = getPropertyValue("grails.server.host", serverHost)

metadataFile = new File("${basedir}/application.properties")

metadata = metadataFile.exists() ? Metadata.getInstance(metadataFile) : Metadata.current

grailsAppName = metadata.getApplicationName()
grailsAppVersion = metadata.getApplicationVersion()
appGrailsVersion = metadata.getGrailsVersion()
servletVersion = metadata.getServletVersion() ?:  servletVersion

// If no app name property (upgraded/new/edited project) default to basedir.
if (!grailsAppName) {
    grailsAppName = grailsSettings.baseDir.name
}

// FIXME: Fails if 'grails list-plugins is called from a directory
// that starts w/ a '.'
if(grailsAppName.indexOf('/') >-1) {
    appClassName = grailsAppName[grailsAppName.lastIndexOf('/')..-1]
}
else {
    appClassName = GrailsNameUtils.getClassNameRepresentation(grailsAppName)
}


// Other useful properties.
args = System.getProperty("grails.cli.args")
grailsApp = null

isPluginProject = baseFile.listFiles().find { it.name.endsWith("GrailsPlugin.groovy") }

shouldPackageTemplates = false
config = new ConfigObject()


// Pattern that matches artefacts in the 'grails-app' directory.
// Note that the capturing group matches any package directory
// structure.
artefactPattern = /\S+?\/grails-app\/\S+?\/(\S+?)\.groovy/

// Set up the Grails environment for this script.
if (!System.getProperty("grails.env.set")) {
    if (grailsSettings.defaultEnv && getBinding().variables.containsKey("scriptEnv")) {
        grailsEnv = scriptEnv
        grailsSettings.grailsEnv = grailsEnv
        System.setProperty(Environment.KEY, grailsEnv)
        System.setProperty(Environment.DEFAULT, "")
    }
    println "Environment set to ${grailsEnv}"
    System.setProperty("grails.env.set", "true")
}
if(getBinding().variables.containsKey("scriptScope")) {
    buildScope = (scriptScope instanceof BuildScope) ? scriptScope : BuildScope.valueOf(scriptScope.toString());
    buildScope.enable()
}
else {
    buildScope = BuildScope.ALL
    buildScope.enable()
}

// Prepare a configuration file parser based on the current environment.
configSlurper = new ConfigSlurper(grailsEnv)
configSlurper.setBinding(grailsHome:grailsHome,
                         appName:grailsAppName,
                         appVersion:grailsAppVersion,
                         userHome:userHome,
                         basedir:basedir,
                         servletVersion:servletVersion)

// Ant path based on the class loader for the scripts. This basically
// includes all the Grails JARs, the plugin libraries, and any JARs
// provided by the application. Useful for task definitions.
ant.path(id: "core.classpath") {
    classLoader.URLs.each { URL url ->
        pathelement(location: url.file)
    }
}

// a resolver that doesn't throw exceptions when resolving resources
resolver = new PathMatchingResourcePatternResolver()

resolveResources = {String pattern ->
    try {
        return resolver.getResources(pattern)
    }
    catch (Throwable e) {
        return [] as Resource[]
    }
}

// Closure that returns a Spring Resource - either from $GRAILS_HOME
// if that is set, or from the classpath.
grailsResource = {String path ->
    if (grailsSettings.grailsHome) {
        return new FileSystemResource("${grailsSettings.grailsHome}/$path")
    }
    else {
        return new ClassPathResource(path)
    }
}

// Closure that copies a Spring resource to the file system.
copyGrailsResource = { String targetFile, Resource resource ->
    FileCopyUtils.copy(resource.inputStream, new FileOutputStream(targetFile))
}

// Copies a set of resources to a given directory. The set is specified
// by an Ant-style path-matching pattern.
copyGrailsResources = { String destDir, String pattern ->
    new File(destDir).mkdirs()
    Resource[] resources = resolveResources("classpath:${pattern}")
    resources.each { Resource res ->
        if (res.readable) {
            copyGrailsResource("${destDir}/${res.filename}", res)
        }
    }
}

// Closure for unpacking a JAR file that's on the classpath.
grailsUnpack = {Map args ->
    def dir = args["dest"] ?: "."
    def src = args["src"]
    def overwriteOption = args["overwrite"] == null ? true : args["overwrite"]

    // Can't unjar a file from within a JAR, so we copy it to
    // the destination directory first.
    try {
        ant.copy(todir: dir) {
            javaresource(name: src)
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

/**
 * Modifies the application's metadata, as stored in the "application.properties"
 * file. If it doesn't exist, the file is created.
 */
updateMetadata = { Map entries ->
    if (!metadataFile.exists()) {
        ant.propertyfile(
                file: metadataFile,
                comment: "Do not edit app.grails.* properties, they may change automatically. " +
                        "DO NOT put application configuration in here, it is not the right place!")
        metadata = Metadata.getInstance(metadataFile)
    }

    // Convert GStrings to Strings.
    def stringifiedEntries = [:]
    entries.each { key, value -> stringifiedEntries[key.toString()] = value.toString() }

    metadata.putAll(stringifiedEntries)
    metadata.persist()
}

/**
 * Times the execution of a closure, which can include a target. For
 * example,
 *
 *   profile("compile", compile)
 *
 * where 'compile' is the target.
 */
profile = {String name, Closure callable ->
    if (enableProfile) {
        def now = System.currentTimeMillis()
        println "Profiling [$name] start"
        callable()
        def then = System.currentTimeMillis() - now
        println "Profiling [$name] finish. Took $then ms"
    }
    else {
        callable()
    }
}

/**
 * Exits the build immediately with a given exit code.
 */
exit = {
    event("Exiting", [it])
    // Prevent system.exit during unit/integration testing
    if (System.getProperty("grails.cli.testing") || System.getProperty("grails.disable.exit")) {
        throw new ScriptExitException(it)
    } else {
        System.exit(it)
    }
}

/**
 * Interactive prompt that can be used by any part of the build. Echos
 * the given message to the console and waits for user input that matches
 * either 'y' or 'n'. Returns <code>true</code> if the user enters 'y',
 * <code>false</code> otherwise.
 */
confirmInput = {String message, code="confirm.message" ->
    if(!isInteractive) {
        println("Cannot ask for input when --non-interactive flag is passed. You need to check the value of the 'isInteractive' variable before asking for input")
        exit(1)
    }
    ant.input(message: message, addproperty: code, validargs: "y,n")
    def result = ant.antProject.properties[code]
    (result == 'y')
}

// Note: the following only work if you also include _GrailsEvents.
logError = { String message, Throwable t ->
    GrailsUtil.deepSanitize(t)
    t.printStackTrace()
    event("StatusError", ["$message: ${t.message}"])
}

logErrorAndExit = { String message, Throwable t ->
    logError(message, t)
    exit(1)
}
