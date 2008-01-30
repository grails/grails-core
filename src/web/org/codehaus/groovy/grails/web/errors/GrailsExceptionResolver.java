/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.errors;

import grails.util.GrailsUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.codehaus.groovy.grails.exceptions.GrailsRuntimeException;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *  Exception resolver that wraps any runtime exceptions with a GrailsWrappedException instance
 * 
 * @author Graeme Rocher
 * @since 22 Dec, 2005
 */
public class GrailsExceptionResolver  extends SimpleMappingExceptionResolver implements ServletContextAware {
    private ServletContext servletContext;

    private static final Log LOG = LogFactory.getLog(GrailsExceptionResolver.class);

    /* (non-Javadoc)
    * @see org.springframework.web.servlet.handler.SimpleMappingExceptionResolver#resolveException(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, java.lang.Exception)
    */
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ModelAndView mv = super.resolveException(request, response, handler, ex);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        GrailsUtil.deepSanitize(ex);

        LOG.error(ex.getMessage(), ex);

        GrailsWrappedRuntimeException gwrex = new GrailsWrappedRuntimeException(servletContext,ex);
        mv.addObject("exception",gwrex);

        UrlMappingsHolder urlMappings = null;
        try {
            urlMappings = WebUtils.lookupUrlMappings(servletContext);
        } catch (Exception e) {
            // ignore, no app ctx in this case.
        }
        if(urlMappings != null) {
            UrlMappingInfo info = urlMappings.matchStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                if(info != null && info.getViewName() != null) {
                    ViewResolver viewResolver = WebUtils.lookupViewResolver(servletContext);
                    View v = WebUtils.resolveView(request, info, info.getViewName(),viewResolver);
                    if(v != null) {
                        mv.setView(v);
                    }
                }
                else if(info != null && info.getControllerName() != null) {
                    String uri;
                    if(request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE) != null) {
                        uri = (String)request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
                    }
                    else {
                        uri = request.getRequestURI();
                    }

                    if(!response.isCommitted()) {
                        String forwardUrl = WebUtils.forwardRequestForUrlMappingInfo(request, response, info, mv.getModel());
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Matched URI ["+uri+"] to URL mapping ["+info+"], forwarding to ["+forwardUrl+"] with response ["+response.getClass()+"]");
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Unable to render errors view: " + e.getMessage(), e);
                throw new GrailsRuntimeException(e);
            }
        }

        return mv;
    }


    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
