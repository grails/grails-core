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

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" ) 
includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )  
includeTargets << new File ( "${grailsHome}/scripts/PackagePlugins.groovy" )  

task ('default': "Run's a Grails application in Jetty") { 
	depends( checkVersion, configureProxy, packagePlugins, packageApp, generateWebXml )
	runApp()
	watchContext()
}                 
task ( runApp : "Main implementation that executes a Grails application") {
	System.setProperty('org.mortbay.xml.XmlParser.NotValidating', 'true')
    def server = new Server()
    grailsServer = server
    try {
        def connectors = [new SelectChannelConnector()]
        connectors[0].setPort(serverPort)    
        server.setConnectors( (Connector [])connectors )                          
		WebAppContext handler = new WebAppContext("${basedir}/web-app", "/${grailsAppName}")
        handler.setDefaultsDescriptor("${grailsHome}/conf/webdefault.xml")
        handler.setClassLoader(Thread.currentThread().getContextClassLoader())
		grailsHandler = handler
		server.setHandler( handler )
        server.start()
        event("StatusFinal", ["Server running. Browse to http://localhost:$serverPort/$grailsAppName"])
    } catch(Throwable t) {
        t.printStackTrace()
        event("StatusFinal", ["Server failed to start: $t"])
    }
}    
task( watchContext : "Watches the Jetty web.xml for changes and reloads of necessary") {
	def f = new File("${basedir}/web-app/WEB-INF/web.xml")
	long lastModified = f.lastModified()
    while(true) {
		if(lastModified < f.lastModified()) {
    	    event("StatusUpdate", [ "Web Context changed, reloading"])
        	lastModified = f.lastModified()
			def ctx = grailsServer.getContext("/${grailsAppName}")
			if(ctx) {
        	    event("StatusUpdate", [ 'New Controller added. Restarting Grails context: /' + grailsAppName])
				ctx.stop(true)
				ctx.start()
			}
			else {
        	    event("StatusError", [ 'Cannot get server context, new Controller not added'])
			}
    	}
        sleep(2000)
	}	
}
task( stopServer : "Stops the Grails Jetty server") {
	if(grailsServer) {
		grailsServer.stop()		
	}	
	else {
		 def svr = HttpServer.httpServers.iterator().next()
		 svr.stop()
	}
    event("StatusFinal", ["Server stopped"])
}