/*
 * Copyright 2012 SpringSource
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
package org.codehaus.groovy.grails.project.packaging

import grails.build.logging.GrailsConsole
import grails.util.BuildScope
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.Metadata
import grails.util.PluginBuildSettings
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder

import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo

/**
 * Creates a WAR file from a Grails project.
 *
 * @author Graeme Rocher
 * @since 2.1
 */
class GrailsProjectWarCreator extends BaseSettingsApi {

    boolean includeJars = true
    boolean buildExplodedWar
    String warName
    BuildSettings grailsSettings
    GrailsBuildEventListener eventListener
    def additionalEventArgs

    private String artefactPattern = /\S+?\/grails-app\/\S+?\/(\S+?)\.groovy/
    private String basedir
    private ConfigObject buildConfig
    private AntBuilder ant
    private GrailsProjectPackager projectPackager
    private String resourcesDirPath
    private String pluginClassesDirPath
    private String classesDirPath
    private File stagingDir
    private File webXmlFile
    private BuildScope buildScope
    private grailsEnv
    private GrailsConsole grailsConsole = GrailsConsole.getInstance()
    Closure defaultWarDependencies

    GrailsProjectWarCreator(BuildSettings settings, GrailsBuildEventListener buildEventListener, GrailsProjectPackager projectPackager,
            AntBuilder ant = new AntBuilder(), boolean interactive = false) {

        super(settings, buildEventListener, interactive)

        eventListener = buildEventListener
        this.projectPackager = projectPackager
        this.ant = ant
        grailsSettings = settings
        buildConfig = grailsSettings.config
        basedir = grailsSettings.baseDir.absolutePath
        grailsEnv = grailsSettings.grailsEnv
        resourcesDirPath = grailsSettings.resourcesDir.absolutePath
        pluginClassesDirPath = grailsSettings.pluginClassesDir.absolutePath
        classesDirPath = grailsSettings.classesDir.absolutePath
        stagingDir = grailsSettings.projectWarExplodedDir
        webXmlFile = grailsSettings.webXmlLocation
        buildScope = BuildScope.current
        buildExplodedWar = getPropertyValue("grails.war.exploded", false)
        defaultWarDependencies = { antBuilder ->

            if (antBuilder) {
                delegate = antBuilder
                resolveStrategy = Closure.DELEGATE_FIRST
            }

            // For backwards compatibility, we handle the list version of
            // "grails.war.dependencies" specially.
            if (buildConfig.grails.war.dependencies instanceof List) {
                fileset(dir: "${grailsHome}/dist") {
                    include(name: "grails-*.jar")
                    exclude(name: "grails-scripts-*.jar")
                }

                fileset(dir:"${grailsHome}/lib") {
                    for (d in buildConfig.grails.war.dependencies) {
                        include(name: d)
                    }
                }
            }
            else {
                def dependencies = grailsSettings.runtimeDependencies
                if (dependencies) {
                    for (File f in dependencies) {
                        if (f && f.name.endsWith(".jar")) {
                            fileset(dir: f.parent, includes: f.name)
                        }
                    }
                }
            }
        }
    }

    void packageWar() {
        try {
            projectPackager.projectCompiler.compileGroovyPages(grailsAppName, grailsSettings.classesDir)
            packageWarOnly()
        }
        catch (e) {
            if (e.cause?.class?.name == "org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException") {
                eventListener.triggerEvent("StatusError", "GSP Compilation error in file $e.cause.fileName at line $e.cause.lineNumber: $e.cause.message")
            }
            else {
                eventListener.triggerEvent("StatusError", "WAR packaging error: ${e.cause?.message ?: e.message}")
            }
            exit(1)
        }
    }

