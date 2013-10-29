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
 * Gant script that creates a new Grails controller
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")

target ('default': "Creates a new controller") {
    depends(checkVersion, parseArguments)

    def type = "Controller"
    promptForName(type: type)

    for (name in argsMap["params"]) {
        name = purgeRedundantArtifactSuffix(name, type)
        createArtifact(name: name, suffix: type, type: type, path: "grails-app/controllers")

        def viewsDir = "${basedir}/grails-app/views/${propertyName}"
        ant.mkdir(dir:viewsDir)
        event("CreatedFile", [viewsDir])

        createUnitTest(name: name, suffix: type, superClass: "ControllerUnitTestCase")
    }

}

USAGE = """
    create-controller [NAME]

where
    NAME       = The name of the controller. If not provided, this
                 command will ask you for the name.
"""