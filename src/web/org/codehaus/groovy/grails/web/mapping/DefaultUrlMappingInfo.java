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

    public DefaultUrlMappingInfo(Object controllerName, Object actionName, Map params) {
        if(controllerName == null) throw new IllegalArgumentException("Argument [controllerName] cannot be null or blank");
        if(params == null) throw new IllegalArgumentException("Argument [params] cannot be null");

        this.params = Collections.unmodifiableMap(params);
        populateParamsForMapping(this.params);
        this.controllerName = controllerName;
        this.actionName = actionName;
        this.id = (String)params.get(ID_PARAM);
    }

    /**
     * Populates request parameters for the given UrlMappingInfo instance using the GrailsWebRequest
     *
     * @see org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
     *
     * @param dispatchParams The Map instance
     */
    protected void populateParamsForMapping(Map dispatchParams) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
        if(webRequest != null) {
            GrailsParameterMap params = webRequest.getParams();
            for (Iterator j = dispatchParams.keySet().iterator(); j.hasNext();) {
                String name = (String) j.next();
                params.put(name, dispatchParams.get(name));
            }

        }
    }

    public Map getParameters() {
        return params;
    }

    public String getControllerName() {        
        String ctrlName = evaluateNameForValue(this.controllerName);
        if(ctrlName == null) throw new UrlMappingException("Unable to establish controller name to dispatch for ["+this.controllerName+"]. Dynamic closure invocation returned null. Check your mapping file is correct, when assigning the controller name as a request parameter it cannot be an optional token!");
        return ctrlName;
    }

    public String getActionName() {
        return  evaluateNameForValue(this.actionName);
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