    void packageWarOnly() {

        def includeOsgiHeaders = grailsSettings.projectWarOsgiHeaders

        try {
            for (pluginDir in pluginSettings.inlinePluginDirectories) {
                grailsConsole.updateStatus "Generating plugin.xml for inline plugin"
                projectPackager.generatePluginXml(pluginSettings.getPluginDescriptor(pluginDir).file, false)
            }

            configureWarName()

            ant.mkdir(dir:stagingDir)

            eventListener.triggerEvent("StatusUpdate", "Building WAR file")

            ant.copy(todir:stagingDir, overwrite:true, preservelastmodified:true) {
                // Allow the application to override the step that copies
                // 'web-app' to the staging directory.
                if (buildConfig.grails.war.copyToWebApp instanceof Closure) {
                    def callable = buildConfig.grails.war.copyToWebApp
                    callable.delegate = ant
                    callable.resolveStrategy = Closure.DELEGATE_FIRST
                    callable(additionalEventArgs)
                }
                else {
                    fileset(dir: "${basedir}/web-app", includes:"**")
                }
            }
            // package plugin js/etc.
            projectPackager.packagePluginsForWar(stagingDir)

            ant.copy(todir: "${stagingDir}/WEB-INF/grails-app", overwrite: true, preservelastmodified:true) {
                fileset(dir: "${basedir}/grails-app", includes: "views/**")
                fileset(dir: "${resourcesDirPath}/grails-app", includes: "i18n/**")
            }

            def classesDirExcludes = {
                exclude(name: "hibernate")
                exclude(name: "spring")
                exclude(name: "hibernate/*")
                exclude(name: "spring/*")
            }

            ant.copy(todir:"${stagingDir}/WEB-INF/classes", preservelastmodified:true) {
                fileset(dir:pluginClassesDirPath, classesDirExcludes)
            }

            ant.copy(todir:"${stagingDir}/WEB-INF/classes", overwrite:true, preservelastmodified:true) {
                fileset(dir:classesDirPath, classesDirExcludes)
            }

            ant.mkdir(dir:"${stagingDir}/WEB-INF/spring")

            ant.copy(todir:"${stagingDir}/WEB-INF/spring", preservelastmodified:true) {
                fileset(dir:"${basedir}/grails-app/conf/spring", includes:"**/*.xml")
            }

            ant.copy(todir:"${stagingDir}/WEB-INF/classes", failonerror:false, preservelastmodified:true) {
                fileset(dir:"${basedir}/grails-app/conf") {
                    exclude(name:"*.groovy")
                    exclude(name:"log4j.*")
                    exclude(name:"**/hibernate/**")
                    exclude(name:"**/spring/**")
                }
                fileset(dir:"${basedir}/grails-app/conf/hibernate", includes:"**/**")
                fileset(dir:"${grailsSettings.sourceDir}/java") {
                    include(name:"**/**")
                    exclude(name:"**/*.java")
                }
                fileset(dir:"${grailsSettings.sourceDir}/groovy") {
                    include(name:"**/**")
                    exclude(name:"**/*.groovy")
                }
                fileset(dir: resourcesDirPath, includes:"log4j.properties")
            }

            // Copy the project's dependencies (JARs mainly) to the staging area.
            if (includeJars) {
                ant.copy(todir:"${stagingDir}/WEB-INF/lib", preservelastmodified:true) {
                    if (buildConfig.grails.war.dependencies instanceof Closure) {
                        def deps = buildConfig.grails.war.dependencies
                        deps.delegate = ant
                        deps.resolveStrategy = Closure.DELEGATE_FIRST
                        deps()
                    }
                    else {
                        defaultWarDependencies(delegate)
                    }
                }
            }

            ant.copy(file: webXmlFile.absolutePath,
                     tofile: "${stagingDir}/WEB-INF/web.xml",
                     overwrite:true, preservelastmodified:true)

            def webXML = new File(stagingDir, "WEB-INF/web.xml")
            def xmlInput = new XmlParser().parse(webXML)
            webXML.withWriter { xmlOutput ->
                def printer = new XmlNodePrinter(new PrintWriter(xmlOutput), '\t')
                printer.preserveWhitespace = true
                printer.print(xmlInput)
            }

            ant.delete(file:webXmlFile)
            PluginBuildSettings ps = pluginSettings
            def compileScopePluginInfo = ps.compileScopePluginInfo
            def compileScopePluginInfos = ps.getCompileScopedSupportedPluginInfos()
            def resourceList = ps.getCompileScopedArtefactResources()

            if (includeJars && compileScopePluginInfos) {
                def libDir = "${stagingDir}/WEB-INF/lib"
                // Copy embedded libs (dependencies declared inside dependencies.groovy are already provided)
                ant.copy(todir:libDir, flatten:true, failonerror:false, preservelastmodified:true) {
                    for (GrailsPluginInfo info in compileScopePluginInfos) {
                        fileset(dir: info.pluginDir.file.path) {
                            include(name:"lib/*.jar")
                        }
                    }
                }
            }

            String metaInfo = "$stagingDir/META-INF"
            ant.mkdir(dir:metaInfo)
            String manifestFile = "$metaInfo/MANIFEST.MF"
            ant.manifest(file:manifestFile) {
                if (includeOsgiHeaders) {
                    // OSGi bundle headers
                    attribute(name:"Bundle-ManifestVersion",value:"2")
                    attribute(name:"Bundle-Name",value: grailsAppName)
                    attribute(name:"Bundle-SymbolicName",value: grailsAppName)
                    // note that the version must be a valid OSGi version, e.g. major.minor.micro.qualifier,
                    // where major, minor, and micro must be numbers and qualifier can be any string
                    // minor, micro and qualifier are optional
                    attribute(name:"Bundle-Version",value: metadata.getApplicationVersion())
                }
                // determine servlet and jsp versions
                def optionalPackage = "resolution:=optional"
                def servletVersion = ''
                def jspVersion = ''
                switch (grailsSettings.servletVersion) {
                    case '2.4': servletVersion='version="[2.4,3.0)"'; jspVersion = 'version="[2.0,3.0)"'; break
                    case '2.5': servletVersion='version="[2.5,3.0)"'; jspVersion = 'version="[2.1,3.0)"'; break
                    case '3.0': servletVersion='version="[3.0,4.0)"'; jspVersion = 'version="[2.2,3.0)"'; break
                }
                // imported packages
                def importedPackageList = [
                        "javax.servlet;$servletVersion",
                        "javax.servlet.http;$servletVersion",
                        "javax.servlet.resources;$servletVersion",
                        "javax.servlet.jsp;$jspVersion;$optionalPackage",
                        "javax.servlet.jsp.el;$jspVersion;$optionalPackage",
                        "javax.servlet.jsp.jstl;$jspVersion;$optionalPackage",
                        "javax.servlet.jsp.jstl.core;$jspVersion;$optionalPackage",
                        "javax.servlet.jsp.jstl.fmt;$jspVersion;$optionalPackage",
                        "javax.servlet.jsp.jstl.sql;$jspVersion;$optionalPackage",
                        "javax.servlet.jsp.jstl.tlv;$jspVersion;$optionalPackage",
                        "javax.servlet.jsp.tagext;$jspVersion;$optionalPackage",
                        "javax.servlet.jsp.resources;$jspVersion;$optionalPackage",
                        "javax.xml.parsers",
                        "org.w3c.dom",
                        "org.xml.sax",
                        "org.xml.sax.ext",
                        "org.xml.sax.helpers",
                ]
                attribute(name:"Import-Package", value: importedPackageList.join(','))
                // Webapp context, this is used as URL prefix
                attribute(name:"Webapp-Context",value: grailsAppName)

                // Grails sub-section
                section(name:"Grails Application") {
                    attribute(name:"Implementation-Title",value: grailsAppName)
                    attribute(name:"Implementation-Version",value: metadata.getApplicationVersion())
                    attribute(name:"Grails-Version",value: metadata.getGrailsVersion())
                }
            }
            ant.propertyfile(file:"${stagingDir}/WEB-INF/classes/application.properties") {
                entry(key:Environment.KEY, value:grailsEnv)
                entry(key:Metadata.WAR_DEPLOYED, value:"true")
                entry(key:BuildScope.KEY, value:"$buildScope")
                entry(key:Metadata.SERVLET_VERSION, value:grailsSettings.servletVersion)
            }

            ant.replace(file:"${stagingDir}/WEB-INF/applicationContext.xml", token:"classpath*:", value:"")

            if (buildConfig.grails.war.resources instanceof Closure) {
                Closure callable = buildConfig.grails.war.resources
                callable.delegate = ant
                callable.resolveStrategy = Closure.DELEGATE_FIRST

                if (callable.maximumNumberOfParameters == 1) {
                    callable(stagingDir)
                }
                else {
                    callable(stagingDir, additionalEventArgs)
                }
            }

            warPluginsInternal(compileScopePluginInfos)
            createDescriptorInternal(compileScopePluginInfos, resourceList)

            // update OSGi bundle classpath in MANIFEST.MF after event
            // handlers had a chance to modify included jars
            // add all jars in WEB-INF/lib
            def libDir = new File(stagingDir, "WEB-INF/lib")
            def classPathEntries = [ ".", "WEB-INF/classes" ]
            if (includeJars) {
                libDir.eachFileMatch(~/.*\.jar/) { classPathEntries << "WEB-INF/lib/${it.name}" }
            }
            ant.manifest(file:manifestFile, mode:'update') {
                attribute(name:"Bundle-ClassPath",value: classPathEntries.join(','))
            }

            eventListener.triggerEvent("CreateWarStart", warName, stagingDir)
            if (!buildExplodedWar) {
                def dir = new File(warName).parentFile
                if (!dir.exists()) ant.mkdir(dir:dir)
                ant.jar(destfile:warName, basedir:stagingDir, manifest:manifestFile)
            }

            eventListener.triggerEvent("CreateWarEnd", warName, stagingDir)
        }
        finally {
            if (!buildExplodedWar) cleanUpAfterWar()
        }

        if (buildExplodedWar) {
            eventListener.triggerEvent("StatusFinal", "Done creating Unpacked WAR at ${stagingDir}")
        }
        else {
            eventListener.triggerEvent("StatusFinal", "Done creating WAR ${makeRelative(warName)}")
        }
    }

