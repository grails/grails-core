package org.grails.plugins.tomcat.fork

import groovy.transform.CompileStatic
import org.apache.catalina.Context
import org.apache.catalina.startup.Tomcat
import org.codehaus.groovy.grails.io.support.Resource
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.grails.plugins.tomcat.InlineExplodedTomcatServer

/**
 * @author Graeme Rocher
 */
class TomcatDevelopmentRunner extends InlineExplodedTomcatServer {

    private String currentHost
    private int currentPort
    private ClassLoader forkedClassLoader

    TomcatDevelopmentRunner(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
        super(basedir, webXml, contextPath, classLoader)
        this.forkedClassLoader = classLoader
    }

    @Override
    @CompileStatic
    protected void initialize(Tomcat tomcat) {
        final autodeployDir = buildSettings.autodeployDir
        if (autodeployDir.exists()) {
            final wars = autodeployDir.listFiles()
            for (File f in wars) {
                final fileName = f.name
                if (fileName.endsWith(".war")) {
                    tomcat.addWebapp(f.name - '.war', f.absolutePath)
                }
            }
        }

        invokeCustomizer(tomcat)
    }

    private void invokeCustomizer(Tomcat tomcat) {
        Class cls = null
        try {
            cls = forkedClassLoader.loadClass("org.grails.plugins.tomcat.ForkedTomcatCustomizer")
        } catch (Throwable e) {
            // ignore
        }

        if (cls != null) {
            try {
                cls.newInstance().customize(tomcat)
            } catch (e) {
                throw new RuntimeException("Error invoking Tomcat server customizer: " + e.getMessage(), e)
            }
        }
    }

    @Override
    protected void configureAliases(Context context) {
        def aliases = []
        final directories = GrailsPluginUtils.getPluginDirectories()
        for (Resource dir in directories) {
            def webappDir = new File("${dir.file.absolutePath}/web-app")
            if (webappDir.exists()) {
                aliases << "/plugins/${dir.file.name}=${webappDir.absolutePath}"
            }
        }
        if (aliases) {
            context.setAliases(aliases.join(','))
        }
    }

    @Override
    void start(String host, int port) {
        currentHost = host
        currentPort = port
        super.start(host, port)
    }

    @Override
    void stop() {
        try {
            new URL("http://${currentHost}:${currentPort+ 1}").text
        } catch(e) {
            // ignore
        }
    }
}