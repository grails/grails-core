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
 * Gant script that executes Grails using an embedded Jetty server
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.mortbay.jetty.*
import org.mortbay.jetty.nio.*
import org.mortbay.jetty.handler.*
import org.mortbay.jetty.webapp.*
import org.mortbay.jetty.plus.naming.*
import javax.naming.*

import org.codehaus.groovy.tools.RootLoader


Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"
grailsServer = null
grailsContext = null 
autoRecompile = System.getProperty("disable.auto.recompile") ? !(System.getProperty("disable.auto.recompile").toBoolean()) : true

// How often should recompilation occur while the application is running (in seconds)?
// Defaults to 3s.
recompileFrequency = System.getProperty("recompile.frequency")
recompileFrequency = recompileFrequency ? recompileFrequency.toInteger() : 3


includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/PackagePlugins.groovy" )

shouldPackageTemplates=true


// Checks whether the project's sources have changed since the last
// compilation, and then performs a recompilation if this is the case.
// Returns the updated 'lastModified' value.
recompileCheck = { lastModified ->
    try {
        def ant = new AntBuilder()
        ant.taskdef ( 	name : 'groovyc' ,
                        classname : 'org.codehaus.groovy.grails.compiler.GrailsCompiler' )
        def grailsDir = resolveResources("file:${basedir}/grails-app/*")
        def pluginLibs = resolveResources("file:${basedir}/plugins/*/lib")
        ant.path(id:"grails.classpath",grailsClasspath.curry(pluginLibs, grailsDir))

        ant.groovyc(destdir:classesDirPath,
                    classpathref:"grails.classpath",
                    resourcePattern:"file:${basedir}/**/grails-app/**/*.groovy",
                    projectName:baseName) {
                    src(path:"${basedir}/src/groovy")
                    src(path:"${basedir}/grails-app/domain")
                    src(path:"${basedir}/src/java")
                    javac(classpathref:"grails.classpath", debug:"yes")

                }
        ant = null
    }
    catch(Exception e) {
        compilationError = true
        event("StatusUpdate", ["Error automatically restarting container: ${e.message}"])
        e.printStackTrace()

    }

    def tmp = classesDir.lastModified()
    if(lastModified < tmp) {

        // run another compile JIT
        try {
            grailsServer.stop()
            compile()
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader()
            classLoader = new URLClassLoader([classesDir.toURL()] as URL[], contextLoader)
            // reload plugins
            loadPlugins()
            setupWebContext()
            grailsServer.setHandler( webContext )
            grailsServer.start()
        }
        catch(Exception e) {
            event("StatusUpdate", ["Error automatically restarting container: ${e.message}"])
            e.printStackTrace()
        }

        finally {
           lastModified = classesDir.lastModified()
        }
    }

    return lastModified
}

target ('default': "Run's a Grails application in Jetty") {
	depends( checkVersion, configureProxy, packagePlugins, packageApp )
	runApp()
	watchContext()
}
target ( runApp : "Main implementation that executes a Grails application") {
	System.setProperty('org.mortbay.xml.XmlParser.NotValidating', 'true')
    try {
		println "Running Grails application.."
        def server = configureHttpServer()
        profile("start server") {            
            server.start()
        }
        event("StatusFinal", ["Server running. Browse to http://localhost:$serverPort/$grailsAppName"])
    } catch(Throwable t) {
        t.printStackTrace()
        event("StatusFinal", ["Server failed to start: $t"])
    }
}
target( watchContext: "Watches the WEB-INF/classes directory for changes and restarts the server if necessary") {
    long lastModified = classesDir.lastModified()
    while(true) {
        if (autoRecompile) {
            lastModified = recompileCheck(lastModified)
        }
        sleep(recompileFrequency * 1000)
    }
}

target( configureHttpServer : "Returns a jetty server configured with an HTTP connector") {
    def server = new Server()
    grailsServer = server
    def connectors = [new SelectChannelConnector()]
    connectors[0].setPort(serverPort)
    server.setConnectors( (Connector [])connectors )
	setupWebContext()
    server.setHandler( webContext )
    event("ConfigureJetty", [server])
    return server
}

target( setupWebContext: "Sets up the Jetty web context") {
    webContext = new WebAppContext("${basedir}/web-app", "/${grailsAppName}")
    def confClassList = ["org.mortbay.jetty.webapp.WebInfConfiguration", 
                         "org.mortbay.jetty.plus.webapp.EnvConfiguration", 
                         "org.mortbay.jetty.plus.webapp.Configuration", 
                         "org.mortbay.jetty.webapp.JettyWebXmlConfiguration", 
                         "org.mortbay.jetty.webapp.TagLibConfiguration"] 
    webContext.setConfigurationClasses((String[])confClassList ) 
    webContext.setDefaultsDescriptor("${grailsHome}/conf/webdefault.xml")
    webContext.setClassLoader(classLoader)
    webContext.setDescriptor(webXmlFile.absolutePath)	
}

target( stopServer : "Stops the Grails Jetty server") {
	if(grailsServer) {
		grailsServer.stop()		
	}	
    event("StatusFinal", ["Server stopped"])
}