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
 * Gant script that packages a Grails application (note: does not create WAR).
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

if (getBinding().variables.containsKey("_grails_package_called")) return
_grails_package_called = true

includeTargets << grailsScript("_GrailsCompile")
includeTargets << grailsScript("_PackagePlugins")

target(createConfig: "Creates the configuration object") {
    if (!binding.variables.containsKey("configLoaded")) {
        config = projectPackager.createConfig()
        configLoaded = true
    }
}

target(packageApp : "Implementation of package target") {
    depends(createStructure)
    grailsConsole.updateStatus "Packaging Grails application"
    profile("compile") {
        compile()
    }

    projectPackager.classLoader = classLoader

    try {
        config = projectPackager.packageApplication()
    }
    catch(e) {
        grailsConsole.error "Error packaging application: $e.message", e
        exit 1
    }

    configureServerContextPath()

    loadPlugins()
    generateWebXml()

    event("PackagingEnd",[])
}

target(configureServerContextPath: "Configuring server context path") {
    serverContextPath = projectPackager.configureServerContextPath()
}

target(startLogging:"Bootstraps logging") {
    depends(createConfig)
    projectPackager.startLogging(config)
}

target(generateWebXml : "Generates the web.xml file") {
    depends(classpath)
    projectPackager.generateWebXml(pluginManager)
}

target(packageTlds:"packages tld definitions for the correct servlet version") {
    projectPackager.packageTlds()
}

recompileCheck = { lastModified, callback ->
 // do nothing, here for compatibility
}
