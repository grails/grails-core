package grails.web.servlet.plugins

import groovy.transform.CompileStatic
import grails.core.GrailsApplication
import grails.plugins.DefaultGrailsPluginManager
import grails.plugins.GrailsPlugin
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.core.io.Resource

import jakarta.servlet.ServletContext

/**
 * @author Graeme Rocher
 */
@CompileStatic
class GrailsWebPluginManager extends DefaultGrailsPluginManager{

    public static final String SERVLET_CONTEXT_INIT_METHOD = 'doWithServletContext'

    GrailsWebPluginManager(String resourcePath, GrailsApplication application) {
        super(resourcePath, application)
    }

    GrailsWebPluginManager(String[] pluginResources, GrailsApplication application) {
        super(pluginResources, application)
    }

    GrailsWebPluginManager(Class<?>[] plugins, GrailsApplication application) {
        super(plugins, application)
    }

    GrailsWebPluginManager(Resource[] pluginFiles, GrailsApplication application) {
        super(pluginFiles, application)
    }

    void doWithServletContext(ServletContext servletContext) {
        for(GrailsPlugin plugin in allPlugins) {
            def instance = plugin.instance
            if(instance instanceof ServletContextInitializer) {
                ((ServletContextInitializer)instance).onStartup(servletContext)
            }
        }
    }
}
