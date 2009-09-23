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

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.springframework.core.io.Resource

/**
 * Gant script that handles general initialization of a Grails applications
 *
 * @deprecated Use "create-app --inplace" or "upgrade".
 * @author Graeme Rocher
 * @author Peter Ledbrook
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsInit")

setDefaultTarget("init")

//-------------------------------------------------------------------
// Legacy targets and other stuff
//-------------------------------------------------------------------

ant.path(id: "grails.classpath", runtimeClasspath)

compilerClasspath = { testSources ->

	def excludedPaths = ["views", "i18n", "conf"] // conf gets special handling
	def pluginResources = resolveResources("file:${basedir}/plugins/*/grails-app/*").toList() +
						  resolveResources("file:${basedir}/plugins/*/src/java").toList() +
						  resolveResources("file:${basedir}/plugins/*/src/groovy").toList()

	for(dir in new File("${basedir}/grails-app").listFiles()) {
        if(!excludedPaths.contains(dir.name) && dir.isDirectory())
            src(path:"${dir}")
    }
    // Handle conf/ separately to exclude subdirs/package misunderstandings
    src(path: "${basedir}/grails-app/conf")
    // This stops resources.groovy becoming "spring.resources"
    src(path: "${basedir}/grails-app/conf/spring")

	excludedPaths.remove("conf")
    for(dir in pluginResources.file) {
        if(!excludedPaths.contains(dir.name) && dir.isDirectory()) {
            src(path:"${dir}")
        }
     }


    src(path:"${basedir}/src/groovy")
    src(path:"${basedir}/src/java")
    javac(classpathref:"grails.classpath", debug:"yes")
	if(testSources) {
         src(path:"${basedir}/test/unit")
         src(path:"${basedir}/test/integration")
	}
}

// Complete hack. Automatically starts the server, so the return value
// is just an object that does nothing when "start()" is called on it.
configureHttpServer = {
    runApp()
    return [ start: {->} ]
}

configureHttpServerForWar = {
    runWar()
    return [ start: {->} ]
}

stopWarServer = {
    stopServer()
}

target(promptForName: "Prompts the user for the name of the Artifact if it isn't specified as an argument") {
    if (!args) {
        Ant.input(addProperty: "artifact.name", message: "${typeName} name not specified. Please enter:")
        args = Ant.antProject.properties."artifact.name"
    }
}

target(createArtifact: "Creates a specific Grails artifact") {
    depends(promptForName)

    Ant.mkdir(dir: "${basedir}/${artifactPath}")

    // Extract the package name if one is given.
    def name = args
    def pkg = null
    def pos = args.lastIndexOf('.')
    if (pos != -1) {
        pkg = name[0..<pos]
        name = name[(pos + 1)..-1]
    }

    // Convert the package into a file path.
    def pkgPath = ''
    if (pkg) {
        pkgPath = pkg.replace('.' as char, '/' as char)

        // Make sure that the package path exists! Otherwise we won't
        // be able to create a file there.
        Ant.mkdir(dir: "${basedir}/${artifactPath}/${pkgPath}")

        // Future use of 'pkgPath' requires a trailing slash.
        pkgPath += '/'
    }

    // Convert the given name into class name and property name
    // representations.
    className = GCU.getClassNameRepresentation(name)
    propertyName = GCU.getPropertyNameRepresentation(name)
    artifactFile = "${basedir}/${artifactPath}/${pkgPath}${className}${typeName}.groovy"


    if (new File(artifactFile).exists()) {
        Ant.input(addProperty: "${name}.${typeName}.overwrite", message: "${artifactName} ${className}${typeName}.groovy already exists. Overwrite? [y/n]")
        if (Ant.antProject.properties."${name}.${typeName}.overwrite" == "n")
            return
    }

    // first check for presence of template in application
    templateFile = "${basedir}/src/templates/artifacts/${artifactName}.groovy"
    if (!new File(templateFile).exists()) {
        // now check for template provided by plugins
        Resource[] pluginDirs = pluginSettings.pluginDirectories
        List pluginTemplateFiles = []
        pluginDirs.each {
            File template = new File(it.file, "src/templates/artifacts/${artifactName}.groovy")
            if (template.exists()) {
                pluginTemplateFiles << template
            }
        }

        if (pluginTemplateFiles) {
            templateFile = pluginTemplateFiles[0].path
        } else {
            // template not found in application, use default template
            templateFile = "${grailsHome}/src/grails/templates/artifacts/${artifactName}.groovy"
        }
    }
    Ant.copy(file: templateFile, tofile: artifactFile, overwrite: true)
    Ant.replace(file: artifactFile,
            token: "@artifact.name@", value: "${className}${typeName}")
    if (pkg) {
        Ant.replace(file: artifactFile, token: "@artifact.package@", value: "package ${pkg}\n\n")
    }
    else {
        Ant.replace(file: artifactFile, token: "@artifact.package@", value: "")
    }

    // When creating a domain class, "typename" is empty. So, in order
    // to make the status message sensible, we have to pass something
    // else in.
    event("CreatedFile", [artifactFile])
    event("CreatedArtefact", [ typeName ?: "Domain Class", className])
}
