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
package org.codehaus.groovy.grails.web.mapping.filter;

import grails.util.Metadata;
import grails.web.UrlConverter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.cfg.GrailsConfig;
import org.codehaus.groovy.grails.compiler.GrailsProjectWatcher;
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer;
import org.codehaus.groovy.grails.exceptions.StackTraceFilterer;
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.codehaus.groovy.grails.web.mapping.RegexUrlMapping;
import org.codehaus.groovy.grails.web.mapping.UrlMapping;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.mime.MimeType;
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.WrappedResponseHolder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.UrlPathHelper;

/**
 * Uses the Grails UrlMappings to match and forward requests to a relevant controller and action.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public class UrlMappingsFilter extends OncePerRequestFilter {

    public static final boolean WAR_DEPLOYED = Metadata.getCurrent().isWarDeployed();
    private UrlPathHelper urlHelper = new UrlPathHelper();
    private static final Log LOG = LogFactory.getLog(UrlMappingsFilter.class);
    private static final String GSP_SUFFIX = ".gsp";
    private static final String JSP_SUFFIX = ".jsp";
    private HandlerInterceptor[] handlerInterceptors = new HandlerInterceptor[0];
    private GrailsApplication application;
    private GrailsConfig grailsConfig;
    private ViewResolver viewResolver;
    private MimeType[] mimeTypes;
    private StackTraceFilterer filterer;

    private UrlConverter urlConverter;

    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        urlHelper.setUrlDecode(false);
        final ServletContext servletContext = getServletContext();
        final WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        handlerInterceptors = WebUtils.lookupHandlerInterceptors(servletContext);
        application = WebUtils.lookupApplication(servletContext);
        viewResolver = WebUtils.lookupViewResolver(servletContext);
        ApplicationContext mainContext = application.getMainContext();
        urlConverter = mainContext.getBean(UrlConverter.BEAN_NAME, UrlConverter.class);
        if (application != null) {
            grailsConfig = new GrailsConfig(application);
        }

        if (applicationContext.containsBean(MimeType.BEAN_NAME)) {
            mimeTypes = applicationContext.getBean(MimeType.BEAN_NAME, MimeType[].class);
        }
        createStackTraceFilterer();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UrlMappingsHolder holder = WebUtils.lookupUrlMappings(getServletContext());

        String uri = urlHelper.getPathWithinApplication(request);
        if (!"/".equals(uri) && noControllers() && noRegexMappings(holder)) {
            // not index request, no controllers, and no URL mappings for views, so it's not a Grails request
            processFilterChain(request, response, filterChain);
            return;
        }

        if (isUriExcluded(holder, uri)) {
            processFilterChain(request, response, filterChain);
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing URL mapping filter...");
            LOG.debug(holder);
        }

        if (areFileExtensionsEnabled()) {
            String format = WebUtils.getFormatFromURI(uri, mimeTypes);
            if (format != null) {
                MimeType[] configuredMimes = mimeTypes == null ? MimeType.getConfiguredMimeTypes() : mimeTypes;
                // only remove the file extension if it's one of the configured mimes in Config.groovy
                for (MimeType configuredMime : configuredMimes) {
                    if (configuredMime.getExtension().equals(format)) {
                        request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, format);
                        uri = uri.substring(0, (uri.length() - format.length() - 1));
                        break;
                    }
                }
            }
        }

        GrailsWebRequest webRequest = (GrailsWebRequest)request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST);
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
                            String pluginName = info.getPluginName();
                            String featureUri = WebUtils.SLASH + urlConverter.toUrlElement(controllerName) + WebUtils.SLASH + urlConverter.toUrlElement(action);

                            Object featureId = null;
                            if (pluginName != null) {
                                Map featureIdMap = new HashMap();
                                featureIdMap.put("uri", featureUri);
                                featureIdMap.put("pluginName", pluginName);
                                featureId = featureIdMap;
                            } else {
                                featureId = featureUri;
                            }
                            GrailsClass controller = application.getArtefactForFeature(ControllerArtefactHandler.TYPE, featureId);
                            if (controller == null) {
                                continue;
                            }

                            webRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, controller.getLogicalPropertyName(), WebRequest.SCOPE_REQUEST);
                            webRequest.setAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS, controller, WebRequest.SCOPE_REQUEST);
                            webRequest.setAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS_MATCHED_URI, uri, WebRequest.SCOPE_REQUEST);
                        }
                    }
                    catch (Exception e) {
                        if (e instanceof MultipartException) {
                            reapplySitemesh(request);
                            throw ((MultipartException)e);
                        }
                        LOG.error("Error when matching URL mapping [" + info + "]:" + e.getMessage(), e);
                        continue;
                    }

                    dispatched = true;

                    if (!WAR_DEPLOYED) {
                        checkDevelopmentReloadingState(request);
                    }

                    request = checkMultipart(request);

                    if (viewName == null || viewName.endsWith(GSP_SUFFIX) || viewName.endsWith(JSP_SUFFIX)) {
                        if (info.isParsingRequest()) {
                            webRequest.informParameterCreationListeners();
                        }
                        String forwardUrl = WebUtils.forwardRequestForUrlMappingInfo(request, response, info);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Matched URI [" + uri + "] to URL mapping [" + info + "], forwarding to [" + forwardUrl + "] with response [" + response.getClass() + "]");
                        }
                    }
                    else {
                        if (!renderViewForUrlMappingInfo(request, response, info, viewName)) {
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

        if (!dispatched) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No match found, processing remaining filter chain.");
            }
            processFilterChain(request, response, filterChain);
        }
    }

    public static boolean isUriExcluded(UrlMappingsHolder holder, String uri) {
        boolean isExcluded = false;
        @SuppressWarnings("unchecked")
        List<String> excludePatterns = holder.getExcludePatterns();
        if (excludePatterns != null && excludePatterns.size() > 0) {
            for (String excludePattern : excludePatterns) {
                int wildcardLen = 0;
                if (excludePattern.endsWith("**")) {
                    wildcardLen = 2;
                } else if (excludePattern.endsWith("*")) {
                    wildcardLen = 1;
                }
                if (wildcardLen > 0) {
                    excludePattern = excludePattern.substring(0,excludePattern.length() - wildcardLen);
                }
                if ((wildcardLen==0 && uri.equals(excludePattern)) || (wildcardLen > 0 && uri.startsWith(excludePattern))) {
                    isExcluded = true;
                    break;
                }
            }
        }
        return isExcluded;
    }

    private boolean areFileExtensionsEnabled() {
        if (grailsConfig != null) {
            final Boolean value = grailsConfig.get(WebUtils.ENABLE_FILE_EXTENSIONS, Boolean.class);
            if (value != null) {
                return value;
            }
        }
        return true;
    }

    private boolean noRegexMappings(UrlMappingsHolder holder) {
        for (UrlMapping mapping : holder.getUrlMappings()) {
            if (mapping instanceof RegexUrlMapping) {
                return false;
            }
        }
        return true;
    }

    private boolean noControllers() {
        GrailsClass[] controllers = application.getArtefacts(ControllerArtefactHandler.TYPE);
       return controllers == null || controllers.length == 0;
    }

    private void checkDevelopmentReloadingState(HttpServletRequest request) {
        while(GrailsProjectWatcher.isReloadInProgress()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        if (request.getAttribute(GrailsExceptionResolver.EXCEPTION_ATTRIBUTE) != null) return;
        MultipleCompilationErrorsException compilationError = GrailsProjectWatcher.getCurrentCompilationError();
        if (compilationError != null) {
            throw compilationError;
        }
        Throwable currentReloadError = GrailsProjectWatcher.getCurrentReloadError();
        if (currentReloadError != null) {
            throw new RuntimeException(currentReloadError);
        }
    }

    protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
        // Lookup from request attribute. The resolver that handles MultiPartRequest is dealt with earlier inside DefaultUrlMappingInfo with Grails
        HttpServletRequest resolvedRequest = (HttpServletRequest) request.getAttribute(MultipartHttpServletRequest.class.getName());
        if (resolvedRequest != null) return resolvedRequest;
        return request;
    }

    private boolean renderViewForUrlMappingInfo(HttpServletRequest request, HttpServletResponse response, UrlMappingInfo info, String viewName) {
        if (viewResolver != null) {
            View v;
            try {
                // execute pre handler interceptors
                for (HandlerInterceptor handlerInterceptor : handlerInterceptors) {
                    if (!handlerInterceptor.preHandle(request, response, this)) return false;
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
            }
            catch (Throwable e) {
                // let the sitemesh filter re-run for the error
                reapplySitemesh(request);
                for (HandlerInterceptor handlerInterceptor : handlerInterceptors) {
                    try {
                        handlerInterceptor.afterCompletion(request, response, this, e instanceof Exception ? (Exception)e : new GroovyPagesException(e.getMessage(), e));
                    }
                    catch (Exception e1) {
                        UrlMappingException ume = new UrlMappingException("Error executing filter after view error: " + e1.getMessage() + ". Original error: " + e.getMessage(), e1);
                        filterAndThrow(ume);
                    }
                }
                UrlMappingException ume = new UrlMappingException("Error mapping onto view [" + viewName + "]: " + e.getMessage(), e);
                filterAndThrow(ume);
            }
        }
        return true;
    }

    private void filterAndThrow(UrlMappingException ume) {
        filterer.filter(ume, true);
        throw ume;
    }

    private void reapplySitemesh(HttpServletRequest request) {
        request.removeAttribute("com.opensymphony.sitemesh.APPLIED_ONCE");
    }

    private void processFilterChain(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        try {
            WrappedResponseHolder.setWrappedResponse(response);
            if (filterChain != null) {
                filterChain.doFilter(request,response);
            }
        }
        finally {
            WrappedResponseHolder.setWrappedResponse(null);
        }
    }

    protected void createStackTraceFilterer() {
        try {
            filterer = (StackTraceFilterer)GrailsClassUtils.instantiateFromFlatConfig(
                    application.getFlatConfig(), "grails.logging.stackTraceFiltererClass", DefaultStackTraceFilterer.class.getName());
        }
        catch (Throwable t) {
            logger.error("Problem instantiating StackTracePrinter class, using default: " + t.getMessage());
            filterer = new DefaultStackTraceFilterer();
        }
    }
}
