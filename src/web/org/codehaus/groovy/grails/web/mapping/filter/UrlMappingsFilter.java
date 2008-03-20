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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.WrappedResponseHolder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

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
    private static final Log LOG = LogFactory.getLog(UrlMappingsFilter.class);
    private static final String GSP_SUFFIX = ".gsp";
    private static final String JSP_SUFFIX = ".jsp";


    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        urlHelper.setUrlDecode(false);
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UrlMappingsHolder holder = WebUtils.lookupUrlMappings(getServletContext());
        GrailsApplication application = WebUtils.lookupApplication(getServletContext());
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
        if(WebUtils.areFileExtensionsEnabled()) {
            String format = WebUtils.getFormatFromURI(uri);
            if(format!=null) {
                request.setAttribute(GrailsApplicationAttributes.CONTENT_FORMAT, format);
                uri = uri.substring(0, (uri.length()-format.length()-1));
            }
        }

        UrlMappingInfo[] urlInfos = holder.matchAll(uri);
        WrappedResponseHolder.setWrappedResponse(response);
        boolean dispatched = false;
        try {
            for (int i = 0; i < urlInfos.length; i++) {
                UrlMappingInfo info = urlInfos[i];
                    if(info!=null) {
                        info.configure(webRequest);
                        String action = info.getActionName() == null ? "" : info.getActionName();
                        final String viewName = info.getViewName();

                        if (viewName == null) {
                            final String controllerName = info.getControllerName();
                            GrailsClass controller = application.getArtefactForFeature(ControllerArtefactHandler.TYPE, org.codehaus.groovy.grails.web.util.WebUtils.SLASH + controllerName + org.codehaus.groovy.grails.web.util.WebUtils.SLASH + action);
                            if(controller == null)  {
                                continue;
                            }
                        }

                        dispatched = true;

                        

                        if(viewName == null || viewName.endsWith(GSP_SUFFIX) || viewName.endsWith(JSP_SUFFIX)) {
                            String forwardUrl = WebUtils.forwardRequestForUrlMappingInfo(request, response, info);
                            if(LOG.isDebugEnabled()) {
                                LOG.debug("Matched URI ["+uri+"] to URL mapping ["+info+"], forwarding to ["+forwardUrl+"] with response ["+response.getClass()+"]");
                            }

                        }
                        else {
                            ViewResolver viewResolver = WebUtils.lookupViewResolver(getServletContext());
                            if(viewResolver != null) {
                                View v;
                                try {
                                    v = org.codehaus.groovy.grails.web.util.WebUtils.resolveView(request, info, viewName, viewResolver);
                                    v.render(Collections.EMPTY_MAP, request, response);
                                } catch (Exception e) {
                                    throw new UrlMappingException("Error mapping onto view ["+viewName+"]: " + e.getMessage(),e);
                                }
                            }
                        }
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
            if(filterChain != null)
                filterChain.doFilter(request,response);
        } finally {
            WrappedResponseHolder.setWrappedResponse(null);
        }
    }


}
