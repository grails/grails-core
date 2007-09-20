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
package org.codehaus.groovy.grails.web.mapping.filter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.servlet.GrailsUrlPathHelper;
import org.codehaus.groovy.grails.web.servlet.WrappedResponseHolder;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>A Servlet filter that uses the Grails UrlMappings to match and forward requests to a relevant controller
 * and action
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *
 *        <p/>
 *        Created: Mar 6, 2007
 *        Time: 7:58:19 AM
 */
public class UrlMappingsFilter extends OncePerRequestFilter {

    private UrlPathHelper urlHelper = new UrlPathHelper();
    private static final char SLASH = '/';
    private static final Log LOG = LogFactory.getLog(UrlMappingsFilter.class);


    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UrlMappingsHolder holder = lookupUrlMappings();
        GrailsApplication application = lookupApplication();
        GrailsWebRequest webRequest = (GrailsWebRequest)request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST);

        GrailsClass[] controllers = application.getArtefacts(ControllerArtefactHandler.TYPE);
        if(controllers == null || controllers.length == 0 || holder == null) {
            processFilterChain(request, response, filterChain);
            return;
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Executing URL mapping filter...");
            LOG.debug(holder);
        }


        String uri = urlHelper.getPathWithinApplication(request);

        UrlMappingInfo[] urlInfos = holder.matchAll(uri);
        WrappedResponseHolder.setWrappedResponse(response);
        boolean dispatched = false;
        try {
            for (int i = 0; i < urlInfos.length; i++) {
                UrlMappingInfo info = urlInfos[i];
                    if(info!=null) {
                        String action = info.getActionName() == null ? "" : info.getActionName();
                        GrailsClass controller = application.getArtefactForFeature(ControllerArtefactHandler.TYPE, SLASH + info.getControllerName() + SLASH + action);
                        if(controller == null)  {
                            continue;
                        }
                        dispatched = true;
                        info.configure(webRequest);

                        String forwardUrl = buildDispatchUrlForMapping(request, info);
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Matched URI ["+uri+"] to URL mapping ["+info+"], forwarding to ["+forwardUrl+"] with response ["+response.getClass()+"]");
                        }
                        //populateParamsForMapping(info);
                        RequestDispatcher dispatcher = request.getRequestDispatcher(forwardUrl);
                        populateWebRequestWithInfo(webRequest, info);

                        WebUtils.exposeForwardRequestAttributes(request);
                        dispatcher.forward(request, response);
                        break;
                    }

            }
        }
        finally {
            WrappedResponseHolder.setWrappedResponse(null);
        }

        if(!dispatched) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("No match found, processing remaining filter chain.");
            }
            processFilterChain(request, response, filterChain);
        }

    }

    private void processFilterChain(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        try {
            WrappedResponseHolder.setWrappedResponse(response);
            filterChain.doFilter(request,response);
        } finally {
            WrappedResponseHolder.setWrappedResponse(null);
        }
        return;
    }

    protected static void populateWebRequestWithInfo(GrailsWebRequest webRequest, UrlMappingInfo info) {
        if(webRequest != null) {            
            webRequest.setControllerName(info.getControllerName());
            webRequest.setActionName(info.getActionName());
            String id = info.getId();
            if(!StringUtils.isBlank(id))webRequest.getParams().put(GrailsWebRequest.ID_PARAMETER, id);
        }
    }


    /**
     * Constructs the URI to forward to using the given request and UrlMappingInfo instance
     *
     * @param request The HttpServletRequest
     * @param info The UrlMappingInfo
     * @return The URI to forward to
     */
    protected static String buildDispatchUrlForMapping(HttpServletRequest request, UrlMappingInfo info) {
        StringBuffer forwardUrl = new StringBuffer(GrailsUrlPathHelper.GRAILS_SERVLET_PATH);
        forwardUrl.append(SLASH)
                          .append(info.getControllerName());

        if(!StringUtils.isBlank(info.getActionName())) {
            forwardUrl.append(SLASH)
                      .append(info.getActionName());
        }
        forwardUrl.append(GrailsUrlPathHelper.GRAILS_DISPATCH_EXTENSION);
        return forwardUrl.toString();
    }

    /**
     * Looks up the UrlMappingsHolder instance
     *
     * @return The UrlMappingsHolder
     */
    protected UrlMappingsHolder lookupUrlMappings() {
        WebApplicationContext wac =
                WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

        return (UrlMappingsHolder)wac.getBean(UrlMappingsHolder.BEAN_ID);
    }

    /**
     * Looks up the GrailsApplication instance
     *
     * @return The GrailsApplication instance
     */
    protected GrailsApplication lookupApplication() {
        WebApplicationContext wac =
                WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

        return (GrailsApplication)wac.getBean(GrailsApplication.APPLICATION_ID);

    }

}
