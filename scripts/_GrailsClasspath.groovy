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
import org.codehaus.groovy.control.CompilerConfiguration
import org.springframework.core.io.FileSystemResource

/**
 * Gant script containing the Grails classpath setup.
 *
 * @author Peter Ledbrook
 * @author Graeme Rocher
 *
 * @since 1.1
 */

// No point doing this stuff more than once.
if (getBinding().variables.containsKey("_grails_classpath_called")) return
_grails_classpath_called = true

includeTargets << grailsScript("_GrailsSettings")

classpathSet = false
includePluginJarsOnClasspath = true

target(classpath: "Sets the Grails classpath") {
    setClasspath()
}

/**
 * Obtains all of the plug-in Lib directories
 * @deprecated Use "pluginSettings.pluginLibDirectories"
 */
getPluginLibDirs = {
    pluginSettings.pluginLibDirectories
}

/**
 * Obtains an array of all plug-in JAR files as Spring Resource objects
 * @deprecated Use "pluginSettings.pluginJarFiles".
 */
getPluginJarFiles = {
    pluginSettings.pluginJarFiles
}

getJarFiles = {->
    def jarFiles = resolveResources("file:${basedir}/lib/*.jar").toList()
    if(includePluginJarsOnClasspath) {

        def pluginJars = pluginSettings.pluginJarFiles

        for (pluginJar in pluginJars) {
            boolean matches = jarFiles.any {it.file.name == pluginJar.file.name}
            if (!matches) jarFiles.add(pluginJar)
        }
    }

    def userJars = resolveResources("file:${userHome}/.grails/lib/*.jar")
    for (userJar in userJars) {
        jarFiles.add(userJar)
    }

	jarFiles.addAll(getExtraDependencies())

    jarFiles
}

getExtraDependencies = {
	def jarFiles =[]
	if(buildConfig?.grails?.compiler?.dependencies) {
        def extraDeps = ant.fileScanner(buildConfig.grails.compiler.dependencies)
		for(jar in extraDeps) {
            jarFiles << new FileSystemResource(jar)
		}
	}
	jarFiles
}

populateRootLoader = {rootLoader, jarFiles ->
	for(jar in getExtraDependencies()) {
    	rootLoader?.addURL(jar.URL)
	}
    rootLoader?.addURL(new File("${basedir}/web-app/WEB-INF").toURI().toURL())
}

// Only used by "grailsClasspath" closure.
//defaultCompilerDependencies = { antBuilder ->
//    if (antBuilder) {
//        delegate = antBuilder
//        resolveStrategy = Closure.DELEGATE_FIRST
//    }
//
//    grailsSettings.compileDependencies?.each { file ->
//        file(file: file.absolutePath)
//    }
//
//    if (new File("${basedir}/lib").exists()) {
//        fileset(dir: "${basedir}/lib")
//    }
//}

commonClasspath = {
    def grailsDir = resolveResources("file:${basedir}/grails-app/*")
    for (d in grailsDir) {
        pathelement(location: "${d.file.absolutePath}")
    }

    def pluginLibDirs = pluginSettings.pluginLibDirectories.findAll { it.exists() }
    for (pluginLib in pluginLibDirs) {
        fileset(dir: pluginLib.file.absolutePath)
    }
}

compileClasspath = {
    commonClasspath.delegate = delegate
    commonClasspath.call()

    grailsSettings.compileDependencies?.each { File f ->
        pathelement(location: f.absolutePath)
    }
}

testClasspath = {
    commonClasspath.delegate = delegate
    commonClasspath.call()

    grailsSettings.testDependencies?.each { File f ->
        pathelement(location: f.absolutePath)
    }

    pathelement(location: "${classesDir.absolutePath}")
}

runtimeClasspath = {
    commonClasspath.delegate = delegate
    commonClasspath.call()

    grailsSettings.runtimeDependencies?.each { File f ->
        pathelement(location: f.absolutePath)
    }

    pathelement(location: "${classesDir.absolutePath}")
}

/**
 * Converts an Ant path into a list of URLs.
 */
classpathToUrls = { String classpathId ->
    def propName = "converted.classpath"
    ant.pathconvert(refid: classpathId, dirsep: "/", pathsep: ":", property: propName)

    return ant.project.properties.get(propName).split(":").collect { new File(it).toURI().toURL() }
}


void setClasspath() {
    // Make sure the following code is only executed once.
    if (classpathSet) return

    ant.path(id: "grails.compile.classpath", compileClasspath)
    ant.path(id: "grails.test.classpath", testClasspath)
    ant.path(id: "grails.runtime.classpath", runtimeClasspath)

    def grailsDir = resolveResources("file:${basedir}/grails-app/*")
    StringBuffer cpath = new StringBuffer("")

    def jarFiles = getJarFiles()


    for (dir in grailsDir) {
        cpath << dir.file.absolutePath << File.pathSeparator
        // Adding the grails-app folders to the root loader causes re-load issues as
        // root loader returns old class before the grails GCL attempts to recompile it
        //rootLoader?.addURL(dir.URL)
    }
    cpath << classesDirPath << File.pathSeparator
    cpath << "${basedir}/web-app/WEB-INF"
    for (jar in jarFiles) {
        cpath << jar.file.absolutePath << File.pathSeparator
    }

    // We need to set up this configuration so that we can compile the
    // plugin descriptors, which lurk in the root of the plugin's project
    // directory.
    compConfig = new CompilerConfiguration()
    compConfig.setClasspath(cpath.toString());
    compConfig.sourceEncoding = "UTF-8"

//    rootLoader?.addURL(new File("${basedir}/grails-app/conf/hibernate").toURI().toURL())
//    rootLoader?.addURL(new File("${basedir}/src/java").toURI().toURL())

    // The resources directory must be created before it is added to
    // the root loader, otherwise it is quietly ignored. In other words,
    // if the directory is created after its path has been added to the
    // root loader, it will not be included in the classpath.
    def resourcesDir = new File(resourcesDirPath)
    if (!resourcesDir.exists()) {
        resourcesDir.mkdirs()
    }
    rootLoader?.addURL(resourcesDir.toURI().toURL())

    classpathSet = true
}
