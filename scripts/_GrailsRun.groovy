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
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.cli.GrailsScriptRunner
import grails.util.GrailsUtil

import grails.web.container.EmbeddableServerFactory
import grails.web.container.EmbeddableServer
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils


/**
 * Gant script that executes Grails using an embedded Jetty server
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

includeTargets << grailsScript("_GrailsPlugins")

SCHEME_HTTP="http"
SCHEME_HTTPS="https"



grailsServer = null
grailsContext = null
autoRecompile = System.getProperty("disable.auto.recompile") ? !(System.getProperty("disable.auto.recompile").toBoolean()) : true

// How often should recompilation occur while the application is running (in seconds)?
// Defaults to 3s.
recompileFrequency = System.getProperty("recompile.frequency")
recompileFrequency = recompileFrequency ? recompileFrequency.toInteger() : 3

shouldPackageTemplates = true

// This isn't used within this script but may come in handy for scripts
// that depend on this one.
ant.path(id: "grails.runtime.classpath", runtimeClasspath)

/**
 * Runs the application in dev mode, i.e. with class-reloading.
 */
target(runApp: "Main implementation that executes a Grails application") {
    runInline(SCHEME_HTTP, serverHost, serverPort, serverPortHttps)
}

/**
 * Runs the application in dev mode over HTTPS.
 */
target(runAppHttps: "Main implementation that executes a Grails application with an HTTPS listener") {
    runInline(SCHEME_HTTPS, serverHost, serverPort, serverPortHttps)
}

/**
 * Runs the application using the WAR file directly.
 */
target (runWar : "Main implementation that executes a Grails application WAR") {
    runWar(SCHEME_HTTP, serverHost, serverPort, serverPortHttps)
}


/**
 * Runs the application over HTTPS using the WAR file directly.
 */
target (runWarHttps : "Main implementation that executes a Grails application WAR") {
    runWar(SCHEME_HTTPS, serverHost, serverPort, serverPortHttps)
}

private EmbeddableServerFactory loadServerFactory() {
    def load = { name -> classLoader.loadClass(name).newInstance() }


    String defaultServer = "org.grails.tomcat.TomcatServerFactory"
    def containerClass = getPropertyValue("grails.server.factory", defaultServer)
    EmbeddableServerFactory serverFactory
    try {
        serverFactory = load(containerClass)
    }
    catch(ClassNotFoundException cnfe) {
        if(containerClass==defaultServer) {
            println "WARNING: No default container found, installing Tomcat.."
            doInstallPluginFromGrailsHomeOrRepository "tomcat", GrailsUtil.grailsVersion
            GrailsPluginUtils.clearCaches()
            compilePlugins()
            serverFactory = load(containerClass)            
        }
    }
    catch (Throwable e) {
        GrailsUtil.deepSanitize(e)
        e.printStackTrace()
        event("StatusFinal", ["Failed to load container [$containerClass]: ${e.message}"])
        exit(1)
    }
    return serverFactory
}

private runInline(scheme, host, httpPort, httpsPort) {
    EmbeddableServerFactory serverFactory = loadServerFactory()
    grailsServer = serverFactory.createInline("${basedir}/web-app", webXmlFile.absolutePath, serverContextPath, classLoader)
    runServer server: grailsServer, host:host, httpPort: httpPort, httpsPort: httpsPort, scheme:scheme
    startPluginScanner()
}

private runWar(scheme, host, httpPort, httpsPort) {
    EmbeddableServerFactory serverFactory = loadServerFactory()
    grailsServer = serverFactory.createForWAR(warName, serverContextPath)
    if(serverFactory.class.name.contains("Jetty")) {
         event("ConfigureJetty", [grailsServer.grailsServer])
    }

    runServer server:grailsServer, host:host, httpPort:httpPort, httpsPort: httpsPort, scheme: scheme

}


/**
 * Runs the Server. You can pass these named arguments:
 *
 *   server - The server instance to use (required).
 *   port - The network port the server is running on (used to display the URL) (required).
 *   scheme - The network scheme to display in the URL (optional; defaults to "http").
 */
