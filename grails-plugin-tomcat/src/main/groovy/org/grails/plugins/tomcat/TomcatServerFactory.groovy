package org.grails.plugins.tomcat

    import grails.web.container.EmbeddableServer
import grails.web.container.EmbeddableServerFactory
import org.grails.plugins.tomcat.fork.ForkedTomcatServer
import org.grails.plugins.tomcat.fork.TomcatExecutionContext
import org.codehaus.groovy.grails.cli.support.BuildSettingsAware
import grails.util.BuildSettings
import groovy.transform.CompileStatic
import grails.util.Environment
import org.apache.commons.logging.Log

class TomcatServerFactory implements EmbeddableServerFactory,BuildSettingsAware {

    BuildSettings buildSettings

    @CompileStatic
    EmbeddableServer createInline(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
        return new InlineExplodedTomcatServer(basedir, webXml, contextPath, classLoader)
    }

    EmbeddableServer createForWAR(String warPath, String contextPath) {
        return new IsolatedWarTomcatServer(warPath, contextPath)
    }
}
