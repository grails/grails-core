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
import grails.web.container.EmbeddableServer
import groovy.transform.CompileStatic

import org.apache.catalina.Context
import org.apache.catalina.startup.Tomcat
import org.codehaus.groovy.grails.cli.fork.ExecutionContext
import org.codehaus.groovy.grails.cli.fork.ForkedGrailsProcess
import org.codehaus.groovy.grails.io.support.Resource
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.grails.plugins.tomcat.InlineExplodedTomcatServer
import org.grails.plugins.tomcat.TomcatKillSwitch

/**
 * An implementation of the Tomcat server that runs in forked mode.
 *
 * @author Graeme Rocher
 * @since 2.2
 */
class ForkedTomcatServer extends ForkedGrailsProcess implements EmbeddableServer {

    @Delegate TomcatRunner tomcatRunner
    TomcatExecutionContext executionContext
    ClassLoader forkedClassLoader

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
    def run() {
        TomcatExecutionContext ec = executionContext
        def buildSettings = new BuildSettings(ec.grailsHome, ec.baseDir)
        buildSettings.loadConfig()

        BuildSettingsHolder.settings = buildSettings

        URLClassLoader classLoader = createClassLoader(buildSettings)
        forkedClassLoader = classLoader

        initializeLogging(ec.grailsHome,classLoader)

        tomcatRunner = new TomcatRunner("$buildSettings.baseDir/web-app", buildSettings.webXmlLocation.absolutePath, ec.contextPath, classLoader)
        if (ec.securePort >= 0) {
            tomcatRunner.startSecure(ec.host, ec.port, ec.securePort)
            ec.securePort = tomcatRunner.getLocalHttpsPort()
        }
        else {
            tomcatRunner.start(ec.host, ec.port)
        }
        ec.port = tomcatRunner.getLocalHttpPort()
        File completedExecutionContext = new File(ec.completedContextPath)
        completedExecutionContext.withOutputStream { OutputStream fos ->
            def oos = new ObjectOutputStream(fos)
            oos.writeObject(ec)
        }
        setupReloading(classLoader, buildSettings)
    }

    @CompileStatic
    void start(String host, int port) {
        startSecure(host, port, -1)
    }

    @CompileStatic
    void startSecure(String host, int httpPort, int httpsPort) {
        final ec = executionContext
        ec.host = host
        ec.port = httpPort
        ec.securePort = httpsPort

        def baseName = executionContext.getBaseDir().canonicalFile.name
        File tempFile = File.createTempFile(baseName, "grails-completed-execution-context")
        ec.completedContextPath = tempFile.absolutePath
        tempFile.delete()

        def t = new Thread( {
            final process = fork()
            Runtime.addShutdownHook {
                process.destroy()
            }
        } )

        t.start()
        loadCompletedExecutionContext(ec.completedContextPath)
        while(!isAvailable(host, getLocalHttpPort())) {
            sleep 100
        }
        System.setProperty(TomcatKillSwitch.TOMCAT_KILL_SWITCH_ACTIVE, "true")
    }

    @Override
    int getLocalHttpPort() {
        executionContext.port
    }

    @Override
    int getLocalHttpsPort() {
        executionContext.securePort
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
            new URL("http://${executionContext?.host}:${executionContext?.port - 1}").text
        } catch(e) {
            // ignore
        }
    }

    void loadCompletedExecutionContext(String path) {
        File completedEc = new File(path)
        while(!completedEc.exists()) {
            sleep 100
        }

        executionContext = readExecutionContext(path)
        completedEc.deleteOnExit()
    }

    class TomcatRunner extends InlineExplodedTomcatServer {

        private String currentHost
        private int currentPort

        TomcatRunner(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
            super(basedir, webXml, contextPath, classLoader)
        }

        @Override
        @CompileStatic
        protected void initialize(Tomcat tomcat) {
            final autodeployDir = buildSettings.autodeployDir
            if (autodeployDir.exists()) {
                final wars = autodeployDir.listFiles()
                for (File f in wars) {
                    final fileName = f.name
                    if (fileName.endsWith(".war")) {
                        tomcat.addWebapp(f.name - '.war', f.absolutePath)
                    }
                }
            }

            invokeCustomizer(tomcat)
        }

        private void invokeCustomizer(Tomcat tomcat) {
            Class cls = null
            try {
                cls = forkedClassLoader.loadClass("org.grails.plugins.tomcat.ForkedTomcatCustomizer")
            } catch (Throwable e) {
                // ignore
            }

            if (cls != null) {
                try {
                    cls.newInstance().customize(tomcat)
                } catch (e) {
                    throw new RuntimeException("Error invoking Tomcat server customizer: " + e.getMessage(), e)
                }
            }
        }

        @Override
        protected void configureAliases(Context context) {
            def aliases = []
            final directories = GrailsPluginUtils.getPluginDirectories()
            for (Resource dir in directories) {
                def webappDir = new File("${dir.file.absolutePath}/web-app")
                if (webappDir.exists()) {
                    aliases << "/plugins/${dir.file.name}=${webappDir.absolutePath}"
                }
            }
            if (aliases) {
                context.setAliases(aliases.join(','))
            }
        }

        @Override
        void start(String host, int port) {
            super.start(host, port)
            currentHost = host
            currentPort = getLocalHttpPort()
        }

        @Override
        void stop() {
            try {
                new URL("http://${currentHost}:${currentPort - 1}").text
            } catch(e) {
                // ignore
            }
        }
    }
}

class TomcatExecutionContext extends ExecutionContext {
    String contextPath
    String host = EmbeddableServer.DEFAULT_HOST
    int port = EmbeddableServer.DEFAULT_PORT
    int securePort
    String completedContextPath
}
