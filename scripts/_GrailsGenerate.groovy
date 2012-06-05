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

import org.codehaus.groovy.grails.scaffolding.*
import grails.util.GrailsNameUtils

/**
 * Gant script that generates a CRUD controller and matching views for a given domain class
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsBootstrap")

generateForName = null
generateViews = true
generateController = true
generateForControllerName = null

target(generateForOne: "Generates controllers and views for only one domain class.") {
    depends(loadApp)

    def name = generateForName
    def controllerShortName = generateForControllerName
    name = name.indexOf('.') > -1 ? name : GrailsNameUtils.getClassNameRepresentation(name)
    def domainClass = grailsApp.getDomainClass(name)

    if (!domainClass) {
        println "Domain class not found in grails-app/domain, trying hibernate mapped classes..."
        bootstrap()
        domainClass = grailsApp.getDomainClass(name)
    }

    if (domainClass) {
        generateForDomainClass(domainClass, controllerShortName)
        event("StatusFinal", ["Finished generation for domain class ${domainClass.fullName}"])
    }
    else {
        event("StatusFinal", ["No domain class found for name ${name}. Please try again and enter a valid domain class name"])
        exit(1)
    }
}

target(uberGenerate: "Generates controllers and views for all domain classes.") {
    depends(loadApp)

    def domainClasses = grailsApp.domainClasses

    if (!domainClasses) {
        println "No domain classes found in grails-app/domain, trying hibernate mapped classes..."
        bootstrap()
        domainClasses = grailsApp.domainClasses
    }

    if (domainClasses) {
        domainClasses.each { domainClass -> generateForDomainClass(domainClass) }
        event("StatusFinal", ["Finished generation for domain classes"])
    }
    else {
        event("StatusFinal", ["No domain classes found"])
    }
}

def generateForDomainClass(domainClass, controllerShortName) {
    def templateGenerator = new DefaultGrailsTemplateGenerator(classLoader)

    if (generateViews) {
        if (controllerShortName) {
            event(
                "StatusUpdate",
                ["Generating views for controller ${domainClass.packageName}.${controllerShortName}Controller and domain class ${domainClass.fullName}"])
        }
        else {
            event("StatusUpdate", ["Generating views for domain class ${domainClass.fullName}"])
        }

        templateGenerator.generateViews(domainClass, basedir, controllerShortName)
        event("GenerateViewsEnd", [domainClass.fullName])
    }

    if (generateController) {
        if (controllerShortName) {
            event(
                "StatusUpdate",
                ["Generating controller ${domainClass.packageName}.${controllerShortName}Controller for domain class ${domainClass.fullName}"])
        }
        else {
            event("StatusUpdate", ["Generating controller for domain class ${domainClass.fullName}"])
        }

        templateGenerator.generateController(domainClass, basedir, controllerShortName)

        if (controllerShortName) {
            createUnitTest(
                    name: "${domainClass.packageName}.${controllerShortName}", suffix: "Controller",
                    superClass: "ControllerUnitTestCase")
        }
        else {
            createUnitTest(name: domainClass.fullName, suffix: "Controller", superClass: "ControllerUnitTestCase")
        }

        event("GenerateControllerEnd", [domainClass.fullName])
    }
}

target(determineControllerNameParam: "Parse the arguments to determine controllerName param") {
    if (argsMap["controllerName"]) {
        if (argsMap["controllerName"] instanceof Boolean) {
            def message =
                "Please, check your syntax. Maybe '=' is missing. Parameter 'controllerName' must be specified as " +
                "follows: --controllerName=<YourControllerName>"

            throw new IllegalArgumentException(message)
        }
        else {
            generateForControllerName = argsMap["controllerName"].capitalize()
        }
    }
}