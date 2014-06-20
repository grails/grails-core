package org.codehaus.groovy.grails.web.servlet.mvc

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * An adapter class that takes a regular HttpSession and allows you to access it like a Groovy map.
 *
 * @author Graeme Rocher
 * @since 0.4
 *
 * @deprecated Use {@link grails.web.servlet.mvc.GrailsHttpSession} instead
 */
@Deprecated
class GrailsHttpSession implements HttpSession{

    @Delegate grails.web.servlet.mvc.GrailsHttpSession grailsHttpSession

    GrailsHttpSession(grails.web.servlet.mvc.GrailsHttpSession grailsHttpSession) {
        this.grailsHttpSession = grailsHttpSession
    }
}
