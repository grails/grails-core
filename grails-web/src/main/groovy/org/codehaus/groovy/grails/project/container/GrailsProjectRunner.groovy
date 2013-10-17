/*
 * Copyright 2012 SpringSource
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
package org.codehaus.groovy.grails.project.container

import grails.build.logging.GrailsConsole
import grails.util.Metadata
import grails.web.container.EmbeddableServer
import grails.web.container.EmbeddableServerFactory
import groovy.transform.CompileStatic

import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import org.codehaus.groovy.grails.cli.ScriptExitException
import org.codehaus.groovy.grails.cli.api.BaseSettingsApi
import org.codehaus.groovy.grails.cli.support.BuildSettingsAware
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import org.codehaus.groovy.grails.project.packaging.GrailsProjectPackager
import org.codehaus.groovy.grails.project.packaging.GrailsProjectWarCreator

/**
 * Runs the container embedded within the current JVM.
 *
 * @author Graeme Rocher
 * @since 2.2
 */
class GrailsProjectRunner extends BaseSettingsApi {

    public static final String SCHEME_HTTP = "http"
    public static final String SCHEME_HTTPS = "https"

    private GrailsProjectPackager projectPackager
    private GrailsProjectWarCreator warCreator
    private String serverContextPath

    private GrailsConsole grailsConsole = GrailsConsole.getInstance()
    private ClassLoader classLoader
    private GrailsBuildEventListener eventListener
    private String basedir
    private File webXmlFile
    private EmbeddableServer grailsServer
    private String warName
    boolean usingSecureServer = false
    private ConfigObject config

    GrailsProjectRunner(GrailsProjectPackager projectPackager, GrailsProjectWarCreator warCreator, ClassLoader classLoader) {
        super(projectPackager.buildSettings, warCreator.eventListener, false)
        initialize(projectPackager, warCreator, classLoader)
    }

    GrailsProjectPackager getProjectPackager() {
        return projectPackager
    }

    GrailsProjectWarCreator getWarCreator() {
        return warCreator
    }

    String getServerContextPath() {
        return serverContextPath
    }

    /**
     * @return Whether the server is running
     */
    boolean isServerRunning() {
        ServerSocket serverSocket = null
        try {
            serverSocket = new ServerSocket(serverPort)
            return false
        } catch (e) {
            return true
        }
        finally {
            try {
                serverSocket?.close()
            } catch (Throwable e) {
            }
        }
    }

    @CompileStatic
    private void initialize(GrailsProjectPackager projectPackager, GrailsProjectWarCreator warCreator, ClassLoader classLoader) {
        this.projectPackager = projectPackager
        this.warCreator = warCreator
        this.eventListener = warCreator.eventListener
        this.classLoader = classLoader
        webXmlFile = buildSettings.webXmlLocation
        basedir = buildSettings.baseDir.absolutePath
        warName = warCreator.configureWarName()
    }

    /**
     * Runs the application in dev mode, i.e. with class-reloading.
     */
    @CompileStatic
    EmbeddableServer runApp() {
        runInline(SCHEME_HTTP, serverHost, serverPort, serverPortHttps)
    }

    /**
     * Runs the application in dev mode over HTTPS.
     */
    @CompileStatic
    EmbeddableServer runAppHttps() {
        runInline(SCHEME_HTTPS, serverHost, serverPort, serverPortHttps)
    }

    /**
     * Runs the application using the WAR file directly.
     */
    @CompileStatic
    EmbeddableServer runWar() {
        runWarInternal(SCHEME_HTTP, serverHost, serverPort, serverPortHttps)
    }

    /**
     * Runs the application over HTTPS using the WAR file directly.
     */
    @CompileStatic
    EmbeddableServer runWarHttps() {
        runWarInternal(SCHEME_HTTPS, serverHost, serverPort, serverPortHttps)
    }

    @CompileStatic
    private EmbeddableServerFactory loadServerFactory() {
        serverContextPath = projectPackager.configureServerContextPath()
        config = projectPackager.createConfig()

        def load = { String name -> classLoader.loadClass(name).newInstance() }

        String defaultServer = "org.grails.plugins.tomcat.TomcatServerFactory"
        def containerClass = getPropertyValue("grails.server.factory", defaultServer)
        EmbeddableServerFactory serverFactory
        try {
            serverFactory = createServerFactory(load, containerClass, serverFactory)
        }
        catch (ClassNotFoundException cnfe) {
            if (containerClass == defaultServer) {
                grailsConsole.error "No default container found. Please install a container plugin such as 'tomcat' first."
                exit 1
            }
        }
        catch (Throwable e) {
            grailsConsole.error "Failed to load container [$containerClass]: ${e.message}", e
            exit(1)
        }
        return serverFactory
    }

