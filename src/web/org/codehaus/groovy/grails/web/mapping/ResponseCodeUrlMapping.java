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

import org.codehaus.groovy.grails.validation.ConstrainedProperty;

import java.util.Collections;
import java.util.Map;

/**
 * A Url mapping for http response codes
 *
 * @author mike
 * @since 1.0-RC1
 */
public class ResponseCodeUrlMapping implements UrlMapping {
    private final ResponseCodeMappingData urlData;
    private final Object controllerName;
    private final Object actionName;
    private final ConstrainedProperty[] constraints = new ConstrainedProperty[0];
    private final Object viewName;
    private Map parameterValues = Collections.EMPTY_MAP;

    public ResponseCodeUrlMapping(UrlMappingData urlData, Object controllerName, Object actionName, Object viewName,ConstrainedProperty[] constraints) {
        this.urlData = (ResponseCodeMappingData) urlData;
        this.controllerName = controllerName;
        this.actionName = actionName;
        this.viewName = viewName;

        if (constraints != null && constraints.length > 0) {
            throw new IllegalArgumentException("Constraints can't be used for response code url mapping");
        }
    }

    public UrlMappingInfo match(String uri) {
        return null;
    }

    public UrlMappingData getUrlData() {
        return urlData;
    }

    public ConstrainedProperty[] getConstraints() {
        return constraints;
    }

    public Object getControllerName() {
        return controllerName;
    }

    public Object getActionName() {
        return actionName;
    }

    public Object getViewName() {
        return this.viewName;
    }

    public void setParameterValues(Map parameterValues) {
        this.parameterValues = parameterValues;
    }

    public int compareTo(Object o) {
        return 0;
    }

    public String createURL(Map parameterValues, String encoding) {
        throw new UnsupportedOperationException("Method createURL not implemented in " + getClass());
    }

    public String createURL(Map parameterValues, String encoding, String fragment) {
        throw new UnsupportedOperationException("Method createURL not implemented in " + getClass());
    }

    public String createURL(String controller, String action, Map parameterValues, String encoding) {
        throw new UnsupportedOperationException("Method createURL not implemented in " + getClass());
    }

    public String createRelativeURL(String controller, String action, Map parameterValues, String encoding) {
        throw new UnsupportedOperationException("Method createRelativeURL not implemented in " + getClass());
    }

    public String createURL(String controller, String action, Map parameterValues, String encoding, String fragment) {
        throw new UnsupportedOperationException("Method createURL not implemented in " + getClass());
    }

    public UrlMappingInfo match(int responseCode) {
        if (responseCode == urlData.getResponseCode()) return new DefaultUrlMappingInfo(
                controllerName,
                actionName,
                viewName,
                parameterValues,
                urlData);
        return null;
    }
}
