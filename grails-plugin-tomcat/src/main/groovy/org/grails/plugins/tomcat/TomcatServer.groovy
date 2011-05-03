/*
 * Copyright 2011 SpringSource
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
package org.grails.plugins.tomcat

/**
 * Implementation of the Grails {@link EmbeddableServer} interface for Tomcat 7
 *
 * @since 1.4
 * @author Graeme Rocher
 */

import org.codehaus.groovy.grails.plugins.GrailsPluginUtils as GPU

import grails.util.BuildSettingsHolder
import grails.util.GrailsNameUtils
import grails.util.PluginBuildSettings
import grails.web.container.EmbeddableServer
import org.apache.catalina.startup.Tomcat
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

class TomcatServer implements EmbeddableServer {

	static DEFAULT_JVM_ARGS = ["-Xmx512m"]
	static DEFAULT_STARTUP_TIMEOUT_SECS = 300 // 5 mins

	Tomcat tomcat
	def context
	PluginBuildSettings pluginSettings
	def eventListener
	def grailsConfig

    protected String keystore
    protected File keystoreFile
    protected String keyPassword
	protected buildSettings
	protected boolean warRun
	protected warParams = [:]
    protected ant

	TomcatServer(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
		tomcat = new Tomcat()
        this.buildSettings = BuildSettingsHolder.getSettings()

		if(contextPath=='/') contextPath = ''

		def tomcatDir = new File("${buildSettings.projectWorkDir}/tomcat").absolutePath
		def ant = new AntBuilder()
		ant.delete(dir:tomcatDir, failonerror:false)

		tomcat.baseDir = tomcatDir
		context = tomcat.addWebapp(contextPath, basedir)
		tomcat.enableNaming()

		// we handle reloading manually
		context.reloadable = false
		context.setAltDDName("${buildSettings.projectWorkDir}/resources/web.xml")

		def aliases = []
		def pluginManager = PluginManagerHolder.getPluginManager()
		def pluginSettings = GPU.getPluginBuildSettings()
		if(pluginManager!=null) {
			for(plugin in pluginManager.userPlugins) {
				  def dir = pluginSettings.getPluginDirForName(GrailsNameUtils.getScriptName(plugin.name))
				  def webappDir = dir ? new File("${dir.file.absolutePath}/web-app") : null
				  if (webappDir?.exists())
				        aliases << "/plugins/${plugin.fileSystemName}=${webappDir.absolutePath}"
            }
        }

		if(aliases) {
			context.setAliases(aliases.join(','))
		}
		TomcatLoader loader = new TomcatLoader(classLoader)

		loader.container = context
		context.loader = loader

		initialize()
	}

	TomcatServer(String warPath, String contextPath) {
        this.buildSettings = BuildSettingsHolder.getSettings()
		def workDir = buildSettings.projectWorkDir
		ant = new AntBuilder()
		def tomcatDir = new File("${workDir}/tomcat").absolutePath
		def warDir = new File("${workDir}/war").absolutePath
		ant.delete(dir:tomcatDir, failonerror:false)
		ant.delete(dir:warDir, failonerror:false)
		ant.unzip(src:warPath, dest:warDir)

		if(contextPath=='/') contextPath = ''

		warRun = true
		warParams.warPath = warDir
		warParams.contextPath = contextPath
		warParams.tomcatDir = tomcatDir
	}

    protected initialize() {

        keystore = "${buildSettings.grailsWorkDir}/ssl/keystore"
        keystoreFile = new File(keystore)
        keyPassword = "123456"

        System.setProperty('org.mortbay.xml.XmlParser.NotValidating', 'true')
    }

    /**
     * Starts the container on the default port
     */
    void start() {
		start(null, 8080)
	}

    /**
     * Starts the container on the given port
     * @param port The port number
     */
    void start(int port) {
		start(null, port)
	}

    /**
     * Starts the container on the given port
     * @param host The host to start on
     * @param port The port number
     */
    void start(String host, int port) {
    	if(warRun) {
            def outFile = new File(buildSettings.projectTargetDir, "tomcat-out.txt")
            def errFile = new File(buildSettings.projectTargetDir, "tomcat-err.txt")
            [outFile, errFile].each { ant.delete(file: it, failonerror: false) }

            def resultProperty = "tomcat.result"

    	    host = host ?: 'localhost'
    		warParams.host = host
    		warParams.port = port

            Thread.start("tomcat process runner") {
                ant.java(classname: IsolatedTomcat.name, fork: true, failonerror: false, output: outFile, error: errFile, resultproperty: resultProperty) {
                    classpath {
                        def jars = buildSettings.compileDependencies.findAll { it.name.contains("tomcat") }

                        for(jar in jars) {
                            pathelement location:jar
                        }
                    }
                    arg value:warParams.tomcatDir
                    arg value:warParams.warPath
                    arg value:warParams.contextPath
                    arg value:host
                    arg value:port
                    for (a in (getConfigParam('jvmArgs') ?: DEFAULT_JVM_ARGS)) {
                        jvmarg value: a
                    }
                }
            }

    		Runtime.addShutdownHook {
    			// hit the shutdown port
    			try {
    				new URL("http://${host}:${port + 1}").text
    			}catch(e) {}
    		}

            def timeoutSecs = getConfigParam('startupTimeoutSecs') ?: DEFAULT_STARTUP_TIMEOUT_SECS
            def interval = 500 // half a second

            def loops = Math.ceil((timeoutSecs * 1000) / interval)
            def started = false
            def i = 0

            while (!started && i++ < loops) {
                // make sure tomcat didn't error starting up
                def resultCode = ant.project.properties."$resultProperty"
                if (resultCode != null) {
                    def err = ""
                    try { err = errFile.text } catch (IOException e) {}
                    throw new RuntimeException("tomcat exited prematurely with code '$resultCode' (error output: '$err')")
                }

                // look for the magic string that will be written to output when the app is running
                try {
                    started = outFile.text.contains("Server running. ")
                } catch (IOException e) {
                    started = false
                }

                if (!started) { // wait a bit then try again
                    Thread.sleep(interval as long)
                }
            }

            if (!started) { // we didn't start in the specified timeout
                throw new RuntimeException("Tomcat failed to start the app in $timeoutSecs seconds (see output in $outFile.path)")
            }

            println "Tomcat Server running WAR (output written to: $outFile)"
    	}
    	else {
    		preStart()
    		tomcat.port = port
    		if(host) {
    			tomcat.connector.setAttribute("address", host)
    		}

    		tomcat.connector.URIEncoding = 'UTF-8'
    		tomcat.start()
    	}
	}

