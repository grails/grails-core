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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.DefaultGrailsCodecClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.support.encoding.DefaultEncodingStateRegistry;
import org.codehaus.groovy.grails.support.encoding.Encoder;
import org.codehaus.groovy.grails.support.encoding.EncodingStateRegistry;
import org.codehaus.groovy.grails.support.encoding.EncodingStateRegistryLookup;
import org.codehaus.groovy.grails.web.binding.GrailsDataBinder;
import org.codehaus.groovy.grails.web.pages.FilteringCodecsByContentTypeSettings;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.codehaus.groovy.grails.web.util.WithCodecHelper;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.context.ApplicationContext;
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
public class GrailsWebRequest extends DispatcherServletWebRequest implements ParameterInitializationCallback {

    private GrailsApplicationAttributes attributes;
    private GrailsParameterMap params;
    private GrailsParameterMap originalParams;
    private GrailsHttpSession session;
    private boolean renderView = true;
    private boolean skipFilteringCodec = false;
    private Encoder filteringEncoder;
    public static final String ID_PARAMETER = "id";
    private final List<ParameterCreationListener> parameterCreationListeners = new ArrayList<ParameterCreationListener>();
    private final UrlPathHelper urlHelper = new UrlPathHelper();
    private ApplicationContext applicationContext;
    private String baseUrl;
    private HttpServletResponse wrappedResponse;

    private EncodingStateRegistry encodingStateRegistry;

    public GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, GrailsApplicationAttributes attributes) {
        super(request, response);
        this.attributes = attributes;
        inheritEncodingStateRegistry();
    }

    public GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        super(request, response);
        attributes = new DefaultGrailsApplicationAttributes(servletContext);
        inheritEncodingStateRegistry();
    }

    public GrailsWebRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext, ApplicationContext applicationContext) {
        this(request, response, servletContext);
        this.applicationContext = applicationContext;
    }

    private void inheritEncodingStateRegistry() {
        GrailsWebRequest parentRequest = GrailsWebRequest.lookup(getRequest());
        if(parentRequest != null) {
            this.encodingStateRegistry = parentRequest.getEncodingStateRegistry();
        }
    }

    /**
     * Overriden to return the GrailsParameterMap instance,
     *
     * @return An instance of GrailsParameterMap
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map getParameterMap() {
        if (params == null) {
            resetParams();
        }
        return params;
    }

    @Override
    public void requestCompleted() {
        super.requestCompleted();
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
        return super.isRequestActive();
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
    @Override
    public String getContextPath() {
        final HttpServletRequest request = getCurrentRequest();
        String appUri = (String) request.getAttribute(GrailsApplicationAttributes.APP_URI_ATTRIBUTE);
        if (appUri == null) {
            appUri = urlHelper.getContextPath(request);
        }
        return appUri;
    }

    /**
     * @return The FlashScope instance for the current request
     */
    public FlashScope getFlashScope() {
        return attributes.getFlashScope(getRequest());
    }

    /**
     * @return The currently executing request
     */
    public HttpServletRequest getCurrentRequest() {
        return getRequest();
    }

    public HttpServletResponse getCurrentResponse() {
        if(wrappedResponse != null) {
            return wrappedResponse;
        } else {
            return getResponse();
        }
    }

    public HttpServletResponse getWrappedResponse() {
        return wrappedResponse;
    }

    public void setWrappedResponse(HttpServletResponse wrappedResponse) {
        this.wrappedResponse = wrappedResponse;
    }

    /**
     * @return The Grails params object
     */
    public GrailsParameterMap getParams() {
        if (params == null) {
            resetParams();
        }
        return params;
    }

    /**
     * Reset params by re-reading & initializing parameters from request
     */
    public void resetParams() {
        if (originalParams == null) {
            originalParams = new GrailsParameterMap(getCurrentRequest());
        }
        params = (GrailsParameterMap)originalParams.clone();
    }

    @SuppressWarnings("rawtypes")
    public void addParametersFrom(Map previousParams) {
        if (previousParams instanceof GrailsParameterMap) {
            getParams().addParametersFrom((GrailsParameterMap)previousParams);
        } else {
            for (Object key : previousParams.keySet()) {
                String name = String.valueOf(key);
                getParams().put(name, previousParams.get(key));
            }
        }
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

    public void setControllerNamespace(String controllerNamespace) {
     getCurrentRequest().setAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE, controllerNamespace);
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

    /**
     * @return the controllerClass
     */
    public GrailsControllerClass getControllerClass() {
        return (GrailsControllerClass)getCurrentRequest().getAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS);
    }

    /**
    * @return the controllerNamespace
    */
    public String getControllerNamespace() {
        return (String)getCurrentRequest().getAttribute(GrailsApplicationAttributes.CONTROLLER_NAMESPACE_ATTRIBUTE);
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
        GrailsControllerClass controllerClass = getControllerClass();

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

    public EncodingStateRegistry getEncodingStateRegistry() {
        if (encodingStateRegistry == null) {
            encodingStateRegistry = new DefaultEncodingStateRegistry();
        }
        return encodingStateRegistry;
    }

    private static final class DefaultEncodingStateRegistryLookup implements EncodingStateRegistryLookup {
        public EncodingStateRegistry lookup() {
            GrailsWebRequest webRequest = GrailsWebRequest.lookup();
            return webRequest == null ? null : webRequest.getEncodingStateRegistry();
        }
    }

    static {
        DefaultGrailsCodecClass.setEncodingStateRegistryLookup(new DefaultEncodingStateRegistryLookup());
    }

    /**
     * @return true if grails.views.filteringCodecForMimeType settings should be ignored for this request
     */
    public boolean isSkipFilteringCodec() {
        return skipFilteringCodec;
    }

    public void setSkipFilteringCodec(boolean skipCodec) {
        this.skipFilteringCodec = skipCodec;
    }

    public String getFilteringCodec() {
        return filteringEncoder != null ? filteringEncoder.getCodecIdentifier().getCodecName() : null;
    }
    public void setFilteringCodec(String codecName) {
        filteringEncoder=codecName != null ? WithCodecHelper.lookupEncoder(attributes.getGrailsApplication(), codecName) : null;
    }

    public Encoder lookupFilteringEncoder() {
        if (filteringEncoder == null && applicationContext != null && applicationContext.containsBean(FilteringCodecsByContentTypeSettings.BEAN_NAME)) {
            filteringEncoder = applicationContext.getBean(FilteringCodecsByContentTypeSettings.BEAN_NAME, FilteringCodecsByContentTypeSettings.class).getEncoderForContentType(getResponse().getContentType());
        }
        return filteringEncoder;
    }

    public Encoder getFilteringEncoder() {
        return filteringEncoder;
    }

    public void setFilteringEncoder(Encoder filteringEncoder) {
        this.filteringEncoder = filteringEncoder;
    }
}