    def cleanUpAfterWar() {
        eventListener.triggerEvent("CleanUpAfterWarStart")
        ant.delete(dir: stagingDir, failonerror:true)
        eventListener.triggerEvent("CleanUpAfterWarEnd")
    }

    @CompileStatic
    String configureWarName(String commandLineName = null) {
        if (warName) {
            return warName
        }

        def warFileDest = grailsSettings.projectWarFile.absolutePath

        if (warFileDest || commandLineName) {
            // Pick up the name of the WAR to create from the command-line
            // argument or the 'grails.project.war.file' configuration option.
            // The command-line argument takes precedence.
            warName = commandLineName ?: warFileDest

            // Find out whether WAR name is an absolute file path or a relative one.
            def warFile = new File(warName.toString())
            if (!warFile.absolute) {
                // It's a relative path, so adjust it for 'basedir'.
                warFile = new File(basedir, warFile.path)
                warName = warFile.canonicalPath
            }
        }
        else {
            def version = metadata.getApplicationVersion()
            if (version) {
                version = '-' + version
            }
            else {
                version = ''
            }
            warName = "${basedir}/$grailsAppName${version}.war"
        }
        return warName
    }

    void warPluginsInternal(List<GrailsPluginInfo> pluginInfos) {
        ant.sequential {
            for (GrailsPluginInfo info in pluginInfos) {
                warPluginForPluginInfo(info)
            }
        }
    }

