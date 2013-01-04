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
 * Gant script that generates CRUD views for a given domain class
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << grailsScript("_GrailsGenerate")

target ('default': "Generates the CRUD views for a specified domain class") {
    depends(checkVersion, parseArguments, packageApp)
    promptForName(type: "Domain Class")
    generateController = false
    def name = argsMap['params'][0]
    if(!name || name == '*') {
        uberGenerate()
    } else {
        generateForName = name
        generateForOne()
    }
}

USAGE = """
    generate-views [NAME]

where
    NAME       = Either a domain class name (case-sensitive) or a
				 wildcard (*). If you specify the wildcard then
				 views will be generated for all domain classes.
"""
