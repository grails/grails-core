/*
 * Copyright 2011 the original author or authors.
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

import grails.util.GrailsNameUtils
import org.apache.catalina.connector.Connector
import org.apache.catalina.startup.Tomcat
import org.apache.coyote.http11.Http11NioProtocol
import org.codehaus.groovy.grails.lifecycle.ShutdownOperations
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import static grails.build.logging.GrailsConsole.instance as CONSOLE
import org.apache.tomcat.util.scan.StandardJarScanner
import org.springframework.util.ReflectionUtils
/**
 * Serves the app, without packaging as a war and runs it in the same JVM.
 */
class InlineExplodedTomcatServer extends TomcatServer {

    final Tomcat tomcat = new Tomcat()

    def context

    InlineExplodedTomcatServer(String basedir, String webXml, String contextPath, ClassLoader classLoader) {

        if (contextPath == '/') {
            contextPath = ''
        }

        tomcat.basedir = tomcatDir
        context = tomcat.addWebapp(contextPath, basedir)
        def scanConfig = getConfigParam("scan")
        def shouldScan = (Boolean) (scanConfig.enabled instanceof Boolean ? scanConfig.enabled : false)
        def extraJarsToSkip = scanConfig.excludes
        if(extraJarsToSkip instanceof List && shouldScan) {

            try {
                def jarsToSkipField = ReflectionUtils.findField(StandardJarScanner, "defaultJarsToSkip", Set)
                ReflectionUtils.makeAccessible(jarsToSkipField)
                Set jarsToSkip = jarsToSkipField.get(StandardJarScanner)
                jarsToSkip.addAll(extraJarsToSkip)
            } catch (e) {
                // ignore
            }
        }

        

        def jarScanner = new StandardJarScanner()
        jarScanner.setScanClassPath(shouldScan)
        context.setJarScanner(jarScanner)

        tomcat.enableNaming()

        // we handle reloading manually
        context.reloadable = false
        context.setAltDDName(getWorkDirFile("resources/web.xml").absolutePath)

        def aliases = []
        def pluginManager = PluginManagerHolder.getPluginManager()

        if (pluginManager != null) {
            for (plugin in pluginManager.userPlugins) {
                def dir = pluginSettings.getPluginDirForName(GrailsNameUtils.getScriptName(plugin.name))
                def webappDir = dir ? new File("${dir.file.absolutePath}/web-app") : null
                if (webappDir?.exists()) {
                    aliases << "/plugins/${plugin.fileSystemName}=${webappDir.absolutePath}"
                }
            }
        }

        if (aliases) {
            context.setAliases(aliases.join(','))
        }

        def loader = new TomcatLoader(classLoader)
        loader.container = context
        context.loader = loader
    }

    void doStart(String host, int httpPort, int httpsPort) {
        preStart()

        if (host != "localhost") {
            tomcat.connector.setAttribute("address", host)
            tomcat.connector.setAttribute("port", httpPort)
        }

        if (getConfigParam("nio")) {
            CONSOLE.updateStatus "Enabling Tomcat NIO connector"
            def connector = new Connector(Http11NioProtocol.name)
            connector.port = httpPort
            tomcat.service.addConnector(connector)
            tomcat.connector = connector
        }

        tomcat.port = httpPort
        tomcat.connector.URIEncoding = 'UTF-8'

        if (httpsPort) {
            def sslConnector = loadInstance('org.apache.catalina.connector.Connector')
            sslConnector.scheme = "https"
            sslConnector.secure = true
            sslConnector.port = httpsPort
            sslConnector.setProperty("SSLEnabled","true")
            sslConnector.setAttribute("keystoreFile", keystoreFile.absolutePath)
            sslConnector.setAttribute("keystorePass", keyPassword)
            sslConnector.URIEncoding = 'UTF-8'

            if (host != "localhost") {
                sslConnector.setAttribute("address", host)
            }

            if (truststoreFile.exists()) {
                CONSOLE.addStatus "Using truststore $truststore"
                sslConnector.setAttribute("truststoreFile", truststore)
                sslConnector.setAttribute("truststorePass", trustPassword)
                sslConnector.setAttribute("clientAuth", "want")
            }

            tomcat.service.addConnector(sslConnector)
        }

        tomcat.start()
        IsolatedTomcat.startKillSwitch(tomcat, httpPort)
    }

    void stop() {
        ShutdownOperations.runOperations()
        tomcat.stop()
        tomcat.destroy()
        GrailsPluginUtils.clearCaches()
    }

    private loadInstance(String name) {
        tomcat.class.classLoader.loadClass(name).newInstance()
    }

    private preStart() {
        eventListener?.triggerEvent("ConfigureTomcat", tomcat)
        def jndiEntries = grailsConfig?.grails?.naming?.entries

        if (!(jndiEntries instanceof Map)) {
            return
        }

        System.setProperty("javax.sql.DataSource.Factory","org.apache.commons.dbcp.BasicDataSourceFactory");


        jndiEntries.each { name, resCfg ->
            if (resCfg) {
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