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
 * Gant script that generates a CRUD controller and matching views for a given domain class
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.scaffolding.*

includeTargets << grailsScript ( "Bootstrap" )
    
generateViews = true
generateController = true

target ('default': "Generates a CRUD interface (controller + views) for a domain class") {
    depends( checkVersion, packageApp )
    typeName = "Domain Class"
    promptForName()
    generateAll()
}            

target(generateAll:"The implementation target") {

    rootLoader.addURL(classesDir.toURI().toURL())
    loadApp()

    templateGenerator = new DefaultGrailsTemplateGenerator()

    def name = args.trim()
    if (!name || name == "*") {
        uberGenerate()
    }
    else {
        generateForOne(name: name)
    }
}

target(generateForOne: "Generates controllers and views for only one domain class.") { Map args ->
    def name = args["name"]
    name = name.indexOf('.') > -1 ? name : GCU.getClassNameRepresentation(name)
    def domainClass = grailsApp.getDomainClass(name)

    if(!domainClass) {
        println "Domain class not found in grails-app/domain, trying hibernate mapped classes..."
        doRuntimeConfig(grailsApp, appCtx)
        domainClass = grailsApp.getDomainClass(name)
    }

    if(domainClass) {
        generateForDomainClass(domainClass)
        event("StatusFinal", ["Finished generation for domain class ${domainClass.fullName}"])
    }
    else {
        event("StatusFinal", ["No domain class found for name ${name}. Please try again and enter a valid domain class name"])
    }
}

target(uberGenerate: "Generates controllers and views for all domain classes.") {
    rootLoader.addURL(classesDir.toURI().toURL())
    loadApp()

    def domainClasses = grailsApp.domainClasses

    if (!domainClasses) {
        println "No domain classes found in grails-app/domain, trying hibernate mapped classes..."
        doRuntimeConfig(grailsApp, appCtx)
        domainClasses = grailsApp.domainClasses
    }

   if (domainClasses) {
        def generator = new DefaultGrailsTemplateGenerator()
        domainClasses.each { domainClass ->
            generateForDomainClass(domainClass)
        }
        event("StatusFinal", ["Finished generation for domain classes"])
    }
    else {
        event("StatusFinal", ["No domain classes found"])
    }
}

def doRuntimeConfig(app, ctx) {
    try {
        def config = new GrailsRuntimeConfigurator(app, ctx)
        appCtx = config.configure(ctx.servletContext)
    }
    catch (Exception e) {
        println e.message
        e.printStackTrace()
    }
}

def generateForDomainClass(domainClass) {
    if(generateViews) {
        event("StatusUpdate", ["Generating views for domain class ${domainClass.fullName}"])
        templateGenerator.generateViews(domainClass, basedir)
        event("GenerateViewsEnd", [domainClass.fullName])
    }
    if(generateController) {
        event("StatusUpdate", ["Generating controller for domain class ${domainClass.fullName}"])
        templateGenerator.generateController(domainClass, basedir)
        event("GenerateControllerEnd", [domainClass.fullName])
    }
}
