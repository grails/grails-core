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

appName = ""

Ant.property(environment:"env")   
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    
       
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
   try {
    	pluginClass = gcl.parseClass(pluginFile)   
        def plugin = pluginClass.newInstance()    
		def pluginName = GCU.getScriptName(GCU.getLogicalName(pluginClass, "GrailsPlugin"))
        def pluginZip = "${basedir}/grails-${pluginName}-${plugin.version}.zip"
		Ant.delete(file:pluginZip)
        Ant.zip(basedir:"${basedir}", destfile:pluginZip, 
				excludes:"plugins/**,**/WEB-INF/lib/**, **/WEB-INF/classes/**, **/WEB-INF/grails-app/**, **/WEB-INF/spring/**, **/WEB-INF/tld/**,**/WEB-INF/applicationContext.xml, **/WEB-INF/sitemesh.xml, **/WEB-INF/web*.xml")
   }
   catch(Throwable t) {
        event("StatusError", [ t.message])
        t.printStackTrace(System.out)
   }
}