    private getConfigParam(String name) {
        buildSettings.config.grails.tomcat[name]
    }

	private loadInstance(String name) {
		tomcat.class.classLoader.loadClass(name).newInstance()
	}
	private preStart() {
        eventListener?.event("ConfigureTomcat", [tomcat])
		def jndiEntries = grailsConfig?.grails?.naming?.entries

		if(jndiEntries instanceof Map) {
			jndiEntries.each { name, resCfg ->
				if(resCfg) {
                    if (!resCfg["type"]) {
                        throw new IllegalArgumentException("Must supply a resource type for JNDI configuration")
                    }
                    def res = loadInstance('org.apache.catalina.deploy.ContextResource')
                    res.name = name
                    res.type = resCfg.remove("type")
                    res.auth = resCfg.remove("auth")
                    res.description = resCfg.remove("description")
                    res.scope = resCfg.remove("scope")
                    // now it's only the custom properties left in the Map...
                    resCfg.each {key, value ->
                        res.setProperty (key, value)
                    }

                    context.namingResources.addResource res
				}
			}
		}
    }

    /**
     * Starts a secure container running over HTTPS
     */
    void startSecure() {
		startSecure(8443)
	}

    /**
     * Starts a secure container running over HTTPS for the given port
     * @param port The port
     */
    void startSecure(int port) {
		startSecure("localhost", 8080, port)
	}

    /**
     * Starts a secure container running over HTTPS for the given port and host.
     * @param host The server host
     * @param httpPort The port for HTTP traffic.
     * @param httpsPort The port for HTTPS traffic.
     */
    void startSecure(String host, int httpPort, int httpsPort) {
		preStart()
		tomcat.hostname = host
		tomcat.port = httpPort
		tomcat.connector.URIEncoding = 'UTF-8'
        if (!(keystoreFile.exists())) {
            createSSLCertificate()
        }

		def sslConnector = loadInstance('org.apache.catalina.connector.Connector')
		sslConnector.scheme = "https"
		sslConnector.secure = true
		sslConnector.port = httpsPort
		sslConnector.setProperty("SSLEnabled","true")
		sslConnector.setAttribute("keystore", keystore)
		sslConnector.setAttribute("keystorePass", keyPassword)
		sslConnector.URIEncoding = 'UTF-8'
		tomcat.service.addConnector sslConnector
		tomcat.start()
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
                keystore, "-storepass", keyPassword,
                "-keypass", keyPassword]
        Class<?> keyToolClass
        try {
            keyToolClass = Class.forName('sun.security.tools.KeyTool')
        }
        catch (ClassNotFoundException e) {
            // no try/catch for this one, if neither is found let it fail
            keyToolClass = Class.forName('com.ibm.crypto.tools.KeyTool')
        }
        keyToolClass.main(keytoolArgs)
        println 'Created SSL Certificate.'
    }

    /**
     * Stops the container
     */
    void stop() {
    	if(warRun) {
    		// hit the shutdown port
			try {
				new URL("http://${warParams.host}:${warParams.port + 1}").text
			}catch(e) {}
    	}
    	else {
    		tomcat.stop()
    	}

	}

    /**
     * Typically combines the stop() and start() methods in order to restart the container
     */
    void restart() {
		stop()
		start()
	}
}


class SearchFirstURLClassLoader extends URLClassLoader {
	SearchFirstURLClassLoader(URL[] urls) {
		super(urls)
	}
	Class loadClass(String name, boolean resolve) {
		if(name.startsWith("grails.") || name.startsWith("org.codehaus.groovy")) throw new ClassNotFoundException(name)
		Class c = findLoadedClass(name)
		if(c==null) {
			try {
				c = findClass(name)
			}
			catch(ClassNotFoundException cnfe) {
					c = getParent().loadClass(name, false)
			}
		}
		if(resolve) resolveClass(c)

		return c
	}
}
class ParentDelegatingClassLoader extends ClassLoader {
	ParentDelegatingClassLoader(ClassLoader parent) {
		super(parent)
	}

    Class<?> findClass(String name) {
		parent.findClass(name)
	}
}

