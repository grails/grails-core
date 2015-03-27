/*
 * Copyright 2014 original authors
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
package org.grails.web.mapping;

import grails.util.GrailsStringUtils;
import grails.web.CamelCaseUrlConverter;
import grails.web.UrlConverter;
import grails.web.mapping.UrlMappingInfo;
import grails.web.mapping.UrlMappingsHolder;
import groovy.lang.Binding;
import org.grails.web.mapping.mvc.UrlMappingsHandlerMapping;
import org.grails.web.util.GrailsApplicationAttributes;
import org.grails.web.servlet.WrappedResponseHolder;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.grails.web.util.IncludeResponseWrapper;
import org.grails.web.util.IncludedContent;
import org.grails.web.util.WebUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for working with UrlMappings
 *
 * @author Graeme Rocher
 * @since 2.4
 */
public class UrlMappingUtils {
    /**
     * Looks up the UrlMappingsHolder instance
     *
     * @return The UrlMappingsHolder
     * @param servletContext The ServletContext object
     */
    public static UrlMappingsHolder lookupUrlMappings(ServletContext servletContext) {
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        return (UrlMappingsHolder)wac.getBean(UrlMappingsHolder.BEAN_ID);
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
        return WebUtils.resolveView(request, viewName, controllerName, viewResolver);
    }

    /**
     * Constructs the URI to forward to using the given request and UrlMappingInfo instance
     *
     * @param info The UrlMappingInfo
     * @return The URI to forward to
     */
    public static String buildDispatchUrlForMapping(UrlMappingInfo info) {
        return buildDispatchUrlForMapping(info, false);
    }

    @SuppressWarnings("rawtypes")
    private static String buildDispatchUrlForMapping(UrlMappingInfo info, boolean includeParams) {
        if (info.getURI() != null) {
            return info.getURI();
        }

        final StringBuilder forwardUrl = new StringBuilder();

        if (info.getViewName() != null) {
            String viewName = info.getViewName();
            if (viewName.startsWith("/")) {
                forwardUrl.append(viewName);
            }
            else {
                forwardUrl.append(WebUtils.SLASH).append(viewName);
            }
        }
        else {
            forwardUrl.append(WebUtils.SLASH).append(info.getControllerName());

            if (!GrailsStringUtils.isBlank(info.getActionName())) {
                forwardUrl.append(WebUtils.SLASH).append(info.getActionName());
            }
        }

        final Map parameters = info.getParameters();
        if (parameters != null && !parameters.isEmpty() && includeParams) {
            try {
                forwardUrl.append(WebUtils.toQueryString(parameters));
            }
            catch (UnsupportedEncodingException e) {
                throw new ControllerExecutionException("Unable to include ");
            }
        }
        return forwardUrl.toString();
    }

    /**
     * @see #forwardRequestForUrlMappingInfo(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, grails.web.mapping.UrlMappingInfo, java.util.Map)
     */
    public static String forwardRequestForUrlMappingInfo(HttpServletRequest request,
            HttpServletResponse response, UrlMappingInfo info) throws ServletException, IOException {
        return forwardRequestForUrlMappingInfo(request, response, info, Collections.EMPTY_MAP);
    }

    /**
     * @see #forwardRequestForUrlMappingInfo(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, grails.web.mapping.UrlMappingInfo, java.util.Map, boolean)
     */
    @SuppressWarnings("rawtypes")
    public static String forwardRequestForUrlMappingInfo(HttpServletRequest request,
            HttpServletResponse response, UrlMappingInfo info, Map model) throws ServletException, IOException {
        return forwardRequestForUrlMappingInfo(request, response, info, model, false);
    }

