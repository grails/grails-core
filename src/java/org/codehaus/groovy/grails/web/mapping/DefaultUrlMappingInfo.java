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
import groovy.lang.Closure;
import groovy.util.ConfigObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

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
public class DefaultUrlMappingInfo implements UrlMappingInfo {
    private Map params = Collections.EMPTY_MAP;
    private Object controllerName;
    private Object actionName;
    private Object id;
    private static final String ID_PARAM = "id";
    private UrlMappingData urlData;
    private Object viewName;
    private ServletContext servletContext;
    private static final String SETTING_GRAILS_WEB_DISABLE_MULTIPART = "grails.web.disable.multipart";
    private boolean parsingRequest;


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

    public String toString() {
        return urlData.getUrlPattern();
    }

    /**
     * Populates request parameters for the given UrlMappingInfo instance using the GrailsWebRequest
     *
     * @param webRequest The Map instance
     * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
     */
    protected void populateParamsForMapping(GrailsWebRequest webRequest) {
        Map dispatchParams = webRequest.getParams();
        String encoding = webRequest.getRequest().getCharacterEncoding();
        if (encoding == null) encoding = "UTF-8";

        Collection keys = this.params.keySet();
        keys = DefaultGroovyMethods.toList(keys);
        Collections.sort((List) keys, new Comparator() {

            public int compare(Object leftKey, Object rightKey) {
                Object leftValue = params.get(leftKey);
                Object rightValue = params.get(rightKey);
                boolean leftIsClosure = leftValue instanceof Closure;
                boolean rightIsClosure = rightValue instanceof Closure;
                if (leftIsClosure && rightIsClosure) return 0;
                else if (leftIsClosure && !rightIsClosure) return 1;
                else if (rightIsClosure && !leftIsClosure) return -1;
                return 0;
            }
        });
        for (Iterator j = keys.iterator(); j.hasNext();) {

            String name = (String) j.next();
            Object param = this.params.get(name);
            if (param instanceof Closure) {
                param = evaluateNameForValue(param);
            }
            if (param instanceof String) {
                try {
                    param = URLDecoder.decode((String) param, encoding);
                } catch (UnsupportedEncodingException e) {
                    param = evaluateNameForValue(param);
                }
            }
            dispatchParams.put(name, param);
        }

        final String viewName = getViewName();

        if (viewName == null) {
            webRequest.setControllerName(getControllerName());
            webRequest.setActionName(getActionName());
        }

        String id = getId();
        if (!StringUtils.isBlank(id)) try {
            dispatchParams.put(GrailsWebRequest.ID_PARAMETER, URLDecoder.decode(id, encoding));
        } catch (UnsupportedEncodingException e) {
            dispatchParams.put(GrailsWebRequest.ID_PARAMETER, id);
        }

    }

    public Map getParameters() {
        return params;
    }

    public void configure(GrailsWebRequest webRequest) {
        populateParamsForMapping(webRequest);
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

    private String evaluateNameForValue(Object value) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
        return evaluateNameForValue(value, webRequest);
    }

    private String evaluateNameForValue(Object value, GrailsWebRequest webRequest) {
        if (value == null) {
            return null;
        }
        String name;
        if (value instanceof Closure) {
            Closure callable = (Closure) value;
            final Closure cloned = (Closure) callable.clone();
            cloned.setDelegate(webRequest);
            cloned.setResolveStrategy(Closure.DELEGATE_FIRST);
            Object result = cloned.call();
            name = result != null ? result.toString() : null;
        } else if (value instanceof Map) {
            Map httpMethods = (Map) value;
            name = (String) httpMethods.get(webRequest.getCurrentRequest().getMethod());
        } else {
            name = value.toString();
        }
        return name != null ? name.trim() : null;
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


}
