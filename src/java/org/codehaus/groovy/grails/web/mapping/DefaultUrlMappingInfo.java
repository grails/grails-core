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
import groovy.util.ConfigObject;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * A Class that implements the UrlMappingInfo interface and holds information established from a matched
 * URL
 *
 * @author Graeme Rocher
 * @since 0.5
 *        <p/>
 *        <p/>
 *        Created: Mar 1, 2007
 *        Time: 7:19:35 AM
 */
public class DefaultUrlMappingInfo extends AbstractUrlMappingInfo implements UrlMappingInfo {
    
    private Object controllerName;
    private Object actionName;
    private Object id;
    private static final String ID_PARAM = "id";
    private UrlMappingData urlData;
    private Object viewName;
    private ServletContext servletContext;
    private static final String SETTING_GRAILS_WEB_DISABLE_MULTIPART = "grails.web.disable.multipart";
    private boolean parsingRequest;
	private Object uri;


    private DefaultUrlMappingInfo(Map params, UrlMappingData urlData, ServletContext servletContext) {
        this.params = Collections.unmodifiableMap(params);
        this.id = params.get(ID_PARAM);
        this.urlData = urlData;
        this.servletContext = servletContext;
    }

    public DefaultUrlMappingInfo(Object controllerName, Object actionName, Object viewName, Map params, UrlMappingData urlData, ServletContext servletContext) {
        this(params, urlData, servletContext);
        if (controllerName == null && viewName == null)
            throw new IllegalArgumentException("URL mapping must either provide a controller or view name to map to!");
        if (params == null) throw new IllegalArgumentException("Argument [params] cannot be null");
        this.controllerName = controllerName;
        this.actionName = actionName;
        if (actionName == null)
            this.viewName = viewName;
    }

    public DefaultUrlMappingInfo(Object viewName, Map params, UrlMappingData urlData, ServletContext servletContext) {
        this(params, urlData, servletContext);
        this.viewName = viewName;
        if (viewName == null) throw new IllegalArgumentException("Argument [viewName] cannot be null or blank");

    }
    
    public DefaultUrlMappingInfo(Object uri, UrlMappingData data, ServletContext servletContext) {
    	this(Collections.EMPTY_MAP, data,servletContext);
    	this.uri = uri;
    	
    	if (uri == null) throw new IllegalArgumentException("Argument [uri] cannot be null or blank");
    }

    public String toString() {
        return urlData.getUrlPattern();
    }

    public Map getParameters() {
        return params;
    }

    public boolean isParsingRequest() {
        return this.parsingRequest;
    }

    public void setParsingRequest(boolean parsingRequest) {
        this.parsingRequest = parsingRequest;
    }

    public String getControllerName() {
        String controllerName = evaluateNameForValue(this.controllerName);
        if (controllerName == null && getViewName() == null)
            throw new UrlMappingException("Unable to establish controller name to dispatch for [" + this.controllerName + "]. Dynamic closure invocation returned null. Check your mapping file is correct, when assigning the controller name as a request parameter it cannot be an optional token!");
        return controllerName;
    }

    public String getActionName() {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();

        String name = webRequest != null ? checkDispatchAction(webRequest.getCurrentRequest(), null) : null;
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

    private String checkDispatchAction(HttpServletRequest request, String actionName) {
        Enumeration paramNames = tryMultipartParams(request, request.getParameterNames());

        for (; paramNames.hasMoreElements();) {
            String name = (String) paramNames.nextElement();
            if (name.startsWith(WebUtils.DISPATCH_ACTION_PARAMETER)) {
                // remove .x suffix in case of submit image
                if (name.endsWith(".x") || name.endsWith(".y")) {
                    name = name.substring(0, name.length() - 2);
                }
                actionName = GrailsNameUtils.getPropertyNameRepresentation(name.substring((WebUtils.DISPATCH_ACTION_PARAMETER).length()));
                break;
            }
        }
        return actionName;
    }

    private Enumeration tryMultipartParams(HttpServletRequest request, Enumeration originalParams) {
        Enumeration paramNames = originalParams;
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
        ConfigObject config = app.getConfig();
        boolean disabled = false;
        Object disableMultipart = config.get(SETTING_GRAILS_WEB_DISABLE_MULTIPART);
        if (disableMultipart instanceof Boolean) {
            disabled = ((Boolean) disableMultipart).booleanValue();
        } else if (disableMultipart instanceof String) {
            disabled = Boolean.valueOf((String) disableMultipart).booleanValue();
        }
        return disabled;
    }

    private MultipartResolver getResolver() {
        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        MultipartResolver resolver = (MultipartResolver)
                ctx.getBean(DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME);
        return resolver;
    }

	public String getURI() {
		return evaluateNameForValue(this.uri);
	}


}
