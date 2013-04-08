package org.codehaus.groovy.grails.web.filters;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.web.taglib.JavascriptTagLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Sets up the Javascript library to use based on configuration.
 */
public class JavascriptLibraryHandlerInterceptor extends HandlerInterceptorAdapter  {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected String library;

    public JavascriptLibraryHandlerInterceptor(GrailsApplication application) {
        Object lib = application.getFlatConfig().get("grails.views.javascript.library");
        if (lib instanceof CharSequence) {
            library = lib.toString();
            log.debug("Using [{}] as the default Ajax provider.", library);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (library != null) {
            @SuppressWarnings("unchecked")
            List<String> libraries = (List<String>) request.getAttribute(JavascriptTagLib.INCLUDED_LIBRARIES);
            if (libraries == null) {
                libraries = new ArrayList<String>(1);
                request.setAttribute(JavascriptTagLib.INCLUDED_LIBRARIES, libraries);
            }
            libraries.add(library);
        }
        return true;
    }
}
