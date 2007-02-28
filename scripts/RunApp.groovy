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
import org.mortbay.http.*


Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME" 
grailsServer = null

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" ) 
includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )  
includeTargets << new File ( "${grailsHome}/scripts/PackagePlugins.groovy" )  

task ('default': "Run's a Grails application in Jetty") { 
	depends( checkVersion, packagePlugins, packageApp )
	runApp()
	watchContext()
}                 
task ( runApp : "Main implementation that executes a Grails application") {
	System.setProperty('org.mortbay.xml.XmlParser.NotValidating', 'true')
    def server = new Server()
    grailsServer = server
    try {
        def listener = new SocketListener()
        listener.setPort(serverPort)
        server.addListener(listener)                          

        server.addWebApplication("/${grailsAppName}", "${basedir}/web-app")
        server.start()
    } catch(Throwable t) {
        t.printStackTrace()
    }	
}    
task( watchContext : "Watches the Jetty web.xml for changes and reloads of necessary") {
	def f = new File("${basedir}/web-app/WEB-INF/web.xml")
	long lastModified = f.lastModified()
    while(true) {
		if(lastModified < f.lastModified()) {
			println 'Web Context changed. Reloading...'
        	lastModified = f.lastModified()
			def ctx = grailsServer.getContext("/${grailsAppName}")
			if(ctx) {
				println 'New Controller added. Restarting Grails context: /' + grailsAppName
				ctx.stop(true)
				ctx.start()
			}
			else {
				println 'WARNING: Cannot get server context, new Controller not added'
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
}