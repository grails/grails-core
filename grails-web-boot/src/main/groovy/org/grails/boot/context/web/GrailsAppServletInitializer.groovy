package org.grails.boot.context.web

import grails.boot.GrailsAppBuilder
import groovy.transform.CompileStatic
import org.springframework.boot.SpringApplication
import org.springframework.boot.builder.ParentContextApplicationContextInitializer
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext
import org.springframework.boot.web.servlet.support.ErrorPageFilter
import org.springframework.boot.web.servlet.support.ServletContextApplicationContextInitializer
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.Assert
import org.springframework.web.context.WebApplicationContext

import javax.servlet.ServletContext

/**
 * Ensure a {@link grails.boot.GrailsApp} in constructed during servlet initialization
 *
 * @author Graeme Rocher
 * @since 3.0.6
 */
@CompileStatic
abstract class GrailsAppServletInitializer extends SpringBootServletInitializer {

    /*
     * TODO: Replace with createSpringApplicationBuilder() override when upgrading to Spring Boot 1.3 M4 and delete this method
     */
    protected WebApplicationContext createRootApplicationContext(
            ServletContext servletContext) {
        SpringApplicationBuilder builder = new GrailsAppBuilder()
        builder.main(getClass())
        ApplicationContext parent = getExistingRootWebApplicationContext(servletContext)
        if (parent != null) {
            this.logger.info("Root context already created (using as parent).")
            servletContext.setAttribute(
                    WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null)
            builder.initializers(new ParentContextApplicationContextInitializer(parent))
        }
        builder.initializers(new ServletContextApplicationContextInitializer(
                servletContext))
        builder.contextClass(AnnotationConfigServletWebServerApplicationContext.class)
        builder = configure(builder)
        SpringApplication application = builder.build()
        if (application.getAllSources().isEmpty()
                && AnnotationUtils.findAnnotation(getClass(), Configuration.class) != null) {
            application.getSources().add(getClass().name)
        }
        Assert.state(application.getAllSources().size() > 0,
                "No SpringApplication sources have been defined. Either override the "
                        + "configure method or add an @Configuration annotation")
        // Ensure error pages are registered
        application.getSources().add(ErrorPageFilter.class.name)
        return run(application)
    }

    private ApplicationContext getExistingRootWebApplicationContext(
            ServletContext servletContext) {
        Object context = servletContext
                .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)
        if (context instanceof ApplicationContext) {
            return (ApplicationContext) context
        }
        return null
    }
}
