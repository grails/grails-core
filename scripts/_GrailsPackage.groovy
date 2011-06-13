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

import grails.util.PluginBuildSettings
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

import org.apache.log4j.LogManager
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper
import org.codehaus.groovy.grails.plugins.logging.Log4jConfig
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource

/**
 * Gant script that packages a Grails application (note: does not create WAR)
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


    if (configFile.exists()) {
        def configClass
        try {
            configClass = classLoader.loadClass("Config")
        }
        catch (ClassNotFoundException cnfe) {
            console.error "WARNING: No config found for the application."
        }
        if (configClass) {
            try {
                config = configSlurper.parse(configClass)
                config.setConfigFile(configFile.toURI().toURL())
            }
            catch (Exception e) {
                logError("Failed to compile configuration file",e)
                exit(1)
            }
        }
    }

    def dataSourceFile = new File("${basedir}/grails-app/conf/DataSource.groovy")
    if (dataSourceFile.exists()) {
        try {
            def dataSourceConfig = configSlurper.parse(classLoader.loadClass("DataSource"))
            config.merge(dataSourceConfig)
        }
        catch(ClassNotFoundException e) {
            console.error "WARNING: DataSource.groovy not found, assuming dataSource bean is configured by Spring..."
        }
        catch(Exception e) {
            logError("Error loading DataSource.groovy",e)
            exit(1)
        }
    }
    ConfigurationHelper.initConfig(config, null, classLoader)
    ConfigurationHolder.config = config
}

target(packageApp : "Implementation of package target") {
    depends(createStructure, packagePlugins, packageTlds)

    console.updateStatus "Packaging Grails application"

    try {
        profile("compile") {
            compile()
        }
    }
    catch(Exception e) {
        logError("Compilation error",e)
        exit(1)
    }
    profile("creating config") {
        createConfig()
    }

    configureServerContextPath()

    String i18nDir = "${resourcesDirPath}/grails-app/i18n"
    ant.mkdir(dir:i18nDir)

    def files = ant.fileScanner {
        fileset(dir:"${basedir}/grails-app/views", includes:"**/*.jsp")
    }

    if (files.iterator().hasNext()) {
        ant.mkdir(dir:"${basedir}/web-app/WEB-INF/grails-app/views")
        ant.copy(todir:"${basedir}/web-app/WEB-INF/grails-app/views") {
            fileset(dir:"${basedir}/grails-app/views", includes:"**/*.jsp")
        }
    }

    def nativeascii = config.grails.enable.native2ascii
    nativeascii = (nativeascii instanceof Boolean) ? nativeascii : true
    if (nativeascii) {
        Thread.start {
            profile("converting native message bundles to ascii") {
                def ant = new AntBuilder(ant.project)
                ant.native2ascii(src:"${basedir}/grails-app/i18n",
                                 dest:i18nDir,
                                 includes:"**/*.properties",
                                 encoding:"UTF-8")

                PluginBuildSettings settings = pluginSettings
                def i18nPluginDirs = settings.pluginI18nDirectories
                if (i18nPluginDirs) {
                    ExecutorService pool = Executors.newFixedThreadPool(5)
                    for (Resource r in i18nPluginDirs) {
                        pool.execute({ Resource srcDir ->
                            if (srcDir.exists()) {
                                def file = srcDir.file
                                def pluginDir = file.parentFile.parentFile
                                def info = settings.getPluginInfo(pluginDir.absolutePath)

                                if (info) {
                                    def pluginDirName = pluginDir.name
                                    def destDir = "$resourcesDirPath/plugins/${info.name}-${info.version}/grails-app/i18n"
                                    try {
                                        def localAnt = new AntBuilder(ant.project)
                                        localAnt.project.defaultInputStream = System.in
                                        localAnt.mkdir(dir:destDir)
                                        localAnt.native2ascii(src:file,
                                                         dest:destDir,
                                                         includes:"**/*.properties",
                                                         encoding:"UTF-8")
                                    }
                                    catch (e) {
                                        console.error "native2ascii error converting i18n bundles for plugin [${pluginDirName}] ${e.message}"
                                    }
                                }
                            }
                        }.curry(r))
                    }
                }
            }
        }
    }
    else {
        ant.copy(todir:i18nDir) {
            fileset(dir:"${basedir}/grails-app/i18n", includes:"**/*.properties")
        }
    }
    ant.copy(todir:classesDirPath) {
        fileset(dir:"${basedir}", includes:metadataFile.name)
    }

    // Copy resources from various directories to the target "resources" dir.
    packageFiles(basedir)

    startLogging()

    if (grailsSettings.modified || !webXmlFile.exists()) {
        loadPlugins()
        generateWebXml()
    }
    else {
        loadPluginsAsync()
    }
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
    LogManager.resetConfiguration()
    if (config.log4j instanceof Closure) {
        profile("configuring log4j") {
            new Log4jConfig().configure(config.log4j)
        }
    }
    else {
        // setup default logging
        new Log4jConfig().configure()
    }
}

target(generateWebXml : "Generates the web.xml file") {
    depends(classpath)

    if (buildConfig.grails.config.base.webXml) {
        def customWebXml = resolveResources(buildConfig.grails.config.base.webXml)
        if (customWebXml) {
            webXml = customWebXml[0]
        }
        else {
            event("StatusError", [ "Custom web.xml defined in config [${buildConfig.grails.config.base.webXml}] could not be found." ])
            exit(1)
        }
    }
    else {
        webXml = new FileSystemResource("${basedir}/src/templates/war/web.xml")
        def tmpWebXml = "${projectWorkDir}/web.xml.tmp"
        if (!webXml.exists()) {
            copyGrailsResource(tmpWebXml, grailsResource("src/war/WEB-INF/web${servletVersion}.template.xml"))
        }
        else {
            ant.copy(file:webXml.file, tofile:tmpWebXml, overwrite:true)
        }
        webXml = new FileSystemResource(tmpWebXml)
        ant.replace(file:tmpWebXml, token:"@grails.project.key@",
                    value:"${grailsAppName}-${grailsEnv}-${grailsAppVersion}")
    }

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
    ant.mkdir(dir:scaffoldDir)
    if (new File("${basedir}/src/templates/scaffolding").exists()) {
        ant.copy(todir:scaffoldDir, overwrite:true) {
            fileset(dir:"${basedir}/src/templates/scaffolding", includes:"**")
        }
    }
    else {
        copyGrailsResources(scaffoldDir, "src/grails/templates/scaffolding/*")
    }
}

target(packageTlds:"packages tld definitions for the correct servlet version") {
    // We don't know until runtime what servlet version to use, so
    // install the relevant TLDs now.
    copyGrailsResources("${basedir}/web-app/WEB-INF/tld", "web-app/WEB-INF/tld/${servletVersion}/*", false)
}

recompileCheck = { lastModified, callback ->
 // do nothing, here for compatibility
}
