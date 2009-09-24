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

import grails.util.GrailsUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.mime.MimeType;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.WrappedResponseHolder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private HandlerInterceptor[] handlerInterceptors = new HandlerInterceptor[0];
    private GrailsApplication application;
    private ViewResolver viewResolver;

    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        urlHelper.setUrlDecode(false);
        final ServletContext servletContext = getServletContext();
        this.handlerInterceptors = WebUtils.lookupHandlerInterceptors(servletContext);       
        this.application = WebUtils.lookupApplication(servletContext);
        this.viewResolver = WebUtils.lookupViewResolver(servletContext);
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UrlMappingsHolder holder = WebUtils.lookupUrlMappings(getServletContext());

        GrailsWebRequest webRequest = (GrailsWebRequest)request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST);

        List<String> excludePatterns = holder.getExcludePatterns();

        String uri = urlHelper.getPathWithinApplication(request);
        
        GrailsClass[] controllers = application.getArtefacts(ControllerArtefactHandler.TYPE);
        if((controllers == null || controllers.length == 0 || holder == null) && !"/".equals(uri)) {
            processFilterChain(request, response, filterChain);
            return;
        }else if (excludePatterns!=null && excludePatterns.size()>0){
            for (String excludePattern:excludePatterns){
                if (uri.equals(excludePattern)||
                        (excludePattern.endsWith("*")&&
                                excludePattern.substring(0,excludePattern.length()-1).
                                        regionMatches(0,uri,0,excludePattern.length()-1))){
                    processFilterChain(request, response, filterChain);
                    return;
                }
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Executing URL mapping filter...");
            LOG.debug(holder);
        }



        if(WebUtils.areFileExtensionsEnabled()) {
            String format = WebUtils.getFormatFromURI(uri);
            if(format!=null) {
                MimeType[] configuredMimes = MimeType.getConfiguredMimeTypes();
                // only remove the file extension if its one of the configured mimes in Config.groovy
                for (MimeType configuredMime : configuredMimes) {
                    if (configuredMime.getExtension().equals(format)) {
                        request.setAttribute(GrailsApplicationAttributes.CONTENT_FORMAT, format);
                        uri = uri.substring(0, (uri.length() - format.length() - 1));
                        break;
                    }
                }
            }
        }

        UrlMappingInfo[] urlInfos = holder.matchAll(uri);
        WrappedResponseHolder.setWrappedResponse(response);
        boolean dispatched = false;
        try {
            // GRAILS-3369: Save the original request parameters.
            Map backupParameters;

            try {
                backupParameters = new HashMap(webRequest.getParams());
            }
            catch (Exception e) {
                GrailsUtil.deepSanitize(e);
                LOG.error("Error creating params object: " + e.getMessage(), e);
                backupParameters = Collections.EMPTY_MAP;
            }

            for (UrlMappingInfo info : urlInfos) {
                if (info != null) {
                    // GRAILS-3369: The configure() will modify the
                    // parameter map attached to the web request. So,
                    // we need to clear it each time and restore the
                    // original request parameters.
                    webRequest.getParams().clear();
                    webRequest.getParams().putAll(backupParameters);

                    final String viewName;
                    try {
                        info.configure(webRequest);
                        String action = info.getActionName() == null ? "" : info.getActionName();
                        viewName = info.getViewName();
                        if (viewName == null && info.getURI() == null) {
                            final String controllerName = info.getControllerName();
                            GrailsClass controller = application.getArtefactForFeature(ControllerArtefactHandler.TYPE, WebUtils.SLASH + controllerName + WebUtils.SLASH + action);
                            if (controller == null) {
                                continue;
                            }
                        }
                    }
                    catch (Exception e) {
                        GrailsUtil.deepSanitize(e);
                        if(e instanceof MultipartException) {
                        	throw ((MultipartException)e);
                        }
                        LOG.error("Error when matching URL mapping [" + info + "]:" + e.getMessage(), e);
                        continue;
                    }

                    dispatched = true;

                    request = checkMultipart(request);

                    if (viewName == null || viewName.endsWith(GSP_SUFFIX) || viewName.endsWith(JSP_SUFFIX)) {
                    	
                        if(info.isParsingRequest()) {
                            webRequest.informParameterCreationListeners();
                        }
                        String forwardUrl = WebUtils.forwardRequestForUrlMappingInfo(request, response, info);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Matched URI [" + uri + "] to URL mapping [" + info + "], forwarding to [" + forwardUrl + "] with response [" + response.getClass() + "]");
                        }

                    }
                    else {
                        if(!renderViewForUrlMappingInfo(request, response, info, viewName)) {
                            dispatched = false;
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

    protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
        // Lookup from request attribute. The resolver that handles MultiPartRequest is dealt with earlier inside DefaultUrlMappingInfo with Grails
        HttpServletRequest resolvedRequest = (HttpServletRequest) request.getAttribute(MultipartHttpServletRequest.class.getName());
        if(resolvedRequest!=null) return resolvedRequest;
        return request;
    }


    private boolean renderViewForUrlMappingInfo(HttpServletRequest request, HttpServletResponse response, UrlMappingInfo info, String viewName) {
        if(viewResolver != null) {
            View v;
            try {

                // execute pre handler interceptors
                for (HandlerInterceptor handlerInterceptor : handlerInterceptors) {
                    if(!handlerInterceptor.preHandle(request, response, this)) return false;
                }

                // execute post handlers directly after, since there is no controller. The filter has a chance to modify the view at this point;
                final ModelAndView modelAndView = new ModelAndView(viewName);
                for (HandlerInterceptor handlerInterceptor : handlerInterceptors) {
                    handlerInterceptor.postHandle(request, response, this, modelAndView);
                }

                v = WebUtils.resolveView(request, info, modelAndView.getViewName(), viewResolver);
                v.render(modelAndView.getModel(), request, response);

                // after completion
                for (HandlerInterceptor handlerInterceptor : handlerInterceptors) {
                    handlerInterceptor.afterCompletion(request, response, this, null);
                }


            } catch (Exception e) {
                GrailsUtil.deepSanitize(e);
                for (HandlerInterceptor handlerInterceptor : handlerInterceptors) {
                    try {
                        handlerInterceptor.afterCompletion(request, response, this, e);
                    }
                    catch (Exception e1) {
                        throw new UrlMappingException("Error executing filter after view error: " + e1.getMessage() + ". Original error: " + e.getMessage(), e1);
                    }
                }
                throw new UrlMappingException("Error mapping onto view ["+viewName+"]: " + e.getMessage(),e);
            }
        }
        return true;
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
