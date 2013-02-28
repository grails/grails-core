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
package org.codehaus.groovy.grails.web.servlet.mvc;

import grails.validation.DeferredBindingActions;

import java.io.IOException;
import java.io.Writer;
import java.security.Principal;
import java.util.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.web.binding.GrailsDataBinder;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.portlet.MockClientDataRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * Encapsulates a Grails request. An instance of this class is bound to the current thread using
 * Spring's RequestContextHolder which can later be retrieved using:
 *
 * def webRequest = RequestContextHolder.currentRequestAttributes()
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsWebRequest implements ParameterInitializationCallback, NativeWebRequest,RequestAttributes {

    private GrailsApplicationAttributes attributes;
    private GrailsParameterMap params;
    private final HttpServletResponse response;
    private GrailsHttpSession session;
    private boolean renderView = true;
    public static final String ID_PARAMETER = "id";
    private final List<ParameterCreationListener> parameterCreationListeners = new ArrayList<ParameterCreationListener>();
    private final UrlPathHelper urlHelper = new UrlPathHelper();
    private ApplicationContext applicationContext;
    private String baseUrl;

    private DispatcherServletWebRequest targetWebRequest;
    private boolean active;

    public GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        targetWebRequest = new DispatcherServletWebRequest(request, response);
        attributes = new DefaultGrailsApplicationAttributes(servletContext);
        this.response = response;
    }

    public GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ApplicationContext applicationContext) {
        this(request, response, servletContext);
        this.applicationContext = applicationContext;
    }

    public String getHeader(String headerName) {
        return targetWebRequest.getHeader(headerName);
    }

    public String[] getHeaderValues(String headerName) {
        return targetWebRequest.getHeaderValues(headerName);
    }

    public Iterator<String> getHeaderNames() {
        return targetWebRequest.getHeaderNames();
    }

    public String getParameter(String paramName) {
        return targetWebRequest.getParameter(paramName);
    }

    public String[] getParameterValues(String paramName) {
        return targetWebRequest.getParameterValues(paramName);
    }

    public Iterator<String> getParameterNames() {
        return targetWebRequest.getParameterNames();
    }

    /**
     * Overriden to return the GrailsParameterMap instance,
     *
     * @return An instance of GrailsParameterMap
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map getParameterMap() {
        if (params == null) {
            params = new GrailsParameterMap(getCurrentRequest());
        }
        return params;
    }

    public Locale getLocale() {
        return targetWebRequest.getLocale();
    }

    public void requestCompleted() {
        this.active = false;
        targetWebRequest.requestCompleted();
        DeferredBindingActions.clear();
    }

    /**
     * @return the out
     */
    public Writer getOut() {
        Writer out = attributes.getOut(getCurrentRequest());
        if (out == null) {
            try {
                return getCurrentResponse().getWriter();
            } catch (IOException e) {
                throw new ControllerExecutionException("Error retrieving response writer: " + e.getMessage(), e);
            }
        }
        return out;
    }

    /**
     * Whether the web request is still active
     * @return true if it is
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param out the out to set
     */
    public void setOut(Writer out) {
        attributes.setOut(getCurrentRequest(), out);
    }

    /**
     * @return The ServletContext instance
     */
    public ServletContext getServletContext() {
        return attributes.getServletContext();
    }

    /**
     * Returns the context path of the request.
     * @return the path
     */
    public String getContextPath() {
        final HttpServletRequest request = getCurrentRequest();
        String appUri = (String) request.getAttribute(GrailsApplicationAttributes.APP_URI_ATTRIBUTE);
        if (appUri == null) {
            appUri = urlHelper.getContextPath(request);
        }
        return appUri;
    }

    public String getRemoteUser() {
        return targetWebRequest.getRemoteUser();
    }

    public Principal getUserPrincipal() {
        return targetWebRequest.getUserPrincipal();
    }

    public boolean isUserInRole(String role) {
        return targetWebRequest.isUserInRole(role);
    }

    public boolean isSecure() {
        return targetWebRequest.isSecure();
    }

    public boolean checkNotModified(long lastModifiedTimestamp) {
        return targetWebRequest.checkNotModified(lastModifiedTimestamp);
    }

    public boolean checkNotModified(String eTag) {
        return targetWebRequest.checkNotModified(eTag);
    }

    public String getDescription(boolean includeClientInfo) {
        return targetWebRequest.getDescription(includeClientInfo);
    }

    /**
     * @return The FlashScope instance for the current request
     */
    public FlashScope getFlashScope() {
        return attributes.getFlashScope(targetWebRequest.getRequest());
    }

    /**
     * @return The currently executing request
     */
    public HttpServletRequest getCurrentRequest() {
        return targetWebRequest.getRequest();
    }

    public HttpServletResponse getCurrentResponse() {
        return targetWebRequest.getResponse();
    }

    public HttpServletResponse getResponse() {
        return targetWebRequest.getResponse();
    }

    /**
     * @return The Grails params object
     */
    public GrailsParameterMap getParams() {
        if (params == null) {
            params = new GrailsParameterMap(getCurrentRequest());
        }
        return params;
    }

    /**
     * Informs any parameter creation listeners.
     */
    public void informParameterCreationListeners() {
        for (ParameterCreationListener parameterCreationListener : parameterCreationListeners) {
            parameterCreationListener.paramsCreated(getParams());
        }
    }

    /**
     * @return The Grails session object
     */
    public GrailsHttpSession getSession() {
        if (session == null) {
            session = new GrailsHttpSession(getCurrentRequest());
        }

        return session;
    }

    /**
     * @return The GrailsApplicationAttributes instance
     */
    public GrailsApplicationAttributes getAttributes() {
        return attributes;
    }

    public void setActionName(String actionName) {
        getCurrentRequest().setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, actionName);
    }

    public void setControllerName(String controllerName) {
        getCurrentRequest().setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, controllerName);
    }

    /**
     * @return the actionName
     */
    public String getActionName() {
        return (String)getCurrentRequest().getAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE);
    }

    /**
     * @return the controllerName
     */
    public String getControllerName() {
        return (String)getCurrentRequest().getAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE);
    }

    public void setRenderView(boolean renderView) {
        this.renderView = renderView;
    }

    /**
     * @return true if the view for this GrailsWebRequest should be rendered
     */
    public boolean isRenderView() {
        return renderView;
    }

    public String getId() {
        Object id = getParams().get(ID_PARAMETER);
        return id == null ? null : id.toString();
    }

    /**
     * Returns true if the current executing request is a flow request
     *
     * @return true if it is a flow request
     */
    public boolean isFlowRequest() {
        GrailsApplication application = getAttributes().getGrailsApplication();
        GrailsControllerClass controllerClass = (GrailsControllerClass)application.getArtefactByLogicalPropertyName(
                ControllerArtefactHandler.TYPE, getControllerName());

        if (controllerClass == null) return false;

        String actionName = getActionName();
        if (actionName == null) actionName = controllerClass.getDefaultAction();
        if (actionName == null) return false;

        if (controllerClass != null && controllerClass.isFlowAction(actionName)) return true;
        return false;
    }

    public void addParameterListener(ParameterCreationListener creationListener) {
        parameterCreationListeners.add(creationListener);
    }

    /**
     * Obtains the ApplicationContext object.
     *
     * @return The ApplicationContext
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext == null ? getAttributes().getApplicationContext() : applicationContext;
    }

    /**
     * Obtains the PropertyEditorRegistry instance.
     * @return The PropertyEditorRegistry
     */
    public PropertyEditorRegistry getPropertyEditorRegistry() {
        final HttpServletRequest servletRequest = getCurrentRequest();
        PropertyEditorRegistry registry = (PropertyEditorRegistry) servletRequest.getAttribute(GrailsApplicationAttributes.PROPERTY_REGISTRY);
        if (registry == null) {
            registry = new PropertyEditorRegistrySupport();
            GrailsDataBinder.registerCustomEditors(this, registry, RequestContextUtils.getLocale(servletRequest));
            servletRequest.setAttribute(GrailsApplicationAttributes.PROPERTY_REGISTRY, registry);
        }
        return registry;
    }

    /**
     * Looks up the GrailsWebRequest from the current request.
     * @param request The current request
     * @return The GrailsWebRequest
     */
    public static GrailsWebRequest lookup(HttpServletRequest request) {
        GrailsWebRequest webRequest = (GrailsWebRequest) request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST);
        return webRequest == null ? lookup() : webRequest;
    }

    /**
     * Looks up the current Grails WebRequest instance
     * @return The GrailsWebRequest instance
     */
    public static GrailsWebRequest lookup() {
        GrailsWebRequest webRequest = null;
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof GrailsWebRequest) {
            webRequest = (GrailsWebRequest) requestAttributes;
        }
        return webRequest;
    }

    /**
     * Looks up the GrailsApplication from the current request.

     * @return The GrailsWebRequest
     */
    public static GrailsApplication lookupApplication() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof GrailsWebRequest) {
            return ((GrailsWebRequest) requestAttributes).getAttributes().getGrailsApplication();
        }
        return null;
    }

    /**
     * Sets the id of the request.
     * @param id The id
     */
    public void setId(Object id) {
        getParams().put(GrailsWebRequest.ID_PARAMETER, id);
    }

    public String getBaseUrl() {
        if (baseUrl == null) {
            HttpServletRequest request=getCurrentRequest();
            String scheme =request.getScheme();
            StringBuilder sb=new StringBuilder();
            sb.append(scheme).append("://").append(request.getServerName());
            int port = request.getServerPort();
            if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
                sb.append(":").append(port);
            }
            String contextPath = request.getContextPath();
            if (contextPath != null) {
                sb.append(contextPath);
            }
            baseUrl = sb.toString();
        }
        return baseUrl;
    }

    public Object getNativeRequest() {
        return targetWebRequest.getNativeRequest();
    }

    public Object getNativeResponse() {
        return targetWebRequest.getNativeResponse();
    }

    public <T> T getNativeRequest(Class<T> requiredType) {
        return targetWebRequest.getNativeRequest(requiredType);
    }

    public <T> T getNativeResponse(Class<T> requiredType) {
        return targetWebRequest.getNativeResponse(requiredType);
    }

    public Object getAttribute(String name, int scope) {
        return targetWebRequest.getAttribute(name, scope);
    }

    public void setAttribute(String name, Object value, int scope) {
        targetWebRequest.setAttribute(name, value, scope);
    }

    public void removeAttribute(String name, int scope) {
        targetWebRequest.removeAttribute(name, scope);
    }

    public String[] getAttributeNames(int scope) {
        return targetWebRequest.getAttributeNames(scope);
    }

    public void registerDestructionCallback(String name, Runnable callback, int scope) {
        targetWebRequest.registerDestructionCallback(name, callback, scope);
    }

    public Object resolveReference(String key) {
        return targetWebRequest.resolveReference(key);
    }

    public String getSessionId() {
        return targetWebRequest.getSessionId();
    }

    public Object getSessionMutex() {
        return targetWebRequest.getSessionMutex();
    }

    public HttpServletRequest getRequest() {
        return targetWebRequest.getRequest();
    }
}
