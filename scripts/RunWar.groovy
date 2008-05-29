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
 * Gant script that executes creates WAR file and runs Grails using an embedded Jetty server
 * against the WAR, without reloading
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
grailsWarServer = null
grailsWarContext = null


includeTargets << new File ( "${grailsHome}/scripts/War.groovy" )

shouldPackageTemplates=true

target ('default': "Run's a Grails application's WAR in Jetty") {
	depends( checkVersion, configureProxy, war )
	runWar()
}
target ( runWar : "Main implementation that executes a Grails application WAR") {
	System.setProperty('org.mortbay.xml.XmlParser.NotValidating', 'true')
    try {
		println "Running Grails application from WAR..."
        def server = configureHttpServerForWar()
        profile("start server") {
            server.start()
        }
        event("StatusFinal", ["Server running. Browse to http://localhost:$serverPort$serverContextPath"])
    } catch(Throwable t) {
        t.printStackTrace()
        event("StatusFinal", ["Server failed to start: $t"])
        return
    }

    try {
        while(true) {
            sleep(1000)
        }
    } catch(Throwable t) {
        stopWarServer()
        event("StatusFinal", ["Server stopped: $t"])
        return
    }
}

target( configureHttpServerForWar : "Returns a jetty server configured with an HTTP connector") {
    def server = new Server()
    grailsWarServer = server
    def connectors = [new SelectChannelConnector()]
    connectors[0].setPort(serverPort)
    server.setConnectors( (Connector [])connectors )

    webContext = new WebAppContext(war:warName, contextPath:serverContextPath)
    

    server.setHandler( webContext )

    event("ConfigureJetty", [server])
    return server
}

target( stopWarServer : "Stops the Grails Jetty server") {
	if(grailsWarServer) {
		grailsWarServer.stop()
	}
    event("StatusFinal", ["Server stopped"])
}