    @CompileStatic
    void createDescriptor() {
        PluginBuildSettings ps = pluginSettings
        def pluginInfos = ps.supportedPluginInfos
        def compileScopePluginInfo = ps.compileScopePluginInfo
        def compileScopePluginInfos = compileScopePluginInfo.pluginInfos
        compileScopePluginInfos = compileScopePluginInfos.findAll { GrailsPluginInfo info -> pluginInfos.any { GrailsPluginInfo it -> it.name == info.name } }

        def resourceList = compileScopePluginInfo.artefactResources

        createDescriptorInternal(compileScopePluginInfos, resourceList)
    }

    protected void createDescriptorInternal(pluginInfos, resourceList) {

        new File(stagingDir, "WEB-INF/grails.xml").withWriter { writer ->
            def xml = new MarkupBuilder(writer)
            xml.grails {
                xml.resources {

                    def addedResources = new HashSet()

                    for (r in resourceList) {
                        def matcher = r.URL.toString() =~ artefactPattern

                        // Replace the slashes in the capture group with '.' so
                        // that we get a qualified class name. So for example,
                        // the file:
                        //
                        //    grails-app/domain/org/example/MyFilters.groovy
                        //
                        // will result in a capturing group of:
                        //
                        //    org/example/MyFilters
                        //
                        // which the following step will convert to:
                        //
                        //    org.example.MyFilters
                        //
                        def name = matcher[0][1].replaceAll('/', /\./)
                        if (name == 'spring.resources') {
                            name = 'resources'
                        }
                        name = name.toString()
                        if (!addedResources.contains(name)) {
                            xml.resource(name)
                            addedResources.add name
                        }
                    }
                }
                xml.plugins {
                    def addedPlugins = new HashSet()
                    for (GrailsPluginInfo info in pluginInfos) {
                        def name = info.descriptor.file.name - '.groovy'
                        name = name.toString()
                        if (!addedPlugins.contains(name)) {
                            xml.plugin(name)
                            addedPlugins.add name
                        }
                    }
                }
            }
        }
    }

