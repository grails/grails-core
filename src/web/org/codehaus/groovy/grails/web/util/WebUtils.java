/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsUrlPathHelper;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 *
 * Utility methods to access commons objects and perform common web related functions for the internal framework
 * 
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Oct 10, 2007
 */
public class WebUtils extends org.springframework.web.util.WebUtils {
    public static final char SLASH = '/';
    private static final Log LOG = LogFactory.getLog(WebUtils.class);
    public static final String ENABLE_FILE_EXTENSIONS = "grails.mime.file.extensions";
    public static final String DISPATCH_ACTION_PARAMETER = "_action_";

    public static ViewResolver lookupViewResolver(ServletContext servletContext) {
        WebApplicationContext wac =
                WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

        String[] beanNames = wac.getBeanNamesForType(ViewResolver.class);
        if(beanNames.length > 0) {
            String beanName = beanNames[0];
            return (ViewResolver)wac.getBean(beanName);
        }
        return null;

    }

    /**
     * Looks up the UrlMappingsHolder instance
     *
     * @return The UrlMappingsHolder
     * @param servletContext The ServletContext object
     */
    public static UrlMappingsHolder lookupUrlMappings(ServletContext servletContext) {
        WebApplicationContext wac =
                WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

        return (UrlMappingsHolder)wac.getBean(UrlMappingsHolder.BEAN_ID);
    }

    /**
     * Looks up the GrailsApplication instance
     *
     * @return The GrailsApplication instance
     */
    public static GrailsApplication lookupApplication(ServletContext servletContext) {
        WebApplicationContext wac =
                WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

        return (GrailsApplication)wac.getBean(GrailsApplication.APPLICATION_ID);

    }


    /**
     * Resolves a view for the given view and UrlMappingInfo instance
     *
     * @param request The request
     * @param info The info
     * @param viewName The view name
     * @param viewResolver The view resolver
     * @return The view or null
     * @throws Exception
     */
    public static View resolveView(HttpServletRequest request, UrlMappingInfo info, String viewName, ViewResolver viewResolver) throws Exception {
        String controllerName = info.getControllerName();
        return resolveView(request, viewName, controllerName, viewResolver);
    }

    /**
     * Resolves a view for the given view name and controller name
     * @param request The request
     * @param viewName The view name
     * @param controllerName The controller name
     * @param viewResolver The resolver
     * @return A View or null
     * @throws Exception Thrown if an error occurs
     */
    public static View resolveView(HttpServletRequest request, String viewName, String controllerName, ViewResolver viewResolver) throws Exception {
        GrailsWebRequest webRequest = (GrailsWebRequest)request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST);

