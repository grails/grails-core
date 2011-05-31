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
package org.codehaus.groovy.grails.web.mapping;

import grails.util.GrailsNameUtils;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Holds information established from a matched URL.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public class DefaultUrlMappingInfo extends AbstractUrlMappingInfo implements UrlMappingInfo {

    private Object controllerName;
    private Object actionName;
    private Object id;
    private static final String ID_PARAM = "id";
    private UrlMappingData urlData;
    private Object viewName;
    private ServletContext servletContext;
    private static final String SETTING_GRAILS_WEB_DISABLE_MULTIPART = "grails.disableCommonsMultipart";
    private boolean parsingRequest;
    private Object uri;

    @SuppressWarnings({"unchecked","rawtypes"})
    private DefaultUrlMappingInfo(Map params, UrlMappingData urlData, ServletContext servletContext) {
        this.params = Collections.unmodifiableMap(params);
        this.id = params.get(ID_PARAM);
        this.urlData = urlData;
        this.servletContext = servletContext;
    }

    @SuppressWarnings("rawtypes")
    public DefaultUrlMappingInfo(Object controllerName, Object actionName, Object viewName, Map params,
            UrlMappingData urlData, ServletContext servletContext) {
        this(params, urlData, servletContext);
        Assert.isTrue(controllerName != null || viewName != null, "URL mapping must either provide a controller or view name to map to!");
        Assert.notNull(params, "Argument [params] cannot be null");
        this.controllerName = controllerName;
        this.actionName = actionName;
        if (actionName == null) {
            this.viewName = viewName;
        }
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

    @Override
    public String toString() {
        return urlData.getUrlPattern();
    }

    @SuppressWarnings("rawtypes")
    public Map getParameters() {
        return params;
    }

    public boolean isParsingRequest() {
        return parsingRequest;
    }

    public void setParsingRequest(boolean parsingRequest) {
        this.parsingRequest = parsingRequest;
    }

    public String getControllerName() {
        String name = evaluateNameForValue(this.controllerName);
        if (name == null && getViewName() == null) {
            throw new UrlMappingException("Unable to establish controller name to dispatch for [" +
                    controllerName + "]. Dynamic closure invocation returned null. Check your mapping file is correct, when assigning the controller name as a request parameter it cannot be an optional token!");
        }
        return name;
    }

    public String getActionName() {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();

        String name = webRequest != null ? checkDispatchAction(webRequest.getCurrentRequest()) : null;
        if (name == null) {
            name = evaluateNameForValue(this.actionName, webRequest);
        }
        return name;
    }

    public String getViewName() {
        return evaluateNameForValue(this.viewName);
    }

    public String getId() {
        return evaluateNameForValue(this.id);
    }

    @SuppressWarnings("unchecked")
    private String checkDispatchAction(HttpServletRequest request) {
        String dispatchActionName = null;
        Enumeration<String> paramNames = tryMultipartParams(request, request.getParameterNames());

        for (; paramNames.hasMoreElements();) {
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

    @SuppressWarnings("unchecked")
    private Enumeration<String> tryMultipartParams(HttpServletRequest request, Enumeration<String> originalParams) {
        Enumeration<String> paramNames = originalParams;
        boolean disabled = getMultipartDisabled();
        if (!disabled) {
            MultipartResolver resolver = getResolver();
            if (resolver.isMultipart(request)) {
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
        GrailsApplication app = WebUtils.lookupApplication(servletContext);
        Object disableMultipart = app.getFlatConfig().get(SETTING_GRAILS_WEB_DISABLE_MULTIPART);
        boolean disabled = false;
        if (disableMultipart instanceof Boolean) {
            disabled = ((Boolean) disableMultipart).booleanValue();
        }
        else if (disableMultipart instanceof String) {
            disabled = Boolean.valueOf((String) disableMultipart).booleanValue();
        }
        return disabled;
    }

    private MultipartResolver getResolver() {
        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        return (MultipartResolver)ctx.getBean(DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME);
    }

    public String getURI() {
        return evaluateNameForValue(this.uri);
    }
}
