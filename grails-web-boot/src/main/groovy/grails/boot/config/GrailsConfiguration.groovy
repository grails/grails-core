package grails.boot.config

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.grails.boot.support.GrailsPluginManagerPostProcessor
import org.springframework.context.annotation.Bean

/**
 * A handy utility class for creating a Grails Boot configuration
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
abstract class GrailsConfiguration {

    /**
     * @return The classes that constitute the Grails application
     */
    abstract Collection<Class> classes()

    /**
     * @return The {@link GrailsApplication} instance
     */
    @Bean(destroyMethod = 'clear')
    GrailsApplication grailsApplication() {
        def application = new DefaultGrailsApplication( classes() as Class[] )
        application.initialise()
        return application
    }

    /**
     * @return The {@link GrailsPluginManager} instance
     */
    @Bean(destroyMethod = 'shutdown')
    GrailsPluginManager pluginManager() {
        def manager = new DefaultGrailsPluginManager(grailsApplication())
        manager.loadPlugins()
        return manager
    }

    /**
     * @return A post processor that uses the {@link GrailsPluginManager} to configure the {@link org.springframework.context.ApplicationContext}
     */
    @Bean
    GrailsPluginManagerPostProcessor pluginManagerPostProcessor() {
        return new GrailsPluginManagerPostProcessor(pluginManager())
    }
}
