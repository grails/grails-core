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
 * Gant script that handles the creation of Grails plugins
 * 
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 *
 * @since 0.4
 */
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU  

appName = ""

Ant.property(environment:"env")   
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/CreateApp.groovy" )

target ( "default" : "Creates a Grails plug-in project, including the necessary directory structure and commons files") {
   createPlugin()
}     

target( createPlugin: "The implementation target")  {
	depends( appName, createStructure, updateAppProperties, copyBasics, createIDESupportFiles )

    Ant.copy(file:"${grailsHome}/src/grails/grails-app/conf/UrlMappings.groovy", todir:"${basedir}/grails-app/conf")
    Ant.copy(file:"${grailsHome}/src/grails/grails-app/conf/DataSource.groovy", todir:"${basedir}/grails-app/conf")
    
    pluginName = GCU.getNameFromScript(grailsAppName)
 	new File("${basedir}/${pluginName}GrailsPlugin.groovy") <<
"""\
class ${pluginName}GrailsPlugin {
    def version = 0.1
    def dependsOn = [:]

    // TODO Fill in these fields
    def author = "Your name"
    def authorEmail = ""
    def title = "Plugin summary/headline"
    def description = '''\\
Brief description of the plugin.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/${pluginName}+Plugin"

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }
   
    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)		
    }

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional)
    }
	                                      
    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }
	
    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
"""
    new File("${basedir}/scripts/_Install.groovy") <<
"""\
//
// This script is executed by Grails after plugin was installed to project.
// This script is a Gant script so you can use all special variables provided
// by Gant (such as 'baseDir' which points on project base dir). You can
// use 'Ant' to access a global instance of AntBuilder
//
// For example you can create directory under project tree:
// Ant.mkdir(dir:"${basedir}/grails-app/jobs")
//

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

"""
    new File("${basedir}/scripts/_Upgrade.groovy") <<
"""\
//
// This script is executed by Grails during application upgrade ('grails upgrade' command).
// This script is a Gant script so you can use all special variables
// provided by Gant (such as 'baseDir' which points on project base dir).
// You can use 'Ant' to access a global instance of AntBuilder
//
// For example you can create directory under project tree:
// Ant.mkdir(dir:"${basedir}/grails-app/jobs")
//

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

"""
    event("StatusFinal", [ "Created plugin ${pluginName}"])
}