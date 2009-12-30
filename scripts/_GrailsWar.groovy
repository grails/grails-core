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

import grails.util.BuildScope
import grails.util.Environment
import grails.util.Metadata
import org.codehaus.groovy.grails.compiler.support.*
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
buildExplodedWar = getPropertyValue("grails.war.exploded", false).toBoolean()
warName = null


defaultWarDependencies = { antBuilder ->

    if (antBuilder) {
        delegate = antBuilder
        resolveStrategy = Closure.DELEGATE_FIRST
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

target(checkInPlacePlugins:"Perform a check whether inplace plugins have been packaged") {
    boolean force = argsMap.force || !isInteractive ?: false
    if(pluginSettings.inlinePluginDirectories) {
        println "WARNING: You have inplace plugins installed which require package-plugin to be run before a WAR is created."
        if (!force) {
            if (!confirmInput("Have you already run package-plugin in each plugin directory? [y/n]","confirm.inplace.packaging")) {
                println "Please run package-plugin in each inplace plugin directory before creating a WAR file"
                exit 1
            }
        }
    }

}
target (war: "The implementation target") {
    depends( parseArguments, configureRunningScript, checkInPlacePlugins, cleanWarFile, packageApp, compilegsp)

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
            fileset(dir:"${grailsSettings.sourceDir}/java") {
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
        	def pluginInfos = pluginSettings.supportedPluginInfos

            GrailsPluginManager pm = pluginManager
            pluginInfos = pluginInfos.findAll { info -> pm.supportsCurrentBuildScope(info.name) }


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

        String metaInfo = "$stagingDir/META-INF"
        ant.mkdir(dir:metaInfo)
        String manifestFile = "$metaInfo/MANIFEST.MF"
        ant.manifest(file:manifestFile) {
            section(name:"Grails Application") {
                attribute(name:"Implementation-Title",value:"${grailsAppName}")
                attribute(name:"Implementation-Version",value:"${metadata.getApplicationVersion()}")
                attribute(name:"Grails-Version",value:"${metadata.getGrailsVersion()}")
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
        if (!buildExplodedWar) {
            def warFile = new File(warName)
            def dir = warFile.parentFile
            if(!dir.exists()) ant.mkdir(dir:dir)
            ant.jar(destfile:warName, basedir:stagingDir, manifest:manifestFile)
        }
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
    def resourceList = pluginSettings.getArtefactResources()
    def pluginInfos = pluginSettings.getPluginInfos(pluginsHome)

    new File("${stagingDir}/WEB-INF/grails.xml").withWriter { writer ->
        def xml = new groovy.xml.MarkupBuilder(writer)
        xml.grails {
            xml.resources {
				
				def addedResources = new HashSet()
				
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
                    if(name == 'spring.resources')
                    	name='resources'
					name = name.toString()
					if(!addedResources.contains(name)) {
						xml.resource(name)
						addedResources.add name
					} else {
						println "\tWARNING: Duplicate resource '${name}', using the last one in compile order."
					}
                }
            }
            xml.plugins {

                GrailsPluginManager pm = pluginManager
				
				def addedPlugins = new HashSet()
                for(PluginInfo info in pluginInfos) {
                        boolean supportsScope = pm.supportsCurrentBuildScope(info.name)
                        if(supportsScope) {
                            def name = info.descriptor.file.name - '.groovy'
							name = name.toString()
							if(!addedPlugins.contains(name)) {
								xml.plugin(name)
								addedPlugins.add name
							}
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
        def pluginInfos = pluginSettings.supportedPluginInfos
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
                        include(name:"grails-app/views/**")
                        exclude(name:"grails-app/**/*.groovy")
                    }
                    def pluginResources = new File("$resourcesDirPath/plugins/${info.name}-${info.version}")
                    if(pluginResources.exists()) {
                        fileset(dir:pluginResources) {
                            include(name:"grails-app/**")
                            exclude(name:"grails-app/**/*.groovy")
                        }
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
    def warFileDest = grailsSettings.projectWarFile.absolutePath

    if(warFileDest || argsMap["params"]) {
        // Pick up the name of the WAR to create from the command-line
        // argument or the 'grails.war.destFile' configuration option.
        // The command-line argument takes precedence.
        warName = argsMap["params"] ? argsMap["params"][0] : warFileDest

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
