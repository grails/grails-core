package org.codehaus.groovy.grails.web.servlet.mvc

import groovy.transform.CompileStatic

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
@CompileStatic
class GrailsParameterMap implements Map {

    @Delegate grails.web.servlet.mvc.GrailsParameterMap delegate

    GrailsParameterMap(grails.web.servlet.mvc.GrailsParameterMap delegate) {
        this.delegate = delegate
    }

    GrailsParameterMap(Map values, HttpServletRequest request) {
        this.delegate = new grails.web.servlet.mvc.GrailsParameterMap(values, request)
    }

    public GrailsParameterMap(HttpServletRequest request) {
        this.delegate = new grails.web.servlet.mvc.GrailsParameterMap(request)
    }
}
