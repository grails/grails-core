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

import groovy.lang.Closure;
import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * A Class that implements the UrlMappingInfo interface and holds information established from a matched
 * URL
 *
 * @author Graeme Rocher
 * @since 0.5
 * 
 *        <p/>
 *        Created: Mar 1, 2007
 *        Time: 7:19:35 AM
 */
public class DefaultUrlMappingInfo implements UrlMappingInfo {
    private Map params = Collections.EMPTY_MAP;
    private Object controllerName;
    private Object actionName;
    private static final String ID_PARAM = "id";
    private String id;
    private UrlMappingData urlData;
    private Object viewName;

    private DefaultUrlMappingInfo(Map params, UrlMappingData urlData) {
        this.params = Collections.unmodifiableMap(params);
        this.id = (String)params.get(ID_PARAM);
        this.urlData = urlData;
    }

    public DefaultUrlMappingInfo(Object controllerName, Object actionName, Object viewName,Map params, UrlMappingData urlData) {
        this(params, urlData);
        if(controllerName == null && viewName == null) throw new IllegalArgumentException("URL mapping must either provide a controller or view name to map to!");
        if(params == null) throw new IllegalArgumentException("Argument [params] cannot be null");
        this.controllerName = controllerName;
        this.actionName = actionName;
        if(actionName == null)
            this.viewName = viewName;
    }

    public DefaultUrlMappingInfo(Object viewName, Map params, UrlMappingData urlData) {
        this(params, urlData);
        this.viewName = viewName;
        if(viewName == null) throw new IllegalArgumentException("Argument [viewName] cannot be null or blank");

    }

    public String toString() {
        return urlData.getUrlPattern();
    }

    /**
     * Populates request parameters for the given UrlMappingInfo instance using the GrailsWebRequest
     *
     * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
     *
     * @param dispatchParams The Map instance
     */
    protected void populateParamsForMapping(Map dispatchParams) {
        for (Iterator j = this.params.keySet().iterator(); j.hasNext();) {
            String name = (String) j.next();
            dispatchParams.put(name, this.params.get(name));
        }
    }

    public Map getParameters() {
        return params;
    }

    public void configure(GrailsWebRequest webRequest) {
        populateParamsForMapping(webRequest.getParams());
    }

    public String getControllerName() {        
        String controllerName = evaluateNameForValue(this.controllerName);
        if(controllerName == null && getViewName() == null) throw new UrlMappingException("Unable to establish controller name to dispatch for ["+this.controllerName+"]. Dynamic closure invocation returned null. Check your mapping file is correct, when assigning the controller name as a request parameter it cannot be an optional token!");
        return controllerName;
    }

    public String getActionName() {
        return  evaluateNameForValue(this.actionName);
    }

    public String getViewName() {
        return evaluateNameForValue(this.viewName);
    }

    public String getId() {
        return id;
    }

    private String evaluateNameForValue(Object value) {
        if(value == null)return null;
        String name;
        if(value instanceof Closure) {
            Closure callable = (Closure)value;
            Object result = ((Closure)callable.clone()).call();
            name = result != null ? result.toString() : null;
        }
        else if(value instanceof Map) {
            Map httpMethods = (Map)value;
            GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
            name = (String)httpMethods.get(webRequest.getCurrentRequest().getMethod());
        }
        else {
            name = value.toString();
        }
        return name;
    }

}
