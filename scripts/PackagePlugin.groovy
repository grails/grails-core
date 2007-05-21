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
 *
 * @since 0.4
 */
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU  
import org.codehaus.groovy.control.*
import groovy.xml.MarkupBuilder


appName = ""

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"

pluginIncludes = [
	"application.properties",
	"*GrailsPlugin.groovy",
    "plugin.xml",
	"grails-app/**",
	"lib/**",
	"web-app/**",
	"src/**",
]

pluginExcludes = [
	"web-app/WEB-INF/**",
	"grails-app/conf/*DataSource.groovy",
	"grails-app/conf/log4j.*.properties",
	"**/.svn/**",
	"**/CVS/**"
]

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/Compile.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/CreateApp.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

task ( "default" : "Packages a Grails plugin into a zip for distribution") {
   depends(packageApp)
   packagePlugin()
}

task(packagePlugin:"Implementation task") {
    depends (compile)

    def pluginFile
    new File("${basedir}").eachFile {
        if(it.name.endsWith("GrailsPlugin.groovy")) {
            pluginFile = it
        }
    }

    if(!pluginFile) Ant.fail("Plugin file not found for plugin project")

    def cl = Thread.currentThread().getContextClassLoader()
    def compConfig = new CompilerConfiguration()
    compConfig.setClasspath("${basedir}/web-app/WEB-INF/classes");

    def gcl = new GroovyClassLoader(cl,compConfig,true)

    Class pluginClass
    def plugin
    try {
        pluginClass = gcl.parseClass(pluginFile)
        plugin = pluginClass.newInstance()
    }
    catch(Throwable t) {
        event("StatusError", [ t.message])
        t.printStackTrace(System.out)
        Ant.fail("Cannot instantiate plugin file")
    }
    def pluginName = GCU.getScriptName(GCU.getLogicalName(pluginClass, "GrailsPlugin"))

    // Generate plugin.xml descriptor from info in *GrailsPlugin.groovy
    new File("${basedir}/plugin.xml").delete()
    def writer = new IndentPrinter( new PrintWriter( new FileWriter("${basedir}/plugin.xml")))
    def xml = new MarkupBuilder(writer)
    def props = ['author','authorEmail','title','description','documentation']
    xml.plugin(name:"${pluginName}",version:"${plugin.version}") {
        props.each {
            if( plugin.properties[it] ) "${it}"(plugin.properties[it])
        }
    }

    // Package plugin's zip distribution
    def pluginZip = "${basedir}/grails-${pluginName}-${plugin.version}.zip"
    Ant.delete(file:pluginZip)
    def includesList = pluginIncludes.join(",")
	def excludesList = pluginExcludes.join(",")
    Ant.zip(basedir:"${basedir}", destfile:pluginZip, includes:includesList, excludes:excludesList)
}
