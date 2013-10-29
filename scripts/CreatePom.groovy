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

import org.codehaus.groovy.grails.cli.maven.MavenPomGenerator

includeTargets << grailsScript("_GrailsArgParsing")

target(default: "Creates a POM for a Grails project") {
    depends(parseArguments)

    if (!argsMap.params) {
        println msg()
        exit 1
    }

    def generator = new MavenPomGenerator(grailsSettings)
    try {
        def group = argsMap.params[0]?.trim()
        if (group) {
            if (argsMap["with-parent"]) {
                generator.generateWithParent group
            }
            else {
                generator.generate group
            }
            grailsConsole.addStatus "POM generated."
        }
        else {
            println msg()
            exit 1
        }
    }
    catch(e) {
        grailsConsole.error "Error occurred creating POM: ${e.message}", e
        exit 1
    }

}

String msg() {
    return '''\
Usage: grails [--with-parent] create-pom <group>
Example: grails create-pom com.mycompany

where:
    with-parent = Adds a <parent> to the POM that references the POM in the
                  parent directory. If no parent POM exists, the command will
                  fail.
'''
}
