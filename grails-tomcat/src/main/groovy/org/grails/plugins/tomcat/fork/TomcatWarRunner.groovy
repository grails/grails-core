/*
* Copyright 2013 SpringSource
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
import grails.util.Metadata
import groovy.transform.CompileStatic

import org.apache.catalina.LifecycleException
import org.apache.catalina.connector.Connector
import org.apache.catalina.startup.Tomcat
import org.apache.coyote.http11.Http11NioProtocol
import org.grails.plugins.tomcat.TomcatServer

/**
 * A Tomcat runner that runs a WAR file
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class TomcatWarRunner extends TomcatServer{

    private static final GrailsConsole CONSOLE = GrailsConsole.getInstance()

    protected Tomcat tomcat = new Tomcat()
    protected String warPath
    protected String contextPath

    TomcatWarRunner(String warPath, String contextPath) {
        this.warPath = warPath
        this.contextPath = contextPath
    }

    protected void enableSslConnector(String host, int httpsPort) {
        Connector sslConnector
        try {
            sslConnector = new Connector()
        } catch (Exception e) {
            throw new RuntimeException("Couldn't create HTTPS connector", e)
        }

        sslConnector.setScheme("https")
        sslConnector.setSecure(true)
        sslConnector.setPort(httpsPort)
        sslConnector.setProperty("SSLEnabled", "true")
        sslConnector.setAttribute("keystoreFile", keystoreFile)
        sslConnector.setAttribute("keystorePass", keyPassword)
        sslConnector.setURIEncoding("UTF-8")

        if (!host.equals("localhost")) {
            sslConnector.setAttribute("address", host)
        }

        tomcat.getService().addConnector(sslConnector)
    }


    @Override
    protected void doStart(String host, int httpPort, int httpsPort) {

        Metadata.getCurrent().put(Metadata.WAR_DEPLOYED, "true")
        tomcat.port = httpPort
        tomcat.setSilent(true)

        if (getConfigParam("nio")) {
            CONSOLE.updateStatus("Enabling Tomcat NIO Connector")
            def connector = new Connector(Http11NioProtocol.name)
            connector.port = httpPort
            tomcat.service.addConnector(connector)
            tomcat.connector = connector
        }

        tomcat.baseDir = tomcatDir
        try {
            tomcat.addWebapp contextPath, warPath
        } catch (Throwable e) {
            CONSOLE.error("Error loading Tomcat: " + e.getMessage(), e)
            System.exit(1)
        }
        tomcat.enableNaming()

        final Connector connector = tomcat.getConnector()

        // Only bind to host name if we aren't using the default
        if (!host.equals("localhost")) {
            connector.setAttribute("address", host)
        }

        connector.setURIEncoding("UTF-8")

        if (httpsPort) {
            enableSslConnector(host, httpsPort)
        }

        final int serverPort = httpPort
        ForkedTomcatServer.startKillSwitch(tomcat, serverPort)

        try {
            tomcat.start()
            String message = "Server running. Browse to http://"+(host != null ? host : "localhost")+":"+httpPort+contextPath
            CONSOLE.addStatus(message)
        } catch (LifecycleException e) {
            CONSOLE.error("Error loading Tomcat: " + e.getMessage(), e)
            System.exit(1)
        }
    }

    @Override
    void stop() {
        tomcat.stop()
    }
}
