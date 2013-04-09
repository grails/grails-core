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

import org.codehaus.groovy.grails.project.packaging.*

/**
 * Gant script that creates a WAR file from a Grails project
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsPackage")

includeJars = true
buildExplodedWar = getPropertyValue("grails.war.exploded", false).toBoolean()
warCreator = new GrailsProjectWarCreator(grailsSettings, eventListener, projectPackager, ant, isInteractive)

defaultWarDependencies = warCreator.defaultWarDependencies

target (configureRunningScript: "Sets the currently running script, in case called directly") {
    System.setProperty('current.gant.script', "war")
}

target (war: "The implementation target") {
    depends(parseArguments, configureRunningScript, cleanWarFile, packageApp, configureWarName)
    warCreator.includeJars = argsMap.nojars ? false : true
    warCreator.packageWar()
}

target(createDescriptor:"Creates the WEB-INF/grails.xml file used to load Grails classes in WAR mode") {
    warCreator.createDescriptor()
}

target(cleanUpAfterWar:"Cleans up after performing a WAR") {
    ant.delete(dir:"${stagingDir}", failonerror:true)
}

target(warPlugins:"Includes the plugins in the WAR") {
    def pluginInfos = pluginSettings.supportedPluginInfos
    warCreator.warPluginsInternal(pluginInfos)
}

target(configureWarName: "Configuring WAR name") {
    warCreator.configureWarName(argsMap.params ? argsMap.params[0] : null)
}
