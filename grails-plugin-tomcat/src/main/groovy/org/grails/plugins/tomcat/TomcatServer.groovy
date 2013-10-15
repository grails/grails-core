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

import static grails.build.logging.GrailsConsole.instance as CONSOLE
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.PluginBuildSettings
import grails.web.container.EmbeddableServer

import org.apache.tomcat.util.scan.StandardJarScanner
import org.codehaus.groovy.grails.cli.support.GrailsBuildEventListener
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.springframework.util.ReflectionUtils

/**
 * Provides common functionality for the inline and isolated variants of tomcat server.
 *
 * @see IsolatedWarTomcatServer
 * @see InlineExplodedTomcatServer
 */
abstract class TomcatServer implements EmbeddableServer {

    protected final BuildSettings buildSettings
    protected final PluginBuildSettings pluginSettings

    protected final File workDir
    protected final File tomcatDir

    protected final boolean usingUserKeystore
    protected final File keystoreFile
    protected final String keyPassword
    protected String truststore
    protected File truststoreFile
    protected String trustPassword

    // These are set from the outside in _GrailsRun
    def grailsConfig
    GrailsBuildEventListener eventListener

    TomcatServer() {
        buildSettings = BuildSettingsHolder.getSettings()
        pluginSettings = GrailsPluginUtils.getPluginBuildSettings()

        workDir = buildSettings.projectWorkDir
        tomcatDir = getWorkDirFile("tomcat")

        def userKeystore = getConfigParam("keystorePath")
        if (userKeystore) {
            usingUserKeystore = true
            keystoreFile = new File(userKeystore)
            keyPassword = getConfigParam("keystorePassword") ?: "changeit" // changeit is the keystore default
        } else {
            usingUserKeystore = false
            keystoreFile = getWorkDirFile("ssl/keystore")
            keyPassword = "123456"
        }

        truststore = "${buildSettings.grailsWorkDir}/ssl/truststore"
        truststoreFile = new File(truststore)
        trustPassword = "123456"

        System.setProperty('org.mortbay.xml.XmlParser.NotValidating', 'true')

        tomcatDir.deleteDir()
    }

    protected boolean checkAndInitializingClasspathScanning() {
        def scanConfig = getConfigParam("scan")
        def shouldScan = (Boolean) (scanConfig.enabled instanceof Boolean ? scanConfig.enabled : false)
        def extraJarsToSkip = scanConfig.excludes
        if (extraJarsToSkip instanceof List && shouldScan) {

            try {
                def jarsToSkipField = ReflectionUtils.findField(StandardJarScanner, "defaultJarsToSkip", Set)
                ReflectionUtils.makeAccessible(jarsToSkipField)
                Set jarsToSkip = jarsToSkipField.get(StandardJarScanner)
                jarsToSkip.addAll(extraJarsToSkip)
            } catch (e) {
                // ignore
            }
        }
        shouldScan
    }
    /**
     * The host and port params will never be null, defaults will be passed if necessary.
     *
     * If httpsPort is > 0, the server should listen for https requests on that port.
     */
    protected abstract void doStart(String host, int httpPort, int httpsPort)

    /**
     * Shutdown the server.
     */
    abstract void stop()

    void restart() {
        stop()
        start()
    }

    void start() {
        start(null, null)
    }

    void start(int port) {
        start(null, port)
    }

    void start(String host, int port) {
        doStart(host ?: DEFAULT_HOST, port, -1)
    }

    void startSecure() {
        startSecure(null)
    }

    void startSecure(int port) {
        startSecure(null, null, port)
    }

    void startSecure(String host, int httpPort, int httpsPort) {
        if (!keystoreFile.exists()) {
            if (usingUserKeystore) {
                throw new IllegalStateException("cannot start tomcat in https because use keystore does not exist (value: $keystoreFile)")
            } else {
                createSSLCertificate()
            }
        }

        doStart(host ?: DEFAULT_HOST, (httpPort >= 0) ? httpPort : DEFAULT_PORT, (httpsPort >= 0) ? httpsPort : DEFAULT_SECURE_PORT)
    }

    protected File getWorkDirFile(String path) {
        new File(workDir, path)
    }

    protected getConfigParam(String name) {
        buildSettings.config.grails.tomcat[name]
    }

    protected Map getConfigParams() {
        buildSettings.config.grails.tomcat
    }

    protected createSSLCertificate() {
        CONSOLE.updateStatus 'Creating SSL Certificate...'

        def keystoreDir = keystoreFile.parentFile
        if (!keystoreDir.exists() && !keystoreDir.mkdir()) {
            throw new RuntimeException("Unable to create keystore folder: " + keystoreDir.canonicalPath)
        }

        getKeyToolClass().main(
            "-genkey",
            "-alias", "localhost",
            "-dname", "CN=localhost,OU=Test,O=Test,C=US",
            "-keyalg", "RSA",
            "-validity", "365",
            "-storepass", "key",
            "-keystore", keystoreFile.absolutePath,
            "-storepass", keyPassword,
            "-keypass", keyPassword)

        println 'Created SSL Certificate.'
    }

    private getKeyToolClass() {
        try {
            Class.forName('sun.security.tools.KeyTool')
        } catch (ClassNotFoundException e) {
            // no try/catch for this one, if neither is found let it fail
            Class.forName('com.ibm.crypto.tools.KeyTool')
        }
    }
}
