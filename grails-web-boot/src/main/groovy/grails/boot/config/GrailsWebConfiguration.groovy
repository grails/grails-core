package grails.boot.config

import groovy.transform.CompileStatic
import org.grails.boot.support.GrailsWebApplicationPostProcessor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.EnableWebMvc

/**
 * A Grails configuration that scans for classes using the packages defined by the packages() method and creates the necessary
 * {@link grails.core.GrailsApplication} and {@link grails.plugins.GrailsPluginManager} beans
 * that constitute a Grails application.
 *
 * @see org.grails.boot.support.GrailsWebApplicationPostProcessor
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@Configuration
@EnableWebMvc
class GrailsWebConfiguration extends GrailsAutoConfiguration {

    @Override
    GrailsApplicationPostProcessor grailsApplicationPostProcessor() {
        return new GrailsWebApplicationPostProcessor(applicationContext, classes() as Class[])
    }
}
