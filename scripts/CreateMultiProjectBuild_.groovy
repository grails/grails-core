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

/**
 * @author Graeme Rocher
 * @since 2.1
 */

import org.codehaus.groovy.grails.cli.maven.MavenMultiProjectGenerator

includeTargets << grailsScript("_GrailsArgParsing")

target(default: "Creates a multi-project build for Maven") {
    depends(parseArguments)

    if (!argsMap.params) {
        println msg()
        exit 1
    }

    def generator = new MavenMultiProjectGenerator(grailsSettings)
    try {
        def coordinates = argsMap.params[0].split(":")
        if (coordinates.size() != 3) {
            println "You must specify groupId, artifactId and version in the argument - ${argsMap.params[0]} is invalid"
            println()
            println msg()
            exit 1
        }

        def (group, name, version) = coordinates
        if (group && name && version) {
            generator.generate group, name, version
            grailsConsole.addStatus "Multi-module Maven build configured."
            grailsConsole.log """
Sub-projects have not been configured with the dependencies explicitly declared in BuildConfig.groovy. \
You can either manually add them to each project's pom.xml or run `create-pom --with-parent` in the sub-projects. \
If you run `create-pom` in the sub-projects, you will have to manually add the inter-project dependencies.
"""
        }
        else {
            println msg()
            exit 1
        }

    }
    catch(e) {
        grailsConsole.error "Error occurred creating multi-project build: ${e.message}", e
        exit 1
    }

}

String msg() {
    return '''\
Usage: grails create-multi-project-build <group>:<name>:<version>
Example: grails create-multi-project-build org.mycompany:foo:1.0
'''
}