runServer = { Map args ->
    try {
        println "Running Grails application.."
        def message = "Server running. Browse to http://${args.host ?: 'localhost'}:${args.httpPort}$serverContextPath"

        EmbeddableServer server = args["server"]
        if(server.class.name.contains("Jetty")) {
             server.eventListener = this
        }

        profile("start server") {
            if(args.scheme == 'https') {
                server.startSecure args.host, args.httpPort, args.httpsPort

                // Update the message to reflect the fact we are running HTTPS as well.
                message += " or https://${args.host ?: 'localhost'}:${args.httpsPort}$serverContextPath"
            }
            else {
                server.start args.host, args.httpPort
            }
        }
        event("StatusFinal", [message])
    } catch (Throwable t) {
        GrailsUtil.deepSanitize(t)
        if (!(t.class instanceof java.net.SocketException) && !(t.cause?.class instanceof java.net.SocketException))
            t.printStackTrace()
        event("StatusFinal", ["Server failed to start: $t"])
        exit(1)
    }
}

/**
 * Starts the plugin scanner. Call this after starting the server if you
 * want changes to artifacts automatically detected and loaded.
 */
target(startPluginScanner: "Starts the plugin manager's scanner that detects changes to artifacts.") {
    // Start the plugin change scanner.
    PluginManagerHolder.pluginManager.startPluginChangeScanner()
}

/**
 * Keeps the server alive and checks for changes in domain classes or
 * source files under "src". If any changes are detected, the servlet
 * container is restarted.
 */
target(watchContext: "Watches the WEB-INF/classes directory for changes and restarts the server if necessary") {
    depends(classpath)

    long lastModified = classesDir.lastModified()
    boolean keepRunning = true
    boolean isInteractive = System.getProperty("grails.interactive.mode") == "true"

    if (isInteractive) {
        def daemonThread = new Thread({
            println "--------------------------------------------------------"
            println "Application loaded in interactive mode, type 'exit' to shutdown server or your command name in to continue (hit ENTER to run the last command):"

            def reader = new BufferedReader(new InputStreamReader(System.in))
            def cmd = reader.readLine()
            def scriptName
            while (cmd != null) {
                if (cmd == 'exit' || cmd == 'quit') break
                if (cmd != 'run-app') {
                    scriptName = cmd ? GrailsScriptRunner.processArgumentsAndReturnScriptName(cmd) : scriptName
                    if (scriptName) {
                        def now = System.currentTimeMillis()
                        GrailsScriptRunner.callPluginOrGrailsScript(scriptName)
                        def end = System.currentTimeMillis()
                        println "--------------------------------------------------------"
                        println "Command [$scriptName] completed in ${end - now}ms"
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
            PluginManagerHolder.pluginManager.stopPluginChangeScanner()
            keepRunning = false

        })
        daemonThread.daemon = true
        daemonThread.run()
    }

    def killFile = new File("${basedir}/.kill-run-app")
    while (keepRunning) {
        if (autoRecompile) {
            lastModified = recompileCheck(lastModified) {
                try {
                    grailsServer.stop()
                    compile()
                    Thread currentThread = Thread.currentThread()
                    classLoader = new URLClassLoader([classesDir.toURI().toURL()] as URL[], rootLoader)
                    currentThread.setContextClassLoader classLoader
                    PluginManagerHolder.pluginManager = null
                    // reload plugins
                    loadPlugins()
                    runApp()
                } catch (Throwable e) {
                    logError("Error restarting container",e)
                    exit(1)
                }
            }
        }
        sleep(recompileFrequency * 1000)

        // Check whether the kill file exists. This is a hack for the
        // functional tests so that we can stop the servers that are
        // started.
        if (killFile.exists()) {
            println "Stopping Jetty server..."
            grailsServer.stop()
            killFile.delete()
            keepRunning = false
        }
    }
}

target(keepServerAlive: "Idles the script, ensuring that the server stays running.") {
    def keepRunning = true
    def killFile = new File("${basedir}/.kill-run-app")
    while (keepRunning) {
        sleep(recompileFrequency * 1000)

        // Check whether the kill file exists. This is a hack for the
        // functional tests so that we can stop the servers that are
        // started.
        if (killFile.exists()) {
            println "Stopping Jetty server..."
            grailsServer.stop()
            killFile.delete()
            keepRunning = false
        }
    }
}

target(stopServer: "Stops the Grails Jetty server") {
    if (grailsServer) {
        grailsServer.stop()
    }
    event("StatusFinal", ["Server stopped"])
}



