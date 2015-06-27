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
package org.grails.web.mapping;

import grails.util.GrailsNameUtils;
import grails.web.CamelCaseUrlConverter;
import grails.web.UrlConverter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import grails.web.mapping.UrlMapping;
import grails.web.mapping.UrlMappingData;
import grails.core.GrailsApplication;
import grails.web.mapping.exceptions.UrlMappingException;
import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.util.WebUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Holds information established from a matched URL.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public class DefaultUrlMappingInfo extends AbstractUrlMappingInfo {

    private static final String CONTROLLER_PREFIX = "controller:";
    private static final String ACTION_PREFIX = "action:";
    private static final String PLUGIN_PREFIX = "plugin:";
    private static final String NAMESPACE_PREFIX = "namespace:";
    private static final String ID_PREFIX = "id:";
    private static final String VIEW_PREFIX = "view:";
    private static final String METHOD_PREFIX = "method:";
    private static final String VERSION_PREFIX = "version:";
    private Object controllerName;
    private Object actionName;
    private Object pluginName;
    private Object namespace;
    private Object redirectInfo;
    private Object id;
    private static final String ID_PARAM = "id";
    private UrlMappingData urlData;
    private Object viewName;
    private ServletContext servletContext;
    private static final String SETTING_GRAILS_WEB_DISABLE_MULTIPART = "grails.web.disable.multipart";
    private boolean parsingRequest;
    private Object uri;
    private UrlConverter urlConverter;
    private String httpMethod;
    private String version;

    @SuppressWarnings("rawtypes")
    private DefaultUrlMappingInfo(Map params, UrlMappingData urlData, ServletContext servletContext) {
        setParams(params);
        id = getParams().get(ID_PARAM);
        this.urlData = urlData;
        this.servletContext = servletContext;
        ApplicationContext applicationContext = null;
        if(servletContext != null) {

            applicationContext = WebUtils.findApplicationContext(servletContext);
        }
        if(applicationContext != null && applicationContext.containsBean(UrlConverter.BEAN_NAME)) {
            urlConverter = applicationContext.getBean(UrlConverter.BEAN_NAME, UrlConverter.class);
        }
        else {
            urlConverter = new CamelCaseUrlConverter();
        }
    }

    @SuppressWarnings("rawtypes")
    public DefaultUrlMappingInfo(Object redirectInfo, Object controllerName, Object actionName, Object namespace, Object pluginName, Object viewName, Map params,
            UrlMappingData urlData, ServletContext servletContext) {
        this(redirectInfo, controllerName, actionName, namespace, pluginName, viewName, null, UrlMapping.ANY_VERSION, params, urlData, servletContext);
    }

    public DefaultUrlMappingInfo(Object redirectInfo, Object controllerName, Object actionName, Object namespace, Object pluginName, Object viewName,
                                 String httpMethod, String version, Map<?, ?> params, UrlMappingData urlData, ServletContext servletContext) {
        this(params, urlData, servletContext);
        Assert.isTrue(redirectInfo != null || controllerName != null || viewName != null, "URL mapping must either provide redirect information, a controller or a view name to map to!");
        Assert.notNull(params, "Argument [params] cannot be null");
        this.controllerName = controllerName;
        this.actionName = actionName;
        this.pluginName = pluginName;
        this.namespace = namespace;
        this.httpMethod = httpMethod;
        this.version = version;
        this.redirectInfo = redirectInfo;
        if (actionName == null) {
            this.viewName = viewName;
        }
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @SuppressWarnings("rawtypes")
    public DefaultUrlMappingInfo(Object viewName, Map params, UrlMappingData urlData, ServletContext servletContext) {
        this(params, urlData, servletContext);
        this.viewName = viewName;
        Assert.notNull(viewName, "Argument [viewName] cannot be null or blank");
    }

    public DefaultUrlMappingInfo(Object uri, UrlMappingData data, ServletContext servletContext) {
        this(Collections.EMPTY_MAP, data, servletContext);
        this.uri = uri;
        Assert.notNull(uri, "Argument [uri] cannot be null or blank");
    }
    public DefaultUrlMappingInfo(Object uri,String httpMethod, UrlMappingData data, ServletContext servletContext) {
        this(Collections.EMPTY_MAP, data, servletContext);
        this.uri = uri;
        this.httpMethod = httpMethod;
        Assert.notNull(uri, "Argument [uri] cannot be null or blank");
    }


    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public String toString() {
        return urlData.getUrlPattern();
    }

    @SuppressWarnings("rawtypes")
    public Map getParameters() {
        return getParams();
    }

    public boolean isParsingRequest() {
        return parsingRequest;
    }

    public void setParsingRequest(boolean parsingRequest) {
        this.parsingRequest = parsingRequest;
    }

    public String getPluginName() {
        return pluginName == null ? null : pluginName.toString();
    }

    public String getNamespace() {
        String name = evaluateNameForValue(namespace);
        return urlConverter.toUrlElement(name);
    }
    public String getControllerName() {
        String name = evaluateNameForValue(controllerName);
        if (name == null && getViewName() == null) {
            throw new UrlMappingException("Unable to establish controller name to dispatch for [" +
                    controllerName + "]. Dynamic closure invocation returned null. Check your mapping file is correct, when assigning the controller name as a request parameter it cannot be an optional token!");
        }
        return urlConverter.toUrlElement(name);
    }

    public String getActionName() {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();

        String name = webRequest == null ? null : checkDispatchAction(webRequest.getCurrentRequest());
        if (name == null) {
            name = evaluateNameForValue(actionName, webRequest);
        }
        return urlConverter.toUrlElement(name);
    }

    public String getViewName() {
        return evaluateNameForValue(viewName);
    }

    public String getId() {
        return evaluateNameForValue(id);
    }

    private String checkDispatchAction(HttpServletRequest request) {
        if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) return null;

        String dispatchActionName = null;
        Enumeration<String> paramNames = tryMultipartParams(request, request.getParameterNames());

        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            if (name.startsWith(WebUtils.DISPATCH_ACTION_PARAMETER)) {
                // remove .x suffix in case of submit image
                if (name.endsWith(".x") || name.endsWith(".y")) {
                    name = name.substring(0, name.length() - 2);
                }
                dispatchActionName = GrailsNameUtils.getPropertyNameRepresentation(name.substring((WebUtils.DISPATCH_ACTION_PARAMETER).length()));
                break;
            }
        }
        return dispatchActionName;
    }

    private Enumeration<String> tryMultipartParams(HttpServletRequest request, Enumeration<String> originalParams) {
        Enumeration<String> paramNames = originalParams;
        boolean disabled = getMultipartDisabled();
        if (!disabled) {
            MultipartResolver resolver = getResolver();
            if (resolver != null && resolver.isMultipart(request)) {
                MultipartHttpServletRequest resolvedMultipartRequest = getResolvedRequest(request, resolver);
                paramNames = resolvedMultipartRequest.getParameterNames();
            }
        }
        return paramNames;
    }

    private MultipartHttpServletRequest getResolvedRequest(HttpServletRequest request, MultipartResolver resolver) {
        MultipartHttpServletRequest resolvedMultipartRequest = (MultipartHttpServletRequest) request.getAttribute(MultipartHttpServletRequest.class.getName());
        if (resolvedMultipartRequest == null) {
            resolvedMultipartRequest = resolver.resolveMultipart(request);
            request.setAttribute(MultipartHttpServletRequest.class.getName(), resolvedMultipartRequest);
        }
        return resolvedMultipartRequest;
    }

    private boolean getMultipartDisabled() {
        boolean disabled = false;
        GrailsApplication app = WebUtils.findApplication(servletContext);
        if(app != null) {
            Object disableMultipart = app.getFlatConfig().get(SETTING_GRAILS_WEB_DISABLE_MULTIPART);
            if (disableMultipart instanceof Boolean) {
                disabled = (Boolean)disableMultipart;
            }
            else if (disableMultipart instanceof String) {
                disabled = Boolean.valueOf((String)disableMultipart);
            }
        }

        return disabled;
    }

    private MultipartResolver getResolver() {
        ApplicationContext ctx = WebUtils.findApplicationContext(servletContext);
        if(ctx != null) {
            return (MultipartResolver)ctx.getBean(DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME);
        }
        return null;
    }

    public String getURI() {
        return evaluateNameForValue(uri);
    }

    @Override
    public Object getRedirectInfo() {
        return redirectInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultUrlMappingInfo that = (DefaultUrlMappingInfo) o;

        if (actionName != null ? !actionName.equals(that.actionName) : that.actionName != null) return false;
        if (controllerName != null ? !controllerName.equals(that.controllerName) : that.controllerName != null)
            return false;
        if (httpMethod != null ? !httpMethod.equals(that.httpMethod) : that.httpMethod != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;
        if (pluginName != null ? !pluginName.equals(that.pluginName) : that.pluginName != null) return false;
        if (redirectInfo != null ? !redirectInfo.equals(that.redirectInfo) : that.redirectInfo != null) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;
        if (viewName != null ? !viewName.equals(that.viewName) : that.viewName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = controllerName != null ? (CONTROLLER_PREFIX + controllerName).hashCode() : 0;
        result = 31 * result + (actionName != null ? (ACTION_PREFIX + actionName).hashCode() : 0);
        result = 31 * result + (pluginName != null ? (PLUGIN_PREFIX + pluginName).hashCode() : 0);
        result = 31 * result + (namespace != null ? (NAMESPACE_PREFIX + namespace).hashCode() : 0);
        result = 31 * result + (redirectInfo != null ? redirectInfo.hashCode() : 0);
        result = 31 * result + (id != null ? (ID_PREFIX + id).hashCode() : 0);
        result = 31 * result + (viewName != null ? (VIEW_PREFIX + viewName).hashCode() : 0);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (httpMethod != null ? (METHOD_PREFIX +httpMethod).hashCode() : 0);
        result = 31 * result + (version != null ? (VERSION_PREFIX +version).hashCode() : 0);
        return result;
    }
}
