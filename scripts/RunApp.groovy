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
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.cli.GrailsScriptRunner


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


shouldPackageTemplates=true




target ('default': "Run's a Grails application in Jetty") {
	depends( checkVersion, configureProxy, packageApp )
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
        event("StatusFinal", ["Server running. Browse to http://localhost:$serverPort$serverContextPath"])
    } catch(Throwable t) {
        t.printStackTrace()
        event("StatusFinal", ["Server failed to start: $t"])
    }
}
target( watchContext: "Watches the WEB-INF/classes directory for changes and restarts the server if necessary") {
    long lastModified = classesDir.lastModified()
    boolean keepRunning = true
    boolean isInteractive = System.getProperty("grails.interactive.mode") == "true"

    if(isInteractive) {
        def daemonThread = new Thread( {
            println "--------------------------------------------------------"
            println "Application loaded in interactive mode, type 'exit' to shutdown server or your command name in to continue (hit ENTER to run the last command):"            

            def reader = new BufferedReader(new InputStreamReader(System.in))
            def cmd = reader.readLine()
            def scriptName
            while(cmd!=null) {
                if(cmd == 'exit' || cmd == 'quit') break
                if(cmd != 'run-app') {
                    scriptName = cmd ? GrailsScriptRunner.processArgumentsAndReturnScriptName(cmd) : scriptName
                    if(scriptName) {
                        def now = System.currentTimeMillis()
                        GrailsScriptRunner.callPluginOrGrailsScript(scriptName)
                        def end = System.currentTimeMillis()
                        println "--------------------------------------------------------"
                        println "Command [$scriptName] completed in ${end-now}ms"
                    }
                }
                else {
                    println "Cannot run the 'run-app' command. Server already running!"
                }
                try {
                    println "--------------------------------------------------------"
                    println "Application loaded in interactive mode, type 'exit' to shutdown server or your command name in to continue (hit ENTER to run the last command):"

                    cmd = reader.readLine()
                } catch (IOException e) {
                    cmd = ""
                }
            }

            println "Stopping Grails server..."
            grailsServer.stop()
            keepRunning = false

        })
        daemonThread.daemon = true
        daemonThread.run()
    }
    
    while(keepRunning) {
        if (autoRecompile) {
            lastModified = recompileCheck(lastModified) {
                try {
                    grailsServer.stop()
                    compile()
                    Thread currentThread = Thread.currentThread()
                    ClassLoader contextLoader = currentThread.getContextClassLoader()
                    classLoader = new URLClassLoader([classesDir.toURI().toURL()] as URL[], contextLoader)
                    currentThread.setContextClassLoader classLoader
                    PluginManagerHolder.pluginManager = null
                    // reload plugins
                    loadPlugins()
                    setupWebContext()
                    grailsServer.setHandler( webContext )
                    grailsServer.start()
                } catch (Throwable e) {
                    GrailsUtil.sanitizeStackTrace(e)
                    e.printStackTrace()
                }
            }
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
    webContext = new WebAppContext("${basedir}/web-app", serverContextPath)
    def configurations = [org.mortbay.jetty.webapp.WebInfConfiguration, 
                          org.mortbay.jetty.plus.webapp.Configuration, 
                          org.mortbay.jetty.webapp.JettyWebXmlConfiguration, 
                          org.mortbay.jetty.webapp.TagLibConfiguration]*.newInstance() 
    def jndiConfig = new org.mortbay.jetty.plus.webapp.EnvConfiguration()						
	if(config.grails.development.jetty.env) {
		def res = resolveResources(config.grails.development.jetty.env)
		if(res) {
			jndiConfig.setJettyEnvXml(res[0].URL)			
		}
	}
	configurations.add(1,jndiConfig)
    webContext.configurations = configurations
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
