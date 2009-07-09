/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.container

import grails.web.container.EmbeddableServer
import grails.util.BuildSettingsHolder
import grails.util.BuildSettings
import org.mortbay.jetty.webapp.WebAppContext
import org.mortbay.jetty.nio.SelectChannelConnector
import org.mortbay.jetty.Server
import org.mortbay.jetty.Connector
import org.mortbay.jetty.security.SslSocketConnector
import sun.security.tools.KeyTool
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.util.FileCopyUtils
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * An implementation of the EmbeddableServer interface for Jetty.
 *
 * @see EmbeddableServer
 *
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Jan 7, 2009
 */

public class JettyServer implements EmbeddableServer{

    BuildSettings buildSettings
    ConfigObject config
    WebAppContext context
    Server grailsServer
    def eventListener

    protected String keystore
    protected File keystoreFile
    protected String keyPassword


    /**
     * Creates a new JettyServer for the given war and context path
     */
    public JettyServer(String warPath, String contextPath) {
        super()

        initialize()
        this.context = new WebAppContext(war:warPath, contextPath:contextPath)
    }

    /**
     * Constructs a Jetty server instance for the given arguments. Used for inline, non-war deployment
     *
     * @basedir The web application root
     * @webXml The web.xml definition
     * @contextPath The context path to deploy to
     * @classLoader The class loader to use
     */
    public JettyServer(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
        super();
        initialize()
        this.context = createStandardContext(basedir, webXml, contextPath, classLoader)
    }

    /**
     * Initializes the JettyServer class
     */
    protected initialize() {
        this.buildSettings = BuildSettingsHolder.getSettings()
        this.config = ConfigurationHolder.getConfig()

        keystore = "${buildSettings.grailsWorkDir}/ssl/keystore"
        keystoreFile = new File("${keystore}")
        keyPassword = "123456"

        System.setProperty('org.mortbay.xml.XmlParser.NotValidating', 'true')
    }





    /**
     * @see EmbeddableServer#start()
     */
    void start() { start DEFAULT_PORT }

    /**
     * @see EmbeddableServer#start(int)
     */
    void start(int port) {
        assertState()
        start DEFAULT_HOST, port
    }

    /**
     * @see EmbeddableServer#start(String, int)
     */
    public void start (String host, int port) {
        startServer configureHttpServer(context, port, host)
    }

    /**
     * @see EmbeddableServer#startSecure()
     */
    void startSecure() { startSecure DEFAULT_SECURE_PORT }

    /**
     * @see EmbeddableServer#startSecure(int)
     */
    void startSecure(int httpsPort) {
        assertState()
        startSecure DEFAULT_HOST, DEFAULT_PORT, httpsPort
    }

    public void startSecure (String host, int httpPort, int httpsPort) {
        startServer configureHttpsServer(context, httpPort, httpsPort, host)
    }


    /**
     * @see EmbeddableServer#stop()
     */
    void stop() {
        assertState()

        grailsServer.stop()

    }

    /**
     * @see EmbeddableServer#restart()
     */
    void restart() {
        assertState()        
        stop()
        start()
    }

    /**
     * Starts the given Grails server
     */
    protected startServer(Server grailsServer) {
        eventListener?.event("ConfigureJetty", [grailsServer])
        grailsServer.start()
    }



    /**
    * Creates a standard WebAppContext from the given arguments
     */
    protected WebAppContext createStandardContext(String webappRoot, java.lang.String webXml, java.lang.String contextPath, java.lang.ClassLoader classLoader) {
        // Jetty requires a "defaults descriptor" on the filesystem. So,
        // we copy it from Grails to the project work directory (if it's
        // not already there).
        def webDefaults = new File("${buildSettings.projectWorkDir}/webdefault.xml")
        if (!webDefaults.exists()) {
            FileCopyUtils.copy(grailsResource("conf/webdefault.xml").inputStream, new FileOutputStream(webDefaults.path))
        }

        def webContext = new WebAppContext(webappRoot, contextPath)
        def configurations = [org.mortbay.jetty.webapp.WebInfConfiguration,
                org.mortbay.jetty.plus.webapp.Configuration,
                org.mortbay.jetty.webapp.JettyWebXmlConfiguration,
                org.mortbay.jetty.webapp.TagLibConfiguration]*.newInstance()
        def jndiConfig = new org.mortbay.jetty.plus.webapp.EnvConfiguration()
        if (config.grails.development.jetty.env) {
            def res = new FileSystemResource(config.grails.development.jetty.env.toString())
            if (res) {
                jndiConfig.setJettyEnvXml(res.URL)
            }
        }
        configurations.add(1, jndiConfig)
        webContext.configurations = configurations
        webContext.setDefaultsDescriptor(webDefaults.path)
        webContext.setClassLoader(classLoader)
        webContext.setDescriptor(webXml)
        return webContext
    }



    /**
     * Configures a new Jetty Server instance for the given WebAppContext
     */
    protected Server configureHttpServer(WebAppContext context, int serverPort = DEFAULT_PORT, String serverHost = DEFAULT_HOST) {
        def server = new Server()
        grailsServer = server
        def connectors = [new SelectChannelConnector()]
        connectors[0].setPort(serverPort)
        if(serverHost) {
            connectors[0].setHost(serverHost)
        }
        server.setConnectors((Connector[]) connectors)
        server.setHandler(context)
        return server
    }


    /**
     * Configures a secure HTTPS server
     */
    protected configureHttpsServer(WebAppContext context, int httpPort = DEFAULT_PORT, int httpsPort = DEFAULT_SECURE_PORT, String serverHost = DEFAULT_HOST ) {
        def server = configureHttpServer(context, httpPort, serverHost)
        if (!(keystoreFile.exists())) {
            createSSLCertificate()
        }
        def secureListener = new SslSocketConnector()
        secureListener.setPort(httpsPort)
        if(serverHost)
            secureListener.setHost(serverHost) 
        secureListener.setMaxIdleTime(50000)
        secureListener.setPassword(keyPassword)
        secureListener.setKeyPassword(keyPassword)
        secureListener.setKeystore(keystore)
        secureListener.setNeedClientAuth(false)
        secureListener.setWantClientAuth(true)
        def connectors = server.getConnectors().toList()
        connectors.add(secureListener)
        server.setConnectors(connectors.toArray(new Connector[0]))
        return server
    }


    /**
     * Creates the necessary SSL certificate for running in HTTPS mode
     */
    protected createSSLCertificate() {
        println 'Creating SSL Certificate...'
        if (!keystoreFile.parentFile.exists() &&
                !keystoreFile.parentFile.mkdir()) {
            def msg = "Unable to create keystore folder: " + keystoreFile.parentFile.canonicalPath
            throw new RuntimeException(msg)
        }
        String[] keytoolArgs = ["-genkey", "-alias", "localhost", "-dname",
                "CN=localhost,OU=Test,O=Test,C=US", "-keyalg", "RSA",
                "-validity", "365", "-storepass", "key", "-keystore",
                "${keystore}", "-storepass", "${keyPassword}",
                "-keypass", "${keyPassword}"]
        KeyTool.main(keytoolArgs)
        println 'Created SSL Certificate.'
    }

    private assertState() {
        if (!context) throw new IllegalStateException("The org.mortbay.jetty.webapp.WebAppContext has not been initialized!")
    }

    private grailsResource(String path) {
        if (buildSettings.grailsHome) {
            return new FileSystemResource("${buildSettings.grailsHome}/$path")
        }
        else {
            return new ClassPathResource(path)
        }
    }    


}