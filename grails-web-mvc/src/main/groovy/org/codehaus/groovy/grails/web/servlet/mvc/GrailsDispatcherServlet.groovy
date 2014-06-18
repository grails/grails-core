package org.codehaus.groovy.grails.web.servlet.mvc

import org.springframework.web.context.WebApplicationContext

/**
 * @deprecated Use {@link org.grails.web.servlet.mvc.GrailsDispatcherServlet} instead
 */
class GrailsDispatcherServlet extends org.grails.web.servlet.mvc.GrailsDispatcherServlet{

    GrailsDispatcherServlet() {
    }

    GrailsDispatcherServlet(WebApplicationContext webApplicationContext) {
        super(webApplicationContext)
    }
}
