package org.grails.plugins.tomcat

import grails.web.container.EmbeddableServer
import grails.web.container.EmbeddableServerFactory

class TomcatServerFactory implements EmbeddableServerFactory {

    def pluginSettings

    EmbeddableServer createInline(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
        return new InlineExplodedTomcatServer(basedir, webXml, contextPath, classLoader)
    }

    EmbeddableServer createForWAR(String warPath, String contextPath) {
        return new IsolatedWarTomcatServer(warPath, contextPath)
    }
}
