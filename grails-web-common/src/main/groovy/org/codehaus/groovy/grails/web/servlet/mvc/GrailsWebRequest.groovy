package org.codehaus.groovy.grails.web.servlet.mvc

import grails.web.util.GrailsApplicationAttributes
import groovy.transform.CompileStatic
import org.springframework.context.ApplicationContext

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Graeme Rocher
 * @since 0.4
 * @deprecated Use {@link org.grails.web.servlet.mvc.GrailsWebRequest} instead
 *
 */
@Deprecated
@CompileStatic
class GrailsWebRequest extends org.grails.web.servlet.mvc.GrailsWebRequest{
    GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, GrailsApplicationAttributes attributes) {
        super(request, response, attributes)
    }

    GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        super(request, response, servletContext)
    }

    GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ApplicationContext applicationContext) {
        super(request, response, servletContext, applicationContext)
    }
}
