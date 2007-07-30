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


import org.mortbay.http.*


Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME" 
grailsServer = null
grailsContext = null


includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )  
includeTargets << new File ( "${grailsHome}/scripts/PackagePlugins.groovy" )  

task ('default': "Run's a Grails application in Jetty") { 
	depends( checkVersion, configureProxy, packagePlugins, packageApp, generateWebXml )
	runApp()
	watchContext()
}                 
task ( runApp : "Main implementation that executes a Grails application") {
	System.setProperty('org.mortbay.xml.XmlParser.NotValidating', 'true')
    try {           
		println "Running Grails application.."
        def server = configureHttpServer()
        server.start()
        event("StatusFinal", ["Server running. Browse to http://localhost:$serverPort/$grailsAppName"])
    } catch(Throwable t) {
        t.printStackTrace()
        event("StatusFinal", ["Server failed to start: $t"])
    }
}
task( watchContext: "Watches the WEB-INF/classes directory for changes and restarts the server if necessary") {
    long lastModified = classesDir.lastModified()
    while(true) {        
		try {
	        Ant.groovyc(destdir:classesDirPath,
	                classpathref:"grails.classpath",
					resourcePattern:"file:${basedir}/**/grails-app/**/*.groovy") {
						src(path:"${basedir}/src/java")
						src(path:"${basedir}/src/groovy")
						src(path:"${basedir}/grails-app/domain")										
			}
			
		}   
		catch(Exception e) {
			println "Compilation error: ${e.message}"	
			e.printStackTrace()
		}
        def tmp = classesDir.lastModified()
        if(lastModified < tmp) {
			lastModified = tmp	
            grailsServer.stop()
            grailsServer.start()  
        }
        sleep(1000)
    }
}
task( configureHttpServer : "Returns a jetty server configured with an HTTP connector") {
    def server = new Server()
    grailsServer = server
    def connectors = [new SelectChannelConnector()]
    connectors[0].setPort(serverPort)
    server.setConnectors( (Connector [])connectors )
    webContext = new WebAppContext("${basedir}/web-app", "/${grailsAppName}")
    webContext.setDefaultsDescriptor("${grailsHome}/conf/webdefault.xml")
    webContext.setClassLoader(Thread.currentThread().getContextClassLoader())
    grailsHandler = webContext
    server.setHandler( webContext )
    return server
}

task( stopServer : "Stops the Grails Jetty server") {
	if(grailsServer) {
		grailsServer.stop()		
	}	
    event("StatusFinal", ["Server stopped"])
}