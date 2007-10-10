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
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.servlet.GrailsUrlPathHelper;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.WebApplicationContext;
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

    public static View resolveView(HttpServletRequest request, UrlMappingInfo info, String viewName, ViewResolver viewResolver) throws Exception {
        View v;
        if(viewName.startsWith(String.valueOf(SLASH))) {
            v = viewResolver.resolveViewName(viewName, request.getLocale());
        }
        else {
            String controllerName = info.getControllerName();
            StringBuffer buf = new StringBuffer();
            buf.append(SLASH);
            if(controllerName != null) {
                buf.append(controllerName).append(SLASH);
            }
            buf.append(viewName);
            v = viewResolver.resolveViewName(buf.toString(), request.getLocale());

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
}
