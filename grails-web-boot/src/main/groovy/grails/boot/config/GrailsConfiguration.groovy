package grails.boot.config

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.grails.boot.support.GrailsPluginManagerPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * A handy utility class for creating a Grails Boot configuration
 *
 * @author Graeme Rocher
 * @since 3.0
 */
abstract class GrailsConfiguration {


    abstract Collection<Class> classes()

    @Bean
    GrailsApplication grailsApplication() {
        def application = new DefaultGrailsApplication( classes() as Class[] )
        application.initialise()
        return application
    }

    @Bean
    GrailsPluginManager pluginManager() {
        def manager = new DefaultGrailsPluginManager(grailsApplication())
        manager.loadPlugins()
        return manager
    }

    @Bean
    GrailsPluginManagerPostProcessor pluginManagerPostProcessor() {
        return new GrailsPluginManagerPostProcessor(pluginManager())
    }
}
