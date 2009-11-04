/*
* Copyright 2008 the original author or authors.
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

import org.springframework.core.io.FileSystemResource
import grails.util.GrailsNameUtils

/**
 * Gant script for creating Grails artifacts of all sorts.
 *
 * @author Peter Ledbrook
 */

createArtifact = { Map args = [:] ->
    def suffix = args["suffix"]
    def type = args["type"]
    def artifactPath = args["path"]

    ant.mkdir(dir: "${basedir}/${artifactPath}")

    // Extract the package name if one is given.
    def name = args["name"]
    def pkg = null
    def pos = name.lastIndexOf('.')
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
        ant.mkdir(dir: "${basedir}/${artifactPath}/${pkgPath}")

        // Future use of 'pkgPath' requires a trailing slash.
        pkgPath += '/'
    }
    else if(!args.skipPackagePrompt) {
        if(!confirmInput("WARNING: You have not specified a package. It is good practise to place classes in packages (eg. mycompany.Book). Do you want to continue?", "no.package.warning")) {
            exit(1)
        }
    }

    // Convert the given name into class name and property name
    // representations.
    className = GrailsNameUtils.getClassNameRepresentation(name)
    propertyName = GrailsNameUtils.getPropertyNameRepresentation(name)
    artifactFile = "${basedir}/${artifactPath}/${pkgPath}${className}${suffix}.groovy"


    if (new File(artifactFile).exists()) {
        if(!confirmInput("${type} ${className}${suffix}.groovy already exists. Overwrite? [y/n]","${name}.${suffix}.overwrite")) {
            return
        }
    }

    // first check for presence of template in application
    templateFile = new FileSystemResource("${basedir}/src/templates/artifacts/${type}.groovy")
    if (!templateFile.exists()) {
        // now check for template provided by plugins
        def pluginTemplateFiles = resolveResources("file:${pluginsHome}/*/src/templates/artifacts/${type}.groovy")
        if (pluginTemplateFiles) {
            templateFile = pluginTemplateFiles[0]
        } else {
            // template not found in application, use default template
            templateFile = grailsResource("src/grails/templates/artifacts/${type}.groovy")
        }
    }

    copyGrailsResource(artifactFile, templateFile)
//    ant.copy(file: templateFile, tofile: artifactFile, overwrite: true)
    ant.replace(file: artifactFile,
            token: "@artifact.name@", value: "${className}${suffix}")
    if (pkg) {
        ant.replace(file: artifactFile, token: "@artifact.package@", value: "package ${pkg}\n\n")
    }
    else {
        ant.replace(file: artifactFile, token: "@artifact.package@", value: "")
    }

    if (args["superClass"]) {
        ant.replace(file: artifactFile, token: "@artifact.superclass@", value: args["superClass"])
    }

    event("CreatedFile", [artifactFile])
    event("CreatedArtefact", [ type, className])
}

createIntegrationTest = { Map args = [:] ->
    def superClass = args["superClass"] ?: "GrailsUnitTestCase"
	createArtifact(name: args["name"], suffix: "${args['suffix']}Tests", type: "Tests", path: "test/integration", superClass: superClass)
}

createUnitTest = { Map args = [:] ->
    def superClass = args["superClass"] ?: "GrailsUnitTestCase"
	createArtifact(name: args["name"], suffix: "${args['suffix']}Tests", type: "Tests", path: "test/unit", superClass: superClass)
}

promptForName = { Map args = [:] ->
    if (!argsMap["params"]) {
        ant.input(addProperty: "artifact.name", message: "${args["type"]} name not specified. Please enter:")
        argsMap["params"] << ant.antProject.properties."artifact.name"
    }
}
