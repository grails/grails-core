/*
 * Copyright 2004-2005 Graeme Rocher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.util;

import groovy.lang.GroovyObject;
import groovy.util.ConfigObject;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.ParameterCreationListener;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Utility methods for clients using the web framework.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
@SuppressWarnings("rawtypes")
public class GrailsWebUtil {

    public static final String DEFAULT_ENCODING = "UTF-8";
    private static final String CHARSET_ATTRIBUTE = ";charset=";
    private static final Pattern CHARSET_IN_CONTENT_TYPE_REGEXP = Pattern.compile(";\\s*charset\\s*=", Pattern.CASE_INSENSITIVE);

    /**
     * Looks up a GrailsApplication instance from the ServletContext.
     *
     * @param servletContext The ServletContext
     * @return A GrailsApplication or null if there isn't one
     */
    public static GrailsApplication lookupApplication(ServletContext servletContext) {
        if (servletContext == null) {
            return null;
        }

        final WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (context == null || !context.containsBean(GrailsApplication.APPLICATION_ID)) {
            return null;
        }

        return context.getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
    }

    /**
     * @return The currently bound GrailsApplication instance
     * @since 2.0
     */
    public static GrailsApplication currentApplication() {
        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof GrailsWebRequest)) {
            return null;
        }

        return ((GrailsWebRequest)requestAttributes).getAttributes().getGrailsApplication();
    }

    /**
     * @return The currently bound GrailsApplication instance
     * @since 2.0
     */
    public static Map currentConfiguration() {
        GrailsApplication application = currentApplication();
        return application == null ? new ConfigObject() : application.getConfig();
    }

    /**
     * @return The currently bound GrailsApplication instance
     * @since 2.0
     */
    public static Map currentFlatConfiguration() {
        GrailsApplication application = currentApplication();
        return application == null ? Collections.emptyMap() : application.getFlatConfig();
    }

    /**
     * Binds a Mock implementation of a GrailsWebRequest object to the current thread. The mock version uses
     * instances of the Spring MockHttpServletRequest, MockHttpServletResponse and MockServletContext classes.
     *
     * @param ctx The WebApplicationContext to use
     *
     * @see org.springframework.mock.web.MockHttpServletRequest
     * @see org.springframework.mock.web.MockHttpServletResponse
     * @see org.springframework.mock.web.MockServletContext
     *
     * @return The GrailsWebRequest instance
     */
    public static GrailsWebRequest bindMockWebRequest(WebApplicationContext ctx) {
        return bindMockWebRequest(ctx, new MockHttpServletRequest(ctx.getServletContext()), new MockHttpServletResponse());
    }

    /**
     * Binds a Mock implementation of a GrailsWebRequest object to the current thread. The mock version uses
     * instances of the Spring MockHttpServletRequest, MockHttpServletResponse and MockServletContext classes.
     *
     * @param ctx The WebApplicationContext to use
     * @param request The request
     * @param response The response
     *
     * @see org.springframework.mock.web.MockHttpServletRequest
     * @see org.springframework.mock.web.MockHttpServletResponse
     * @see org.springframework.mock.web.MockServletContext
     *
     * @return The GrailsWebRequest instance
     */
    public static GrailsWebRequest bindMockWebRequest(WebApplicationContext ctx, MockHttpServletRequest request, MockHttpServletResponse response) {
        GrailsWebRequest webRequest = new GrailsWebRequest(request, response, ctx.getServletContext(), ctx);
        request.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, webRequest);
        for (ParameterCreationListener listener: ctx.getBeansOfType(ParameterCreationListener.class).values()) {
            webRequest.addParameterListener(listener);
        }
        RequestContextHolder.setRequestAttributes(webRequest);
        return webRequest;
    }

    /**
     * Binds a Mock implementation of a GrailsWebRequest object to the current thread. The mock version uses
     * instances of the Spring MockHttpServletRequest, MockHttpServletResponse and MockServletContext classes.
     *
     * @see org.springframework.mock.web.MockHttpServletRequest
     * @see org.springframework.mock.web.MockHttpServletResponse
     * @see org.springframework.mock.web.MockServletContext
     *
     * @return The GrailsWebRequest instance
     */
    public static GrailsWebRequest bindMockWebRequest() {
        ServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        return bindMockWebRequest(servletContext, request, response);
    }

    private static GrailsWebRequest bindMockWebRequest(ServletContext servletContext, MockHttpServletRequest request, MockHttpServletResponse response) {
        GrailsWebRequest webRequest = new GrailsWebRequest(request,
                                                response, servletContext);
        request.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, webRequest);
        RequestContextHolder.setRequestAttributes(webRequest);
        return webRequest;
    }

    /**
     * Retrieves the URI from the request from either the include attribute or the request.getRequestURI() method.
     *
     * @param request The HttpServletRequest instance
     * @return The String URI
     */
    public static String getUriFromRequest(HttpServletRequest request) {
        Object includeUri = request.getAttribute("javax.servlet.include.request_uri");
        return includeUri == null ? request.getRequestURI() : (String)includeUri;
    }

    /**
     * Obtains the currently executing controller from the given request if any.
     * @param request The request object
     * @return The controller or null
     */
    public static GroovyObject getControllerFromRequest(HttpServletRequest request) {
        return (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);
    }

    public static String getContentType(String name, String encoding) {
        if (CHARSET_IN_CONTENT_TYPE_REGEXP.matcher(name).find()) {
            return name;
        }
        if (StringUtils.isBlank(encoding)) encoding = DEFAULT_ENCODING;
        return name + CHARSET_ATTRIBUTE + encoding;
    }
}
