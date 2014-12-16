package org.codehaus.groovy.grails.web.servlet.mvc

import grails.util.Holders
import org.grails.web.util.GrailsApplicationAttributes
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.servlet.DelegatingApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.FlashScope
import org.codehaus.groovy.grails.web.servlet.GrailsFlashScope
import org.springframework.context.ApplicationContext
import org.springframework.web.context.request.NativeWebRequest

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
class GrailsWebRequest implements NativeWebRequest {

    @Delegate org.grails.web.servlet.mvc.GrailsWebRequest webRequest

    GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, GrailsApplicationAttributes attributes) {
        webRequest = new org.grails.web.servlet.mvc.GrailsWebRequest(request, response, attributes)
    }

    GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        webRequest = new org.grails.web.servlet.mvc.GrailsWebRequest(request, response, servletContext)
    }

    GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ApplicationContext applicationContext) {
        webRequest = new org.grails.web.servlet.mvc.GrailsWebRequest(request, response, servletContext, applicationContext)
    }

    GrailsWebRequest(org.grails.web.servlet.mvc.GrailsWebRequest webRequest) {
        this.webRequest = webRequest
    }
/**
     * Looks up the current Grails WebRequest instance
     * @return The GrailsWebRequest instance
     */
    public static GrailsWebRequest lookup() {
        return new GrailsWebRequest(org.grails.web.servlet.mvc.GrailsWebRequest.lookup())
    }

    /**
     * Looks up the GrailsWebRequest from the current request.
     * @param request The current request
     * @return The GrailsWebRequest
     */
    public static GrailsWebRequest lookup(HttpServletRequest request) {
        org.grails.web.servlet.mvc.GrailsWebRequest webRequest = (org.grails.web.servlet.mvc.GrailsWebRequest) request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST);

        return webRequest == null ? lookup() : new GrailsWebRequest(webRequest);
    }

    /**
     * Looks up the GrailsApplication from the current request.

     * @return The GrailsWebRequest
     * @deprecated Use {@link grails.util.Holders#findApplication()} instead
     */
    @Deprecated
    public static GrailsApplication lookupApplication() {
        return (GrailsApplication)Holders.findApplication();
    }

    FlashScope getFlashScope() {
        new GrailsFlashScope(webRequest.getFlashScope())
    }



    /**
     * @return The Grails params object
     */
    public GrailsParameterMap getParams() {
        return new GrailsParameterMap(webRequest.params)
    }

    GrailsHttpSession getSession() {
        return new GrailsHttpSession(webRequest.getSession())
    }

    org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes getAttributes() {
        return new DelegatingApplicationAttributes(webRequest.getAttributes())
    }
}
