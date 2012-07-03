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

import org.codehaus.groovy.grails.cli.fork.ForkedGrailsProcess
import org.codehaus.groovy.grails.cli.fork.ExecutionContext
import grails.web.container.EmbeddableServer
import org.apache.catalina.startup.Tomcat
import org.grails.plugins.tomcat.TomcatServer
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import org.apache.tomcat.util.scan.StandardJarScanner
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.io.support.Resource

/**
 * An implementation of the Tomcat server that runs in forked mode
 *
 *
 * @author Graeme Rocher
 * @since 2.2
 */
class ForkedTomcatServer extends ForkedGrailsProcess implements EmbeddableServer{

    @Delegate TomcatRunner tomcatRunner


    static void main(String[] args) {
        new ForkedTomcatServer().run()

    }

    def run() {
        TomcatExecutionContext ec = (TomcatExecutionContext)readExecutionContext()
        def buildSettings = new BuildSettings(ec.grailsHome, ec.baseDir)
        buildSettings.loadConfig()

        BuildSettingsHolder.settings = buildSettings

        tomcatRunner = new TomcatRunner()

    }




    @Override
    ExecutionContext createExecutionContext() {
        return null  //To change body of implemented methods use File | Settings | File Templates.
    }


    class TomcatRunner extends TomcatServer {

        def context
        @Override
        protected void doStart(String host, int httpPort, int httpsPort) {
            final Tomcat tomcat = new Tomcat()
            tomcat.basedir = tomcatDir
            context = tomcat.addWebapp(contextPath, basedir)

            boolean shouldScan = checkAndInitializingClasspathScanning()

            def jarScanner = new StandardJarScanner()
            jarScanner.setScanClassPath(shouldScan)
            context.setJarScanner(jarScanner)

            tomcat.enableNaming()

            context.reloadable = false
            context.setAltDDName(getWorkDirFile("resources/web.xml").absolutePath)
            def aliases = []
            final directories = GrailsPluginUtils.getPluginDirectories()
            for (Resource dir in directories) {

            }

        }

        @Override
        void stop() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
class TomcatExecutionContext extends ExecutionContext {
    String contextPath
    String host
    int port
    int securePort
    File grailsHome
}
