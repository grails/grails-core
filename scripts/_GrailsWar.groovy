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

import org.codehaus.groovy.grails.compiler.support.*
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import grails.util.BuildScope
import grails.util.Metadata
import grails.util.Environment
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginInfo
import org.codehaus.groovy.grails.plugins.GrailsPlugin

/**
 * Gant script that creates a WAR file from a Grails project
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsPackage")

generateLog4jFile = true
includeJars = true

warName = null

DEFAULT_DEPS = [
    "ant-*.jar",
    "ant-launcher-*.jar",
    "hibernate3-*.jar",
    "jdbc2_0-stdext.jar",
    "jta-*.jar",
    "groovy-all-*.jar",
    "standard-${servletVersion}.jar",
    "jstl-${servletVersion}.jar",
    "antlr-*.jar",
    "cglib-*.jar",
    "dom4j-*.jar",        
    "oscache-*.jar",
    "backport-util-concurrent*.jar",
    "ehcache-*.jar",
    "ivy-*.jar",
    "jsr107cache-*.jar",
    "sitemesh-*.jar",
    "org.springframework.*-*.jar",
    "jcl-over-slf4j-*.jar",
    "slf4j-api-*.jar",
    "slf4j-log4j12-*.jar",
    "log4j-*.jar",
    "ognl-*.jar",
    "hsqldb-*.jar",
    "commons-lang-*.jar",
    "commons-codec-*.jar",
    "commons-collections-*.jar",
    "commons-beanutils-*.jar",
    "commons-pool-*.jar",
    "commons-dbcp-*.jar",
    "commons-cli-*.jar",
    "commons-validator-*.jar",
    "commons-fileupload-*.jar",
    "commons-io-*.jar",
    "*oro-*.jar",
    "xercesImpl-*.jar",
    "xpp3_min-1.1.3.4.O.jar",
    "hibernate-annotations-*.jar",
    "hibernate-commons-annotations-*.jar",
    "ejb3-persistence-*.jar"
]

defaultWarDependencies = { antBuilder ->

    if (antBuilder) {
        delegate = antBuilder
        resolveStrategy = Closure.DELEGATE_FIRST
    }

    fileset(dir:"${basedir}/lib") {
        include(name:"*.jar")
    }

    // For backwards compatibility, we handle the list version of
    // "grails.war.dependencies" specially.
    if (buildConfig.grails.war.dependencies instanceof List) {
        fileset(dir:"${grailsHome}/dist") {
            include(name:"grails-*.jar")
            exclude(name:"grails-scripts-*.jar")
        }
        
        fileset(dir:"${grailsHome}/lib") {
            for(d in buildConfig.grails.war.dependencies) {
                include(name:d)
            }
        }
    }
    else {
        grailsSettings.runtimeDependencies.each {println it}
        grailsSettings.runtimeDependencies?.each { File f ->
            fileset(dir: f.parent, includes: f.name)
        }
    }
}

target (configureRunningScript:"Sets the currently running script, in case called directly") {
    System.setProperty('current.gant.script',"war")
}
target(startLogging:"Bootstraps logging") {
  // do nothing, overrides default behaviour so that logging doesn't kick in
}

target (war: "The implementation target") {
    depends( parseArguments, configureRunningScript, cleanWarFile, packageApp, compilegsp)

    includeJars = argsMap.nojars ? !argsMap.nojars : true
    stagingDir = grailsSettings.projectWarExplodedDir

    try {
        configureWarName()

        ant.mkdir(dir:stagingDir)

        ant.copy(todir:stagingDir, overwrite:true) {
            // Allow the application to override the step that copies
            // 'web-app' to the staging directory.
            if(buildConfig.grails.war.copyToWebApp instanceof Closure) {
                def callable = buildConfig.grails.war.copyToWebApp
                callable.delegate = ant
                callable.resolveStrategy = Closure.DELEGATE_FIRST
                callable(args)
            }
            else {
                fileset(dir:"${basedir}/web-app", includes:"**")
            }
        }
        // package plugin js/etc.
        packagePluginsForWar(stagingDir)
        
        ant.copy(todir:"${stagingDir}/WEB-INF/grails-app", overwrite:true) {
            fileset(dir:"${basedir}/grails-app", includes:"views/**")
            fileset(dir:"${resourcesDirPath}/grails-app", includes:"i18n/**")
        }
        ant.copy(todir:"${stagingDir}/WEB-INF/classes") {
            fileset(dir:classesDirPath) {
                exclude(name:"hibernate")
                exclude(name:"spring")
                exclude(name:"hibernate/*")
                exclude(name:"spring/*")
            }
        }

        ant.mkdir(dir:"${stagingDir}/WEB-INF/spring")
        ant.copy(todir:"${stagingDir}/WEB-INF/spring") {
            fileset(dir:"${basedir}/grails-app/conf/spring", includes:"**/*.xml")
        }

        ant.copy(todir:"${stagingDir}/WEB-INF/classes", failonerror:false) {
            fileset(dir:"${basedir}/grails-app/conf") {
                exclude(name:"*.groovy")
                exclude(name:"log4j.*")
                exclude(name:"**/hibernate/**")
                exclude(name:"**/spring/**")
            }
            fileset(dir:"${basedir}/grails-app/conf/hibernate", includes:"**/**")
            fileset(dir:"${basedir}/src/java") {
                include(name:"**/**")
                exclude(name:"**/*.java")
            }
            fileset(dir:"${resourcesDirPath}", includes:"log4j.properties")
        }

        scaffoldDir = "${stagingDir}/WEB-INF/templates/scaffolding"
        packageTemplates()

        // Copy the project's dependencies (JARs mainly) to the staging
        // area.
        if(includeJars) {
            ant.copy(todir:"${stagingDir}/WEB-INF/lib") {
                if(buildConfig.grails.war.dependencies instanceof Closure) {
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
        ant.copy(file:webXmlFile.absolutePath, tofile:"${stagingDir}/WEB-INF/web.xml", overwrite:true)
        ant.delete(file:webXmlFile)


        if(includeJars) {            
        	def pluginInfos = GrailsPluginUtils.getSupportedPluginInfos(pluginsHome)
        	if(pluginInfos) {
                ant.copy(todir:"${stagingDir}/WEB-INF/lib", flatten:true, failonerror:false) {
                    for(PluginInfo info in pluginInfos) {
                        fileset(dir: info.pluginDir.file.path) {
                            include(name:"lib/*.jar")
                        }
                    }
                }
            }
        }

        ant.propertyfile(file:"${stagingDir}/WEB-INF/classes/application.properties") {
            entry(key:Environment.KEY, value:grailsEnv)
            entry(key:Metadata.WAR_DEPLOYED, value:"true")
            entry(key:BuildScope.KEY, value:"$buildScope")
        }

        ant.replace(file:"${stagingDir}/WEB-INF/applicationContext.xml",
                    token:"classpath*:", value:"" )

        if(buildConfig.grails.war.resources instanceof Closure) {
            Closure callable = buildConfig.grails.war.resources
            callable.delegate = ant
            callable.resolveStrategy = Closure.DELEGATE_FIRST

            if(callable.maximumNumberOfParameters == 1) {
                callable(stagingDir)
            }
            else {
                callable(stagingDir, args)
            }
        }

        warPlugins()
        createDescriptor()
    	event("CreateWarStart", [warName, stagingDir])
        if (!buildExplodedWar) ant.jar(destfile:warName, basedir:stagingDir)
    	event("CreateWarEnd", [warName, stagingDir])
    }
    finally {
        if (!buildExplodedWar) cleanUpAfterWar()
    }

    if (buildExplodedWar) {
      event("StatusFinal", ["Done creating Unpacked WAR at ${stagingDir}"])
    }
    else {
      event("StatusFinal", ["Done creating WAR ${warName}"])
    }
}



target(createDescriptor:"Creates the WEB-INF/grails.xml file used to load Grails classes in WAR mode") {
    def resourceList = GrailsResourceLoaderHolder.resourceLoader.getResources()
    def pluginInfos = GrailsPluginUtils.getPluginInfos(pluginsHome)

    new File("${stagingDir}/WEB-INF/grails.xml").withWriter { writer ->
        def xml = new groovy.xml.MarkupBuilder(writer)
        xml.grails {
            xml.resources {
                for(r in resourceList) {
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
                    if(name == 'spring.resources') xml.resource("resources")
                    else xml.resource(name)
                }
            }
            xml.plugins {

                GrailsPluginManager pm = pluginManager

                for(PluginInfo info in pluginInfos) {
                        boolean supportsScope = pm.supportsCurrentBuildScope(info.name)
                        if(supportsScope) {
                            def name = info.descriptor.file.name - '.groovy'
                            xml.plugin(name)
                        }
                }
            }
        }
    }

}

target(cleanUpAfterWar:"Cleans up after performing a WAR") {
    ant.delete(dir:"${stagingDir}", failonerror:true)
}

target(warPlugins:"Includes the plugins in the WAR") {
    ant.sequential {
        def pluginInfos = GrailsPluginUtils.getSupportedPluginInfos(pluginsHome)
        if(pluginInfos) {
            for(PluginInfo info in pluginInfos) {
                def pluginBase = info.pluginDir.file

                // Note that with in-place plugins, the name of the plugin's
                // directory may not match the "<name>-<version>" form that
                // should be used in the WAR file.

                // copy views and i18n to /WEB-INF/plugins/...
                def targetPluginDir = "${stagingDir}/WEB-INF/plugins/${info.name}-${info.version}"
                mkdir(dir:targetPluginDir)
                copy(todir:targetPluginDir, failonerror:true) {
                    fileset(dir:pluginBase.absolutePath) {
                        include(name:"plugin.xml")
                        include(name:"grails-app/**")
                        exclude(name:"grails-app/**/*.groovy")
                    }
                }

                // copy spring configs to /WEB-INF/spring/...
                ant.copy(todir:"${stagingDir}/WEB-INF/spring", failonerror:false) {
                    fileset(dir:"${pluginBase.absolutePath}/grails-app/conf/spring", includes:"**/*.xml")
                }

                // copy everything else from grails-app/conf to /WEB-INF/classes
                def targetClassesDir = "${stagingDir}/WEB-INF/classes"
                ant.copy(todir:targetClassesDir, failonerror:false) {
                    fileset(dir:"${pluginBase.absolutePath}/grails-app/conf") {
                        exclude(name:"*.groovy")
                        exclude(name:"log4j.*")
                        exclude(name:"**/hibernate/**")
                        exclude(name:"**/spring/**")
                    }
                    fileset(dir:"${pluginBase.absolutePath}/grails-app/conf/hibernate", includes:"**/**")
                    fileset(dir:"${pluginBase.absolutePath}/src/java") {
                        include(name:"**/**")
                        exclude(name:"**/*.java")
                    }
                }
            }
        }
    }
}

target(configureWarName: "Configuring WAR name") {
    if(buildConfig.grails.war.destFile || argsMap["params"]) {
        // Pick up the name of the WAR to create from the command-line
        // argument or the 'grails.war.destFile' configuration option.
        // The command-line argument takes precedence.
        warName = argsMap["params"] ? argsMap["params"][0] : buildConfig.grails.war.destFile

        // Find out whether WAR name is an absolute file path or a
        // relative one.
        def warFile = new File(warName)
        if(!warFile.absolute) {
            // It's a relative path, so adjust it for 'basedir'.
            warFile = new File(basedir, warFile.path)
            warName = warFile.canonicalPath
        }
    }
    else {
        def fileName = grailsAppName
        def version = metadata.getApplicationVersion()
        if(version) {
            version = '-'+version
        }
        else {
            version = ''
        }
        warName = "${basedir}/${fileName}${version}.war"
    }
}
