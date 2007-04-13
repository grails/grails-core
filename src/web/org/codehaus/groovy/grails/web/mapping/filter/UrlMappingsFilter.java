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

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.mapping.UrlMapping;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.codehaus.groovy.grails.web.servlet.WrappedResponseHolder;
import org.codehaus.groovy.grails.web.servlet.GrailsDispatcherServlet;
import org.codehaus.groovy.grails.web.servlet.GrailsUrlPathHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import java.io.IOException;
import java.util.Map;
import java.util.Iterator;

import com.opensymphony.module.sitemesh.filter.RequestDispatcherWrapper;

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
    private static final String SLASH = "/";
    private static final Log LOG = LogFactory.getLog(UrlMappingsFilter.class);


    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UrlMappingsHolder holder = lookupUrlMappings();

        if(LOG.isTraceEnabled()) {
            LOG.trace("Executing URL mapping filter");
        }
        
        UrlMapping[] mappings = holder.getUrlMappings();
        String uri = urlHelper.getPathWithinApplication(request);
        // filter doesn't apply to URLs with extensions for the moment, might add support
        // later to include certain extensions
        if(uri.substring(uri.lastIndexOf(SLASH)).indexOf(".") >-1) {
            filterChain.doFilter(request, response);
            return;
        }

        UrlMappingInfo info = null;
        for (int i = 0; i < mappings.length; i++) {

            UrlMapping mapping = mappings[i];
            if(LOG.isDebugEnabled())
                LOG.debug("Attempting to match URI ["+uri+"] with pattern ["+mapping.getUrlData().getUrlPattern()+"]");
            
            info = mapping.match(uri);

            if(info!=null) {
                break;
            }
        }

        if(info!=null) {
            String forwardUrl = buildDispatchUrlForMapping(request, info);
            if(LOG.isDebugEnabled()) {
                LOG.debug("Matched URI ["+uri+"] to URL mapping, forwarding to ["+forwardUrl+"] with response ["+response.getClass()+"]");
            }
            //populateParamsForMapping(info);
            RequestDispatcher dispatcher = request.getRequestDispatcher(forwardUrl);

            try {
                WrappedResponseHolder.setWrappedResponse(response);
                dispatcher.forward(request, response);                
            }
            finally {
                WrappedResponseHolder.setWrappedResponse(null);
            }



        }
        else {
            if(filterChain!=null)
                filterChain.doFilter(request, response);
        }
    }


    /**
     * Constructs the URI to forward to using the given request and UrlMappingInfo instance
     *
     * @param request The HttpServletRequest
     * @param info The UrlMappingInfo
     * @return The URI to forward to
     */
    protected String buildDispatchUrlForMapping(HttpServletRequest request, UrlMappingInfo info) {
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
}
