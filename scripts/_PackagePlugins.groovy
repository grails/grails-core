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

import org.codehaus.groovy.grails.plugins.PluginInfo

/**
 * Gant script that handles the packaging of Grails plug-ins
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

packageFiles = { String from ->
    def targetPath = grailsSettings.resourcesDir.path
    def dir = new File(from, "grails-app/conf")
    if (dir.exists()) {
	    ant.copy(todir:targetPath, failonerror:false) {
            fileset(dir:dir.path) {
                exclude(name:"**/*.groovy")
                exclude(name:"**/log4j*")
                exclude(name:"hibernate/**/*")
                exclude(name:"spring/**/*")
            }
        }
    }

    dir = new File(dir, "hibernate")
    if (dir.exists()) {
	    ant.copy(todir:targetPath, failonerror:false) {
		    fileset(dir:dir.path, includes:"**/*")
        }
    }

    dir = new File(from, "src/groovy")
    if (dir.exists()) {
	    ant.copy(todir:targetPath, failonerror:false) {
            fileset(dir:dir.path) {
                exclude(name:"**/*.groovy")
                exclude(name:"**/*.java")
            }
        }
    }

    dir = new File(from, "src/java")
    if (dir.exists()) {
	    ant.copy(todir:targetPath, failonerror:false) {
            fileset(dir:dir.path) {
                exclude(name:"**/*.java")
            }
        }
	}
}

target( packagePlugins : "Packages any Grails plugins that are installed for this project") {
	depends( classpath, resolveDependencies )
    def pluginInfos = pluginSettings.getPluginInfos(pluginsHome)
    for(PluginInfo info in pluginInfos) {
        try {
            def pluginBase = info.pluginDir.file
            packageFiles(pluginBase.path)
        }
        catch(Exception e) {
            e.printStackTrace(System.out)
            println "Error packaging plugin [${info.name}] : ${e.message}"
        }

    }
}

packagePluginsForWar = { targetDir ->
    def pluginInfos = pluginSettings.getPluginInfos(pluginsHome)
    for(PluginInfo info in pluginInfos) {
        try {

            def pluginBase = info.pluginDir.file
            def pluginPath = pluginBase.absolutePath
            def pluginName = "${info.name}-${info.version}"

            packageFiles(pluginBase.path)
            if(new File("${pluginPath}/web-app").exists()) {
                ant.mkdir(dir:"${targetDir}/plugins/${pluginName}")
                ant.copy(todir:"${targetDir}/plugins/${pluginName}") {
                    fileset(dir:"${pluginBase}/web-app", includes:"**", excludes:"**/WEB-INF/**, **/META-INF/**")
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace(System.out)
            println "Error packaging plugin [${info.name}] : ${e.message}"
        }

    }
}
