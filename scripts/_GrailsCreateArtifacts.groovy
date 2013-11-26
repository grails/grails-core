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

import grails.util.GrailsNameUtils

import org.codehaus.groovy.grails.io.support.FileSystemResource

/**
 * Gant script for creating Grails artifacts of all sorts.
 *
 * @author Peter Ledbrook
 */

includeTargets << grailsScript("_GrailsPackage")

lastType = null
createArtifact = { Map args = [:] ->
    def suffix = args["suffix"]

    def type = args["type"]
    if (type) {
        lastType = type
    }

    def artifactPath = args["path"]

    ant.mkdir(dir: "${basedir}/${artifactPath}")

    // Extract the package name if one is given.
    def name = args["name"]
    def pkg = null
    def pos = name.lastIndexOf('.')
    if (pos != -1) {
        pkg = name[0..<pos]
        name = name[(pos + 1)..-1]
        if (pkg.startsWith("~")) {
            pkg = pkg.replace("~", createRootPackage())
        }
    }
    else {
        pkg = args.skipPackagePrompt ? '' : createRootPackage()
    }

    // Convert the package into a file path.
    def pkgPath = pkg.replace('.' as char, '/' as char)

    // Make sure that the package path exists! Otherwise we won't
    // be able to create a file there.
    ant.mkdir(dir: "${basedir}/${artifactPath}/${pkgPath}")

    // Future use of 'pkgPath' requires a trailing slash.
    if (pkgPath) pkgPath += '/'

    // Convert the given name into class name and property name representations.
    className = GrailsNameUtils.getClassNameRepresentation(name)
    propertyName = GrailsNameUtils.getPropertyNameRepresentation(name)
    artifactFile = "${basedir}/${artifactPath}/${pkgPath}${className}${suffix}.groovy"

    File destination = new File(artifactFile)
    if (destination.exists()) {
        if (!confirmInput("${type} ${className}${suffix}.groovy already exists. Overwrite?","${name}.${suffix}.overwrite")) {
            return
        }
    }

    def templatePath = args["templatePath"] ?: 'templates/artifacts'
    // first check for presence of template in application
    templateFile = new FileSystemResource("${basedir}/src/${templatePath}/${type ?: lastType}.groovy")
    if (!templateFile.exists()) {
        // now check for template provided by plugins
        def possibleResources = pluginSettings.pluginDirectories.collect { dir ->
            new FileSystemResource("${dir.path}/src/${templatePath}/${type ?: lastType}.groovy")
        }
        templateFile = possibleResources.find { it.exists() }
        if (!templateFile) {
            // template not found in application, use default template
            templateFile = grailsResource("src/grails/${templatePath}/${type ?: lastType}.groovy")
        }
    }

    if (!templateFile.exists()) {
        if (artifactPath.startsWith("test")) {
            templateFile = grailsResource("src/grails/$templatePath/UnitTest.groovy")
        }
        else {
            templateFile = grailsResource("src/grails/$templatePath/Generic.groovy")
        }
    }

    copyGrailsResource(artifactFile, templateFile)
    ant.replace(file: artifactFile, encoding:'UTF-8', token: "@artifact.name@", value: "${className}${suffix}")
    if (pkg) {
        ant.replace(file: artifactFile, encoding:'UTF-8', token: "@artifact.package@", value: "package ${pkg}\n\n")
    }
    else {
        ant.replace(file: artifactFile, encoding:'UTF-8', token: "@artifact.package@", value: "")
    }

    if (args["superClass"]) {
        ant.replace(file: artifactFile, encoding:'UTF-8', token: "@artifact.superclass@", value: args["superClass"])
    }
    ant.replace(file: artifactFile, encoding:'UTF-8', token: "@artifact.testclass@", value: "${className}${type}")

    // optional extra ant.replace name/value pairs
    args.replacements.each { token, value ->
        ant.replace(file: artifactFile, encoding:'UTF-8', token: token, value: value)
    }

    String lineSeparator = System.getProperty('line.separator')
    StringBuilder fixed = new StringBuilder()
    destination.getText('UTF-8').eachLine { fixed << it << lineSeparator }
    destination.withWriter('UTF-8') { it.write fixed.toString() }

    event("CreatedFile", [artifactFile])
    event("CreatedArtefact", [ artifactFile, className])
}

private createRootPackage() {
    compile()
    createConfig()
    return (config.grails.project.groupId ?: grailsAppName).replace('-','.').toLowerCase()
}

createIntegrationTest = { Map args = [:] ->
    def superClass = args["superClass"] ?: "GroovyTestCase"
    createArtifact(
            name: args["name"],
            suffix: "${args['suffix']}Spec",
            type: args.testType ?: args['suffix'],
            path: "test/integration",
            superClass: superClass,
            templatePath:"templates/testing",
            skipPackagePrompt: args['skipPackagePrompt'])
}

createUnitTest = { Map args = [:] ->
    def superClass = args["superClass"] ?: "GrailsUnitTestCase"
    createArtifact(
            name: args["name"],
            suffix: "${args['suffix']}Spec",
            type: args.testType ?: args['suffix'],
            path: "test/unit",
            superClass: superClass,
            templatePath:"templates/testing",
            skipPackagePrompt: args['skipPackagePrompt'])
}

promptForName = { Map args = [:] ->
    if (!argsMap["params"]) {
        argsMap["params"] << grailsConsole.userInput("${args["type"]} name not specified. Please enter:")
    }
}

purgeRedundantArtifactSuffix = { name, suffix ->
    if (name && suffix) {
        if (name =~ /.+$suffix$/) {
            name = name.replaceAll(/$suffix$/, "")
        }
    }
    name
}