    private void warPluginForPluginInfo(GrailsPluginInfo info) {
        def pluginBase = info.pluginDir.file
        ant.sequential {
            // Note that with in-place plugins, the name of the plugin's
            // directory may not match the "<name>-<version>" form that
            // should be used in the WAR file.

            // copy views and i18n to /WEB-INF/plugins/...
            def targetPluginDir = "${stagingDir}/WEB-INF/plugins/${info.name}-${info.version}"
            mkdir(dir: targetPluginDir)
            copy(todir: targetPluginDir, failonerror: true, preservelastmodified:true) {
                fileset(dir: pluginBase.absolutePath) {
                    include(name: "plugin.xml")
                    include(name: "grails-app/views/**")
                    exclude(name: "grails-app/**/*.groovy")
                }
                def pluginResources = new File("$resourcesDirPath/plugins/${info.name}-${info.version}")
                if (pluginResources.exists()) {
                    fileset(dir: pluginResources) {
                        include(name: "grails-app/**")
                        exclude(name: "grails-app/**/*.groovy")
                    }
                }
            }
        }

        // copy spring configs to /WEB-INF/spring/...
        def springDir = new File("${pluginBase.absolutePath}/grails-app/conf/spring")
        if (springDir.exists()) {
            ant.copy(todir: "${stagingDir}/WEB-INF/spring", failonerror: false, preservelastmodified:true) {
                fileset(dir: springDir, includes: "**/*.xml")
            }
        }

        // copy everything else from grails-app/conf to /WEB-INF/classes
        def confDir = new File(pluginBase.absolutePath, "grails-app/conf")
        def hibDir = new File(pluginBase.absolutePath, "grails-app/conf/hibernate")
        def javaDir = new File(pluginBase.absolutePath, "src/java")
        def groovyDir = new File(pluginBase.absolutePath, "src/groovy")
        if (confDir.exists() || hibDir.exists() || javaDir.exists() || groovyDir.exists()) {
            ant.copy(todir: "${stagingDir}/WEB-INF/classes", failonerror: false, preservelastmodified:true) {
                if (confDir.exists()) {
                    fileset(dir: confDir) {
                        exclude(name: "*.groovy")
                        exclude(name: "log4j.*")
                        exclude(name: "**/hibernate/**")
                        exclude(name: "**/spring/**")
                    }
                }

                if (hibDir.exists()) {
                    fileset(dir: hibDir, includes: "**/**")
                }

                if (javaDir.exists()) {
                    fileset(dir: javaDir) {
                        include(name: "**/**")
                        exclude(name: "**/*.java")
                    }
                }

                if (groovyDir.exists()) {
                    fileset(dir: groovyDir) {
                        include(name: "**/**")
                        exclude(name: "**/*.groovy")
                    }
                }
            }
        }
    }
}
