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

import grails.util.GrailsUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.mime.MimeType;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsUrlPathHelper;
import org.codehaus.groovy.grails.web.servlet.WrappedResponseHolder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.CharacterCodingException;
import java.util.*;

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
    private static final String DISPATCH_URI_SUFFIX = ".dispatch";
    private static final String GRAILS_DISPATCH_SERVLET_NAME = "/grails";

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
     * Looks up all of the HandlerInterceptor instances registered for the application
     *
     * @param servletContext The ServletContext instance
     * @return An array of HandlerInterceptor instances
     */
    public static HandlerInterceptor[] lookupHandlerInterceptors(ServletContext servletContext) {
        WebApplicationContext wac =
                WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

        final Collection<HandlerInterceptor> allHandlerInterceptors = new ArrayList<HandlerInterceptor>();

        WebRequestInterceptor[] webRequestInterceptors = lookupWebRequestInterceptors(servletContext);
        for (WebRequestInterceptor webRequestInterceptor : webRequestInterceptors) {
            allHandlerInterceptors.add(new WebRequestHandlerInterceptorAdapter(webRequestInterceptor));
        }
        final Collection<HandlerInterceptor> handlerInterceptors = wac.getBeansOfType(HandlerInterceptor.class).values();

        allHandlerInterceptors.addAll(handlerInterceptors);
        return allHandlerInterceptors.toArray(new HandlerInterceptor[allHandlerInterceptors.size()]);
    }

    /**
     * Looks up all of the WebRequestInterceptor instances registered with the application
     *
     * @param servletContext The ServletContext instance
     * @return An array of WebRequestInterceptor instances
     */
    public static WebRequestInterceptor[] lookupWebRequestInterceptors(ServletContext servletContext) {
        WebApplicationContext wac =
                WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);

        final Collection webRequestInterceptors = wac.getBeansOfType(WebRequestInterceptor.class).values();
        return (WebRequestInterceptor[]) webRequestInterceptors.toArray(new WebRequestInterceptor[webRequestInterceptors.size()]);

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
     * The Grails dispatch servlet maps URIs like /app/grails/example/index.dispatch. This method infers the
     * controller URI for the dispatch URI so that /app/grails/example/index.dispatch becomes /app/example/index
     *
     * @param request The request
     */
    public static String getRequestURIForGrailsDispatchURI(HttpServletRequest request) {
        UrlPathHelper pathHelper = new UrlPathHelper();
        if(request.getRequestURI().endsWith(DISPATCH_URI_SUFFIX))  {
            String path = pathHelper.getPathWithinApplication(request);
            if(path.startsWith(GRAILS_DISPATCH_SERVLET_NAME)) {
                path = path.substring(GRAILS_DISPATCH_SERVLET_NAME.length(),path.length());
            }
            return path.substring(0, path.length()-DISPATCH_URI_SUFFIX.length());

        }
        else {
            return pathHelper.getPathWithinApplication(request);
        }
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
            StringBuilder buf = new StringBuilder();
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
        return buildDispatchUrlForMapping(info, false);
    }

    private static String buildDispatchUrlForMapping(UrlMappingInfo info, boolean includeParams) {
    	if(info.getURI()!=null) {
    		return info.getURI();
    	}
    	else {
            final StringBuilder forwardUrl = new StringBuilder();

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

            final Map parameters = info.getParameters();
            if(parameters !=null && !parameters.isEmpty()  && includeParams) {
                try {
                    forwardUrl.append(toQueryString(parameters));
                }
                catch (UnsupportedEncodingException e) {
                    throw new ControllerExecutionException("Unable to include ");
                }
            }
            return forwardUrl.toString();
    		
    	}
    }


    /**
     * @see org.codehaus.groovy.grails.web.util.WebUtils#forwardRequestForUrlMappingInfo(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.codehaus.groovy.grails.web.mapping.UrlMappingInfo, java.util.Map)
     */
    public static String forwardRequestForUrlMappingInfo(HttpServletRequest request, HttpServletResponse response, UrlMappingInfo info) throws ServletException, IOException {
        return forwardRequestForUrlMappingInfo(request, response, info, Collections.EMPTY_MAP);
    }


    /**
     * @see #forwardRequestForUrlMappingInfo(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.codehaus.groovy.grails.web.mapping.UrlMappingInfo, java.util.Map, boolean)
     */
    public static String forwardRequestForUrlMappingInfo(HttpServletRequest request, HttpServletResponse response, UrlMappingInfo info, Map model) throws ServletException, IOException {
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
     * @throws ServletException Thrown when an error occurs executing the forward
     * @throws IOException Thrown when an error occurs executing the forward
     */
    public static String forwardRequestForUrlMappingInfo(HttpServletRequest request, HttpServletResponse response, UrlMappingInfo info, Map model, boolean includeParams) throws ServletException, IOException {
        String forwardUrl = buildDispatchUrlForMapping(info, includeParams);

        //populateParamsForMapping(info);
        RequestDispatcher dispatcher = request.getRequestDispatcher(forwardUrl);

        exposeForwardRequestAttributes(request);
        exposeRequestAttributes(request, model);
        dispatcher.forward(request, response);
        return forwardUrl;
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
    public static IncludedContent includeForUrlMappingInfo(HttpServletRequest request, HttpServletResponse response, UrlMappingInfo info, Map model) {
        String includeUrl = buildDispatchUrlForMapping(info, true);

        final GrailsWebRequest webRequest = GrailsWebRequest.lookup(request);

        String currentController = null;
        String currentAction = null;
        String currentId = null;
        ModelAndView currentMv = null;
        Map currentParams = null;
        if (webRequest!=null) {
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
            	webRequest.getParameterMap().clear();
            	webRequest.getParameterMap().putAll(currentParams);
                webRequest.setId(currentId);
                webRequest.setControllerName(currentController);
                webRequest.setActionName(currentAction);
                if(currentMv != null) {
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
    public static IncludedContent includeForUrl(String includeUrl, HttpServletRequest request, HttpServletResponse response, Map model) {
        RequestDispatcher dispatcher = request.getRequestDispatcher(includeUrl);
        HttpServletResponse wrapped = WrappedResponseHolder.getWrappedResponse();
        response = wrapped != null ? wrapped : response;


        exposeForwardRequestAttributes(request);
        exposeRequestAttributes(request, model);


        try {
            final IncludeResponseWrapper responseWrapper = new IncludeResponseWrapper(response);
            try {
                WrappedResponseHolder.setWrappedResponse(responseWrapper);
                dispatcher.include(request, responseWrapper);
                if(responseWrapper.getRedirectURL()!=null) {
                    return new IncludedContent(responseWrapper.getRedirectURL());
                }
                else {
                    return new IncludedContent(responseWrapper.getContentType(), responseWrapper.getContent());
                }
            }
            finally {
                WrappedResponseHolder.setWrappedResponse(wrapped);

            }
        }
        catch (Exception e) {
            GrailsUtil.deepSanitize(e);
            throw new ControllerExecutionException("Unable to execute include: " + e.getMessage(),e );
        }
    }

    /**
     * Converts the given params into a query string started with ?
     * @param params The params
     * @param encoding The encoding to use
     * @return The query string
     * @throws UnsupportedEncodingException If the given encoding is not supported
     */
    public static String toQueryString(Map params, String encoding) throws UnsupportedEncodingException {
        if(encoding == null) encoding = "UTF-8";
        StringBuilder queryString = new StringBuilder("?");

        for (Iterator i = params.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            boolean hasMore = i.hasNext();
            boolean wasAppended = appendEntry(entry, queryString, encoding, "");
            if(hasMore && wasAppended) queryString.append('&');
        }
        return queryString.toString();
    }

    /**
     * Converts the given parameters to a query string using the default  UTF-8 encoding
     * @param parameters The parameters
     * @return The query string
     * @throws UnsupportedEncodingException If UTF-8 encoding is not supported
     */
    public static String toQueryString(Map parameters) throws UnsupportedEncodingException {
        return toQueryString(parameters, "UTF-8");
    }

    private static boolean appendEntry(Map.Entry entry, StringBuilder queryString, String encoding, String path) throws UnsupportedEncodingException {
        String name = entry.getKey().toString();
        if(name.indexOf(".") > -1) return false; // multi-d params handled by recursion
        Object value = entry.getValue();
        if(value == null) value = "";
        else if(value instanceof GrailsParameterMap) {
            GrailsParameterMap child = (GrailsParameterMap)value;
            Set nestedEntrySet = child.entrySet();
            for (Object aNestedEntrySet : nestedEntrySet) {
                Map.Entry childEntry = (Map.Entry) aNestedEntrySet;
                appendEntry(childEntry, queryString, encoding, entry.getKey().toString() + '.');
            }
        }
        else {
                queryString.append(URLEncoder.encode(path+name, encoding))
                        .append('=')
                        .append(URLEncoder.encode(value.toString(), encoding));
        }
        return true;
    }




    /**
     * Obtains the format from the URI. The format is the string following the . file extension in the last token of the URI.
     * If nothing comes after the ".", this method assumes that there is no format and returns <code>null</code>.
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
            if(idx > -1 && idx != lastToken.length() - 1) {
                String extension =  lastToken.substring(idx+1, lastToken.length());
                MimeType[] mimeTypes = MimeType.getConfiguredMimeTypes();
                for (MimeType mimeType : mimeTypes) {
                    if (mimeType.getExtension().equals(extension)) return extension;
                }
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

    /**
     * Obtains the forwardURI from the request, since Grails uses a forwarding technique for URL mappings. The actual
     * request URI is held within a request attribute
     *
     * @param request The request
     * @return The forward URI
     */
    public static String getForwardURI(HttpServletRequest request) {
        String result = (String) request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
        if(StringUtils.isBlank(result)) result = request.getRequestURI();
        return result;
    }
}
