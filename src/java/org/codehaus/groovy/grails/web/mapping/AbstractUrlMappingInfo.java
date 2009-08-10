/* Copyright 2004-2005 the original author or authors.
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
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * Abstract super class providing pass functionality for configuring a UrlMappingInfo
 * @author Graeme Rocher
 * @since 1.2
 */
public abstract class AbstractUrlMappingInfo implements UrlMappingInfo{

    protected Map params = Collections.EMPTY_MAP;

    public void configure(GrailsWebRequest webRequest) {
        populateParamsForMapping(webRequest);
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
        for (Object key : keys) {

            String name = (String) key;
            Object param = this.params.get(name);
            if (param instanceof Closure) {
                param = evaluateNameForValue(param);
            }
            if (param instanceof String) {
                try {
                    param = URLDecoder.decode((String) param, encoding);
                }
                catch (UnsupportedEncodingException e) {
                    param = evaluateNameForValue(param);
                }
            }
            dispatchParams.put(name, param);
        }

        final String viewName = getViewName();

        if (viewName == null && getURI() == null) {
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

    protected String evaluateNameForValue(Object value) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
        return evaluateNameForValue(value, webRequest);
    }

    protected String evaluateNameForValue(Object value, GrailsWebRequest webRequest) {
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
}
