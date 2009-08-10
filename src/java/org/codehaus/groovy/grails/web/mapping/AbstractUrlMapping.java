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

import javax.servlet.ServletContext;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * Abstract UrlMapping implementation that provides common basic functionality
 *
 * @author Graeme Rocher
 * @since 0.5.5
 *        <p/>
 *        Created: May 30, 2007
 *        Time: 8:25:36 AM
 */
public abstract class AbstractUrlMapping implements UrlMapping {

    protected final ConstrainedProperty[] constraints;
    protected Object controllerName;
    protected Object actionName;
    protected Object viewName;
    protected Object forwardURI;
    protected ServletContext servletContext;
    protected Map parameterValues = Collections.EMPTY_MAP;
    protected boolean parseRequest;
    protected String mappingName;
    protected boolean restful;

    /**
     * Base constructor required to construct a UrlMapping instance
     *
     * @param controllerName The name of the controller
     * @param actionName The name of the action
     * @param constraints Any constraints that apply to the mapping
     * @param servletContext
     */
    public AbstractUrlMapping(Object controllerName, Object actionName, Object viewName, ConstrainedProperty[] constraints, ServletContext servletContext) {
        this.controllerName = controllerName;
        this.actionName = actionName;
        this.constraints = constraints;
        this.viewName = viewName;
        this.servletContext = servletContext;
    }

    protected AbstractUrlMapping(Object viewName, ConstrainedProperty[] constraints, ServletContext servletContext) {
        this.viewName = viewName;
        this.constraints = constraints;
        this.servletContext = servletContext;
    }
    
    protected AbstractUrlMapping(URI uri, ConstrainedProperty[] constraints, ServletContext servletContext) {
    	this.forwardURI = uri;
    	this.constraints = constraints;
    	this.servletContext = servletContext;
    }

    /**
     * @see UrlMapping#getConstraints()
     */

    public ConstrainedProperty[] getConstraints() {
        return constraints;
    }

    /**
     * @see UrlMapping#getControllerName()
     */
    public Object getControllerName() {
        return controllerName;
    }
    /**
     * @see org.codehaus.groovy.grails.web.mapping.UrlMapping#getActionName()
     */
    public Object getActionName() {
        return actionName;
    }

    /**
     * @see org.codehaus.groovy.grails.web.mapping.UrlMapping#getViewName()
     *
     */
    public Object getViewName() {
        return viewName;
    }

    public void setParameterValues(Map parameterValues) {
        this.parameterValues = Collections.unmodifiableMap(parameterValues);
    }

    public void setParseRequest(boolean shouldParse) {
        this.parseRequest = shouldParse;
    }

    public String getMappingName() {
        return mappingName;
    }

    public void setMappingName(String name) {
        mappingName = name;
    }

    public void setRestfulMapping(boolean isREST) {
        this.restful = isREST;
    }

    public boolean isRestfulMapping() {
        return restful;
    }
}
