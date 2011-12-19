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

import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource

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
    depends(createConfig)
    // Get the application context path by looking for a property named 'app.context' in the following order of precedence:
    //    System properties
    //    application.properties
    //    config
    //    default to grailsAppName if not specified

    serverContextPath = System.getProperty("app.context")
    serverContextPath = serverContextPath ?: metadata.'app.context'
    serverContextPath = serverContextPath ?: config.grails.app.context
    serverContextPath = serverContextPath ?: grailsAppName

    if (!serverContextPath.startsWith('/')) {
        serverContextPath = "/${serverContextPath}"
    }
}

target(startLogging:"Bootstraps logging") {
    depends(createConfig)
    projectPackager.startLogging(config)
}

target(generateWebXml : "Generates the web.xml file") {
    depends(classpath)
    webXml = new FileSystemResource("${basedir}/src/templates/war/web.xml")
    def tmpWebXml = "${projectWorkDir}/web.xml.tmp"

    if(buildConfig.grails.config.base.webXml) {
        def customWebXml = resolveResources(buildConfig.grails.config.base.webXml)
        def customWebXmlFile = customWebXml[0].file
        if (customWebXmlFile.exists()) {
            ant.copy(file:customWebXmlFile, tofile:tmpWebXml, overwrite:true)
        }
        else {
            event("StatusError", [ "Custom web.xml defined in config [${buildConfig.grails.config.base.webXml}] could not be found." ])
            exit(1)
        }
    } else {
        if (!webXml.exists()) {
            copyGrailsResource(tmpWebXml, grailsResource("src/war/WEB-INF/web${servletVersion}.template.xml"))
        }
        else {
            ant.copy(file:webXml.file, tofile:tmpWebXml, overwrite:true)
        }
    }
    webXml = new FileSystemResource(tmpWebXml)
    ant.replace(file:tmpWebXml, token:"@grails.project.key@",
                    value:"${grailsAppName}-${grailsEnv}-${grailsAppVersion}")

    def sw = new StringWriter()

    try {
        profile("generating web.xml from $webXml") {
            event("WebXmlStart", [webXml.filename])
            pluginManager.doWebDescriptor(webXml, sw)
            webXmlFile.withWriter { it << sw.toString() }
            event("WebXmlEnd", [webXml.filename])
        }
    }
    catch (Exception e) {
        logError("Error generating web.xml file",e)
        exit(1)
    }
}

target(packageTemplates: "Packages templates into the app") {
    projectPackager.packageTemplates(scaffoldDir)
}

target(packageTlds:"packages tld definitions for the correct servlet version") {
    projectPackager.packageTlds()
}

recompileCheck = { lastModified, callback ->
 // do nothing, here for compatibility
}