    /**
     * Forwards a request for the given UrlMappingInfo object and model
     *
     * @param request The request
     * @param response The response
     * @param info The UrlMappingInfo object
     * @param model The Model
     * @param includeParams Whether to include any request parameters
     * @return The URI forwarded too
     *
     * @throws javax.servlet.ServletException Thrown when an error occurs executing the forward
     * @throws java.io.IOException Thrown when an error occurs executing the forward
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String forwardRequestForUrlMappingInfo(HttpServletRequest request,
            HttpServletResponse response, UrlMappingInfo info, Map model, boolean includeParams) throws ServletException, IOException {
        org.springframework.web.util.WebUtils.exposeRequestAttributes(request, model);

        String forwardUrl = buildDispatchUrlForMapping(info, includeParams);

        //populateParamsForMapping(info);
        RequestDispatcher dispatcher = request.getRequestDispatcher(forwardUrl);

        // Clear the request attributes that affect view rendering. Otherwise
        // whatever we forward to may render the wrong thing! Note that we
        // don't care about the return value because we're delegating
        // responsibility for rendering the response.
        final GrailsWebRequest webRequest = GrailsWebRequest.lookup(request);
        webRequest.removeAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0);
        info.configure(webRequest);
        webRequest.removeAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS_AVAILABLE, WebRequest.SCOPE_REQUEST);
        webRequest.removeAttribute(UrlMappingsHandlerMapping.MATCHED_REQUEST, WebRequest.SCOPE_REQUEST);
        webRequest.removeAttribute("grailsWebRequestFilter" + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, WebRequest.SCOPE_REQUEST);
        dispatcher.forward(request, response);
        return forwardUrl;
    }

    private static UrlConverter locateUrlConverter(final GrailsWebRequest webRequest) {
        UrlConverter urlConverter = null;
        try {
            urlConverter = webRequest.getAttributes().getApplicationContext().getBean("grailsUrlConverter", UrlConverter.class);
        } catch (NoSuchBeanDefinitionException e) {
            urlConverter = new CamelCaseUrlConverter();
        }
        return urlConverter;
    }

    /**
     * Include whatever the given UrlMappingInfo maps to within the current response
     *
     * @param request The request
     * @param response The response
     * @param info The UrlMappingInfo
     * @param model The model
     *
     * @return The included content
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static IncludedContent includeForUrlMappingInfo(HttpServletRequest request,
            HttpServletResponse response, UrlMappingInfo info, Map model) {
        String includeUrl = buildDispatchUrlForMapping(info, true);

        final GrailsWebRequest webRequest = GrailsWebRequest.lookup(request);

        String currentController = null;
        String currentAction = null;
        String currentId = null;
        ModelAndView currentMv = null;
        Binding currentPageBinding = null;
        Map currentParams = null;
        Object currentLayoutAttribute = null;
        Object currentRenderingView = null;
        if (webRequest != null) {
            currentPageBinding = (Binding) webRequest.getAttribute(GrailsApplicationAttributes.PAGE_SCOPE, 0);
            webRequest.removeAttribute(GrailsApplicationAttributes.PAGE_SCOPE, 0);
            currentLayoutAttribute = webRequest.getAttribute(WebUtils.LAYOUT_ATTRIBUTE, 0);
            if (currentLayoutAttribute != null) {
                webRequest.removeAttribute(WebUtils.LAYOUT_ATTRIBUTE, 0);
            }
            currentRenderingView = webRequest.getAttribute(WebUtils.RENDERING_VIEW, 0);
            if (currentRenderingView != null) {
                webRequest.removeAttribute(WebUtils.RENDERING_VIEW, 0);
            }
            currentController = webRequest.getControllerName();
            currentAction = webRequest.getActionName();
            currentId = webRequest.getId();
            currentParams = new HashMap();
            currentParams.putAll(webRequest.getParameterMap());
            currentMv = (ModelAndView)webRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0);
        }
        try {
            if (webRequest!=null) {
                webRequest.getParameterMap().clear();
                info.configure(webRequest);
                webRequest.getParameterMap().putAll(info.getParameters());
                webRequest.removeAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0);
            }
            return includeForUrl(includeUrl, request, response, model);
        }
        finally {
            if (webRequest!=null) {
                webRequest.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE,currentPageBinding, 0);
                if (currentLayoutAttribute != null) {
                    webRequest.setAttribute(WebUtils.LAYOUT_ATTRIBUTE, currentLayoutAttribute, 0);
                }
                if (currentRenderingView != null) {
                    webRequest.setAttribute(WebUtils.RENDERING_VIEW, currentRenderingView, 0);
                }
                webRequest.getParameterMap().clear();
                webRequest.getParameterMap().putAll(currentParams);
                webRequest.setId(currentId);
                webRequest.setControllerName(currentController);
                webRequest.setActionName(currentAction);
                if (currentMv != null) {
                    webRequest.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, currentMv, 0);
                }
            }
        }
    }

    /**
     * Includes the given URL returning the resulting content as a String
     *
     * @param includeUrl The URL to include
     * @param request The request
     * @param response The response
     * @param model The model
     * @return The content
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static IncludedContent includeForUrl(String includeUrl, HttpServletRequest request,
            HttpServletResponse response, Map model) {
        RequestDispatcher dispatcher = request.getRequestDispatcher(includeUrl);
        HttpServletResponse wrapped = WrappedResponseHolder.getWrappedResponse();
        response = wrapped != null ? wrapped : response;

        WebUtils.exposeIncludeRequestAttributes(request);

        Map toRestore = WebUtils.exposeRequestAttributesAndReturnOldValues(request, model);

        final GrailsWebRequest webRequest = GrailsWebRequest.lookup(request);


        final Object previousControllerClass = webRequest.getAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS_AVAILABLE, WebRequest.SCOPE_REQUEST);
        final Object previousMatchedRequest = webRequest.getAttribute(UrlMappingsHandlerMapping.MATCHED_REQUEST, WebRequest.SCOPE_REQUEST);



        try {
            webRequest.removeAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS_AVAILABLE, WebRequest.SCOPE_REQUEST);
            webRequest.removeAttribute(UrlMappingsHandlerMapping.MATCHED_REQUEST, WebRequest.SCOPE_REQUEST);
            webRequest.removeAttribute("grailsWebRequestFilter" + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, WebRequest.SCOPE_REQUEST);
            final IncludeResponseWrapper responseWrapper = new IncludeResponseWrapper(response);
            try {
                WrappedResponseHolder.setWrappedResponse(responseWrapper);
                dispatcher.include(request, responseWrapper);
                if (responseWrapper.getRedirectURL()!=null) {
                    return new IncludedContent(responseWrapper.getRedirectURL());
                }
                return new IncludedContent(responseWrapper.getContentType(), responseWrapper.getContent());
            }
            finally {
                webRequest.setAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS_AVAILABLE, previousControllerClass,WebRequest.SCOPE_REQUEST);
                webRequest.setAttribute(UrlMappingsHandlerMapping.MATCHED_REQUEST,previousMatchedRequest, WebRequest.SCOPE_REQUEST);

                WrappedResponseHolder.setWrappedResponse(wrapped);
            }
        }
        catch (Exception e) {
            throw new ControllerExecutionException("Unable to execute include: " + e.getMessage(), e);
        }
        finally {
            WebUtils.cleanupIncludeRequestAttributes(request, toRestore);
        }
    }





}