        View v;
        if(viewName.startsWith(String.valueOf(SLASH))) {
            v = viewResolver.resolveViewName(viewName, webRequest.getLocale());
        }
        else {
            StringBuffer buf = new StringBuffer();
            buf.append(SLASH);
            if(controllerName != null) {
                buf.append(controllerName).append(SLASH);
            }
            buf.append(viewName);
            v = viewResolver.resolveViewName(buf.toString(), webRequest.getLocale());

        }
        return v;
    }

    /**
     * Constructs the URI to forward to using the given request and UrlMappingInfo instance
     *
     * @param info The UrlMappingInfo
     * @return The URI to forward to
     */
    public static String buildDispatchUrlForMapping(UrlMappingInfo info) {
        final StringBuffer forwardUrl = new StringBuffer();

        if (info.getViewName() != null) {
            String viewName = info.getViewName();
            forwardUrl.append(SLASH).append(viewName);
        }
        else {
            forwardUrl.append(GrailsUrlPathHelper.GRAILS_SERVLET_PATH);
            forwardUrl.append(SLASH)
                              .append(info.getControllerName());

            if(!StringUtils.isBlank(info.getActionName())) {
                forwardUrl.append(SLASH)
                          .append(info.getActionName());
            }
            forwardUrl.append(GrailsUrlPathHelper.GRAILS_DISPATCH_EXTENSION);
        }

        return forwardUrl.toString();
    }

    private static void populateWebRequestWithInfo(GrailsWebRequest webRequest, UrlMappingInfo info) {
        if(webRequest != null) {
            final String viewName = info.getViewName();

            if (viewName == null) {
                webRequest.setControllerName(info.getControllerName());
                webRequest.setActionName(info.getActionName());
            }

            String id = info.getId();
            if(!StringUtils.isBlank(id))webRequest.getParams().put(GrailsWebRequest.ID_PARAMETER, id);

            // Add all the parameters from the URL mapping.
            webRequest.getParams().putAll(info.getParameters());
        }
    }


    public static String forwardRequestForUrlMappingInfo(HttpServletRequest request, HttpServletResponse response, UrlMappingInfo info) throws ServletException, IOException {
        return forwardRequestForUrlMappingInfo(request, response, info, Collections.EMPTY_MAP);
    }

    public static String forwardRequestForUrlMappingInfo(HttpServletRequest request, HttpServletResponse response, UrlMappingInfo info, Map model) throws ServletException, IOException {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        String forwardUrl = buildDispatchUrlForMapping(info);

        //populateParamsForMapping(info);
        RequestDispatcher dispatcher = request.getRequestDispatcher(forwardUrl);
        populateWebRequestWithInfo(webRequest, info);

        exposeForwardRequestAttributes(request);
        exposeRequestAttributes(request, model);
        dispatcher.forward(request, response);
        return forwardUrl;
    }

    /**
     * Obtains the format from the URI. The format is the string following the . file extension in the last token of the URI
     *
     * @param uri The URI
     * @return The format or null if none
     */
    public static String getFormatFromURI(String uri) {
        if(uri.endsWith("/")) {
            return null;
        }
        int idx = uri.lastIndexOf('/');
        if(idx > -1) {
            String lastToken = uri.substring(idx+1, uri.length());
            idx = lastToken.lastIndexOf('.');
            if(idx > -1) {
                return lastToken.substring(idx+1, lastToken.length());
            }
        }
        return null;
    }

    /**
     * Returns the value of the "grails.mime.file.extensions" setting configured in COnfig.groovy
     *
     * @return True if file extensions are enabled
     */
    public static boolean areFileExtensionsEnabled() {
        Map config = ConfigurationHolder.getFlatConfig();
        Object o = config.get(ENABLE_FILE_EXTENSIONS);
        return !(o != null && o instanceof Boolean) || ((Boolean) o).booleanValue();
    }

    /**
     * Returns the GrailsWebRequest associated with the current request.
     * This is the preferred means of accessing the GrailsWebRequest
     * instance. If the exception is undesired, you can use
     * RequestContextHolder.getRequestAttributes() instead.
     * @throws IllegalStateException if this is called outside of a
     * request.
     */
    public static GrailsWebRequest retrieveGrailsWebRequest() {
        return (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
    }

    /**
     * Helper method to store the given GrailsWebRequest for the current
     * request. Ensures consistency between RequestContextHolder and the
     * relevant request attribute. This is the preferred means of updating
     * the current web request.
     */
    public static void storeGrailsWebRequest(GrailsWebRequest webRequest) {
        RequestContextHolder.setRequestAttributes(webRequest);
        webRequest.getRequest().setAttribute(GrailsApplicationAttributes.WEB_REQUEST, webRequest);
    }

    /**
     * Removes any GrailsWebRequest instance from the current request.
     */
    public static void clearGrailsWebRequest() {
        RequestAttributes reqAttrs = RequestContextHolder.getRequestAttributes();
        if (reqAttrs != null) {
            // First remove the web request from the HTTP request
            // attributes.
            GrailsWebRequest webRequest = (GrailsWebRequest) reqAttrs;
            webRequest.getRequest().removeAttribute(GrailsApplicationAttributes.WEB_REQUEST);

            // Now remove it from RequestContextHolder.
            RequestContextHolder.setRequestAttributes(null);
        }
    }
}
