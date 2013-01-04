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
 * Gant script that creates a new unit test
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")

target ('default': "Creates a new Grails unit test. A unit test requires that you mock out access to dynamic methods, but executes a lot quicker") {
    depends(checkVersion, parseArguments)

    promptForName(type: "Unit test")

    grailsConsole.updateStatus "Creating test"
    for (name in argsMap["params"]) {
        name = purgeRedundantArtifactSuffix(name, 'Tests')
        createUnitTest(name: name, suffix: "", testType:"UnitTest")
    }
}

USAGE = """
    create-unit-test [NAME]

where
    NAME       = The name of the controller. If not provided, this
                 command will ask you for the name.
"""