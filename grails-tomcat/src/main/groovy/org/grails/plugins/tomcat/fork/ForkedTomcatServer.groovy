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
package org.grails.plugins.tomcat.fork

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.Environment
import grails.web.container.EmbeddableServer
import groovy.transform.CompileStatic
import org.apache.catalina.startup.Tomcat
import org.codehaus.groovy.grails.cli.fork.ExecutionContext
import org.codehaus.groovy.grails.cli.fork.ForkedGrailsProcess
import org.grails.plugins.tomcat.TomcatKillSwitch

/**
 * An implementation of the Tomcat server that runs in forked mode.
 *
 * @author Graeme Rocher
 * @since 2.2
 */
class ForkedTomcatServer extends ForkedGrailsProcess implements EmbeddableServer {

    public static final GrailsConsole CONSOLE = GrailsConsole.getInstance()
    @Delegate EmbeddableServer tomcatRunner


    ForkedTomcatServer(TomcatExecutionContext executionContext) {
        this.executionContext = executionContext
        this.forkReserve = true
    }

    private ForkedTomcatServer() {
        executionContext = (TomcatExecutionContext)readExecutionContext()
        if (executionContext == null) {
            throw new IllegalStateException("Forked server created without first creating execution context and calling fork()")
        }
    }

    static void main(String[] args) {
        new ForkedTomcatServer().run()
    }

    @CompileStatic
    def run() {
        if (!isReserveProcess()) {
            runInternal()
        }
        else {
            CONSOLE.verbose("Waiting for resume signal for idle JVM")
            waitForResume()
            CONSOLE.verbose("Resuming idle JVM")
            runInternal()
        }

    }

    protected void runInternal() {
        TomcatExecutionContext ec = (TomcatExecutionContext)executionContext
        BuildSettings buildSettings = initializeBuildSettings(ec)
        URLClassLoader classLoader = initializeClassLoader(buildSettings)
        initializeLogging(ec.grailsHome, classLoader)

        tomcatRunner = createTomcatRunner(buildSettings, ec, classLoader)
        if (ec.securePort > 0) {
            tomcatRunner.startSecure(ec.host, ec.port, ec.securePort)
        } else {
            tomcatRunner.start(ec.host, ec.port)
        }

        setupReloading(classLoader, buildSettings)
    }



    @Override
    protected void discoverAndSetAgent(ExecutionContext executionContext) {
        TomcatExecutionContext tec = (TomcatExecutionContext)executionContext
        // no agent for war mode
        if (!tec.warPath) {
            super.discoverAndSetAgent(executionContext)
        }
    }

    @CompileStatic
    protected EmbeddableServer createTomcatRunner(BuildSettings buildSettings, TomcatExecutionContext ec, URLClassLoader classLoader) {
        if (ec.warPath) {
            if (Environment.isFork()) {
                BuildSettings.initialiseDefaultLog4j(classLoader)
            }

            new TomcatWarRunner(ec.warPath, ec.contextPath)
        }
        else {
            new TomcatDevelopmentRunner("$buildSettings.baseDir/web-app", buildSettings.webXmlLocation.absolutePath, ec.contextPath, classLoader)
        }
    }

    @CompileStatic
    void start(String host, int port) {
        startSecure(host, port, 0)
    }

    @CompileStatic
    void startSecure(String host, int httpPort, int httpsPort) {
        final ec = (TomcatExecutionContext)executionContext
        ec.host = host
        ec.port = httpPort
        ec.securePort = httpsPort
        def t = new Thread( {
            final process = fork()
            Runtime.addShutdownHook {
                process.destroy()
            }
        } )

        t.start()
        while(!isAvailable(host, httpPort)) {
            sleep 100
        }
        System.setProperty(TomcatKillSwitch.TOMCAT_KILL_SWITCH_ACTIVE, "true")
    }

    @CompileStatic
    boolean isAvailable(String host, int port) {
        try {
            new Socket(host, port)


            return true
        } catch (e) {
            return false
        }
    }


    void stop() {
        final ec = (TomcatExecutionContext)executionContext
        try {
            new URL("http://${ec?.host ?: 'localhost'}:${(ec?.port ?: 8080 )  + 1}").text
        } catch(e) {
            // ignore
        }
    }


    public static void startKillSwitch(final Tomcat tomcat, final int serverPort) {
        new Thread(new TomcatKillSwitch(tomcat, serverPort)).start();
    }

}

