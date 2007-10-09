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

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

import java.util.Map;

/**
 * The UrlMappingInfo interface defines that data that was produced when matching a URI with a UrlMapping instance.
 *
 * @see org.codehaus.groovy.grails.web.mapping.UrlMapping
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 28, 2007
 *        Time: 5:56:38 PM
 */
public interface UrlMappingInfo {

    /**
     * The name of the controller that the URL mapping maps to
     *
     * @return The name of the controller
     */
    String getControllerName();

    /**
     * The name of the action that the URL mappping maps to
     *
     * @return The name of the action or null if not known
     */
    String getActionName();

    /**
     * The name of the view that the URL mappping maps to
     *
     * @return The name of the view or null if not known
     */
    String getViewName();

    /**
     * The id part of the URL mapping if any
     * 
     * @return The id or null
     */
    String getId();

    /**
     * The parameters that were extracted from the URI that was matched
     *
     * @return A Map of parameters
     */
    Map getParameters();

    /**
     * Configure this UrlMappingInfo the for the given GrailsWebRequest
     *
     * @param webRequest The GrailsWebRequest instance
     */
    void configure(GrailsWebRequest webRequest);
}
