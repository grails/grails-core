package org.grails.boot.support

import grails.boot.config.GrailsApplicationPostProcessor
import grails.util.Holders
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.grails.web.context.ServletEnvironmentGrailsApplicationDiscoveryStrategy
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.event.ApplicationContextEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.web.context.WebApplicationContext

/**
 * A {@link BeanDefinitionRegistryPostProcessor} that enhances any ApplicationContext with plugin manager capabilities
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@InheritConstructors
class GrailsWebApplicationPostProcessor extends GrailsApplicationPostProcessor {

    @Override
    void onApplicationEvent(ApplicationContextEvent event) {
        super.onApplicationEvent(event)

        def context = event.applicationContext

        if(event instanceof ContextRefreshedEvent) {
            if(context instanceof WebApplicationContext) {
                def servletContext = ((WebApplicationContext) context).servletContext
                Holders.setServletContext(servletContext);
                Holders.addApplicationDiscoveryStrategy(new ServletEnvironmentGrailsApplicationDiscoveryStrategy(servletContext));
            }
        }
    }

}