    private EmbeddableServerFactory createServerFactory(Closure<Object> load, containerClass, EmbeddableServerFactory serverFactory) {
        serverFactory = load(containerClass.toString())
        if (serverFactory instanceof BuildSettingsAware) {
            ((BuildSettingsAware) serverFactory).buildSettings = buildSettings
        }
        serverFactory
    }

    @CompileStatic
    private EmbeddableServer runInline(scheme, host, httpPort, httpsPort) {
        EmbeddableServerFactory serverFactory = loadServerFactory()
        grailsServer = serverFactory.createInline("${basedir}/web-app", webXmlFile.absolutePath, serverContextPath, classLoader)
        runServer server: grailsServer, host:host, httpPort: httpPort, httpsPort: httpsPort, scheme:scheme
    }

    @CompileStatic
    private EmbeddableServer runWarInternal(scheme, host, httpPort, httpsPort) {
        EmbeddableServerFactory serverFactory = loadServerFactory()
        grailsServer = serverFactory.createForWAR(warName, serverContextPath)

        Metadata.getCurrent().put(Metadata.WAR_DEPLOYED, "true")
        runServer server:grailsServer, host:host, httpPort:httpPort, httpsPort: httpsPort, scheme: scheme
    }

    /**
     * Runs the Server. You can pass these named arguments:
     *
     *   server - The server instance to use (required).
     *   port - The network port the server is running on (used to display the URL) (required).
     *   scheme - The network scheme to display in the URL (optional; defaults to "http").
     */
    EmbeddableServer runServer(Map args) {
        try {
            eventListener.triggerEvent("StatusUpdate","Running Grails application")


            EmbeddableServer server = args["server"]
            if (server.hasProperty('eventListener')) {
                server.eventListener = buildEventListener
            }
            if (server.hasProperty('grailsConfig')) {
                server.grailsConfig = config
            }

            def httpsMessage = ""
            profile("start server") {

                try { new ServerSocket(args.httpPort).close() }
                catch (IOException e) {
                    grailsConsole.error("Server failed to start for port $args.httpPort: $e.message", e)
                    exit(1)
                }

                if (args.scheme == 'https') {

                    try { new ServerSocket(args.httpsPort).close() }
                    catch (IOException e) {
                        grailsConsole.error("Server failed to start for port $args.httpsPort: $e.message", e)
                        exit(1)
                    }

                    usingSecureServer = true
                    server.startSecure args.host, args.httpPort, args.httpsPort

                    // Update the message to reflect the fact we are running HTTPS as well.
                    setServerPortHttps(server.localHttpsPort)
                    httpsMessage = " or https://${args.host ?: 'localhost'}:${server.localHttpsPort}$serverContextPath"
                }
                else {
                    server.start args.host, args.httpPort
                }
            }
            setServerPort(server.localHttpPort)
            def message = "Server running. Browse to http://${args.host ?: 'localhost'}:${server.localHttpPort}$serverContextPath" + httpsMessage
            eventListener.triggerEvent("StatusFinal", message)

            boolean isWindows = System.getProperty("os.name").toLowerCase().indexOf("windows") != -1
            if (isWindows) {
                grailsConsole?.reader?.addTriggeredAction((char)3, new ActionListener() {
                    void actionPerformed(ActionEvent e) {
                        stopServer()
                        exit(0)
                    }
                })
            }
            return grailsServer
        }
        catch (Throwable t) {
            if (t instanceof ScriptExitException) throw t
            grailsConsole.error "Server failed to start: $t.message", t
            exit(1)
        }
    }

    @CompileStatic
    void stopServer() {
        if (grailsServer) {
            try {
                grailsServer.stop()
            }
            catch (Throwable e) {
                grailsConsole.error "Error stopping server: ${e.message}", e
            }
        }
        eventListener.triggerEvent("StatusFinal", "Server stopped")
    }
}
