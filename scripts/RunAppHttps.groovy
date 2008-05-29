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
 * Gant script that executes Grails using an embedded Jetty server with
 * an HTTPS listener
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
import org.mortbay.jetty.security.*  
import sun.security.tools.*

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"
userHome = Ant.antProject.properties."user.home"
Ant.property(file:"${grailsHome}/build.properties")
grailsVersion =  Ant.antProject.properties.'grails.version'
grailsServer = null
grailsContext = null
keystore = "${userHome}/.grails/${grailsVersion}/ssl/keystore"
keystoreFile = new File("${keystore}")
keyPassword = "123456"

includeTargets << new File ( "${grailsHome}/scripts/Init.groovy" ) 
includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )
includeTargets << new File ( "${grailsHome}/scripts/RunApp.groovy" )

target ('default': "Run's a Grails application in Jetty with HTTPS listener") {
	depends( checkVersion, configureProxy, packageApp, generateWebXml )
	runAppHttps()
	watchContext()
}                 
target ( runAppHttps : "Main implementation that executes a Grails application with ans HTTPS listener") {
	System.setProperty('org.mortbay.xml.XmlParser.NotValidating', 'true')
    def server = configureHttpServer()
    try {
        if (!(keystoreFile.exists())) {
    	    createCert()
    	}
        def secureListener = new SslSocketConnector()
        secureListener.setPort(serverPortHttps)
    	secureListener.setMaxIdleTime(50000)
    	secureListener.setPassword("${keyPassword}")
    	secureListener.setKeyPassword("${keyPassword}")
    	secureListener.setKeystore("${keystore}")
    	secureListener.setNeedClientAuth(false)
    	secureListener.setWantClientAuth(true)
    	def connectors = server.getConnectors().toList()
    	connectors.add(secureListener)
        server.setConnectors(connectors.toArray(new Connector[0]))
        server.start()
        event("StatusFinal", ["Server running. Browse to https://localhost:$serverPortHttps$serverContextPath"])
    } catch(Throwable t) {
        t.printStackTrace()
        event("StatusFinal", ["Server failed to start: $t"])
    }
}

target(createCert:"Creates a keystore and SSL cert for use with HTTPS"){
 	println 'Creating SSL Cert...'
    if(!keystoreFile.getParentFile().exists() &&
        !keystoreFile.getParentFile().mkdir()){
        def msg = "Unable to create keystore folder: " + keystoreFile.getParentFile().getCanonicalPath()
        event("StatusFinal", [msg])
        throw new RuntimeException(msg)
    }
    String[] keytoolArgs = ["-genkey", "-alias", "localhost", "-dname",
                "CN=localhost,OU=Test,O=Test,C=US", "-keyalg", "RSA",
                "-validity", "365", "-storepass", "key", "-keystore",
                "${keystore}", "-storepass", "${keyPassword}",
                "-keypass", "${keyPassword}"]
    KeyTool.main(keytoolArgs)
    println 'Created SSL Cert'

}
