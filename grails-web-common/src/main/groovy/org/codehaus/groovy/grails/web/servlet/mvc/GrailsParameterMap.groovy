package org.codehaus.groovy.grails.web.servlet.mvc

import javax.servlet.http.HttpServletRequest

/**
 * A parameter map class that allows mixing of request parameters and controller parameters. If a controller
 * parameter is set with the same name as a request parameter the controller parameter value is retrieved.
 *
 * @author Graeme Rocher
 * @author Kate Rhodes
 * @author Lari Hotari
 *
 * @since 0.5
 * @deprecated Use {@link grails.web.servlet.mvc.GrailsParameterMap} instead
 */
@Deprecated
class GrailsParameterMap extends grails.web.servlet.mvc.GrailsParameterMap{
    GrailsParameterMap(Map values, HttpServletRequest request) {
        super(values, request)
    }

    GrailsParameterMap(HttpServletRequest request) {
        super(request)
    }
}
