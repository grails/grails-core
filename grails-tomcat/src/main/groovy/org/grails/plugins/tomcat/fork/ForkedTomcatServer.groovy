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

    @Delegate EmbeddableServer tomcatRunner
    TomcatExecutionContext executionContext


    ForkedTomcatServer(TomcatExecutionContext executionContext) {
        this.executionContext = executionContext
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
    public static Collection<File> findTomcatJars(BuildSettings buildSettings) {
        return buildSettings.buildDependencies.findAll { File it -> it.name.contains("tomcat") && !it.name.contains("grails-plugin-tomcat") } +
                buildSettings.providedDependencies.findAll { File it -> it.name.contains("tomcat") && !it.name.contains("grails-plugin-tomcat") }
    }

    @CompileStatic
    def run() {
        TomcatExecutionContext ec = executionContext
        BuildSettings buildSettings = initializeBuildSettings(ec)
        URLClassLoader classLoader = initializeClassLoader(buildSettings)
        initializeLogging(ec.grailsHome,classLoader)

        tomcatRunner = createTomcatRunner(buildSettings, ec, classLoader)
        if (ec.securePort > 0) {
            tomcatRunner.startSecure(ec.host, ec.port, ec.securePort)
        }
        else {
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
        final ec = executionContext
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

    @Override
    ExecutionContext createExecutionContext() {
        return executionContext
    }

    void stop() {
        try {
            new URL("http://${executionContext?.host}:${executionContext?.port  + 1}").text
        } catch(e) {
            // ignore
        }
    }


    public static void startKillSwitch(final Tomcat tomcat, final int serverPort) {
        new Thread(new TomcatKillSwitch(tomcat, serverPort)).start();
    }

}

