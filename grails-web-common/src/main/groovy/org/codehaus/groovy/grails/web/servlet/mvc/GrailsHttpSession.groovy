package org.codehaus.groovy.grails.web.servlet.mvc

import javax.servlet.http.HttpServletRequest

/**
 * An adapter class that takes a regular HttpSession and allows you to access it like a Groovy map.
 *
 * @author Graeme Rocher
 * @since 0.4
 *
 * @deprecated Use {@link grails.web.servlet.mvc.GrailsHttpSession} instead
 */
class GrailsHttpSession extends grails.web.servlet.mvc.GrailsHttpSession{
    GrailsHttpSession(HttpServletRequest request) {
        super(request)
    }
}
