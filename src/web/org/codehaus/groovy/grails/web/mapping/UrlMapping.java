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
package org.codehaus.groovy.grails.web.mapping;

import org.codehaus.groovy.grails.validation.ConstrainedProperty;

import java.util.Map;

/**
 * <p>An interface that defines a URL mapping. A URL mapping is a mapping between a URI such as /book/list and
 * a controller, action and/or id</p>
 * 
 * <p>A UrlMapping should implement Comparable so that UrlMapping instances can be ordered to allow for precendence rules.
 * In other words the URL /book/list should be matched before /book/* as the wildcard is of lesser precedence. By implementing
 * Comparable this can be allowed for.
 * </p>

 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 28, 2007
 *        Time: 5:49:41 PM
 */
public interface UrlMapping extends Comparable, UrlCreator {

    String CONTROLLER = "controller";
    String ACTION = "action";

    /**
     * Matches the given URI and returns an instance of the UrlMappingInfo interface or null
     * if a match couldn't be established
     *
     * @param uri The URI to match
     * @return An instance of UrlMappingInfo or null if the URI doesn't match
     */
    UrlMappingInfo match(String uri);


    /**
     * Retrieves the UrlMappingData instance that describes this UrlMapping
     *
     * @return The UrlMappingData instance
     */
    UrlMappingData getUrlData();

    /**
     * <p>The constraints the apply to this UrlMapping. Each constraint maps to a GString token in a
     * URL mapping in order. For example consider the URL:
     *
     * <pre>
     * <code>
     *     /blog/$author/$title/$year?/$month?/$day?
     * </code></pre>
     *
     * <p>This results in 5 ConstrainedProperty instances called author, title, year, month and day
     *
     * @return An array containing the ConstrainedProperty objects of this URLMapping
     */
    ConstrainedProperty[] getConstraints();

    /**
     * Retrieves the controller name which is either a groovy.lang.Closure that evaluates the controller
     * name at runtime or a java.lang.String that represents the controller name
     *
     * @return The controller name as a {@link groovy.lang.Closure} or {@link java.lang.String}
     */
    Object getControllerName();

    /**
     * Retrieves the action name which is either a groovy.lang.Closure that evaluates the action
     * name at runtime or a java.lang.String that represents the action name
     *
     * @return The action name as a {@link groovy.lang.Closure} or {@link java.lang.String}
     */
    Object getActionName();

    /**
     * Returns the name of the view to map to
     * @return The view name
     */
    Object getViewName();

    /**
     * Sets any parameter values that should be populated into the request
     * @param parameterValues The parameter values to set
     */
    void setParameterValues(Map parameterValues);
}
