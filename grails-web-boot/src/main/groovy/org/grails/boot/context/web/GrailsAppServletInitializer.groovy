package org.grails.boot.context.web

import grails.boot.GrailsAppBuilder
import groovy.transform.CompileStatic
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

/**
 * Ensure a {@link grails.boot.GrailsApp} in constructed during servlet initialization
 *
 * @author Graeme Rocher
 * @since 3.0.6
 */
@CompileStatic
abstract class GrailsAppServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder createSpringApplicationBuilder() {
        return new GrailsAppBuilder()
    }
}
