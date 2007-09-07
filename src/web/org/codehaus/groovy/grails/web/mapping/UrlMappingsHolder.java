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

import java.util.Map;

/**
 * Main entry point of Grails URL mapping mechanism. This interface defines methods to match
 * URLs and create reverse mappings based on the UrlMapping instances the implementor contains
 *
 * @author Graeme Rocher
 * @since 0.5
 * 
 *        <p/>
 *        Created: Mar 6, 2007
 *        Time: 8:03:25 AM
 */
public interface UrlMappingsHolder {
    
    String BEAN_ID = "grailsUrlMappingsHolder";

    /**
     * Retrieves the held UrlMapping instances as an array
     *
     * @return An array of UrlMapping instances
     */
    UrlMapping[] getUrlMappings();


    /**
     * Retrieves the best guess of a URI for the given controller, action and parameters
     *
     * @param controller The name of the controller
     * @param action The name of the action or null
     * @param params The parameters or null
     * @return A URI for the given arguments
     */
    UrlCreator getReverseMapping(String controller, String action, Map params);

    /**
     * Match and return the first UrlMappingInfo instance possible
     *
     * @param uri The URI to match
     * @return A UrlMappingInfo or null
     */
    UrlMappingInfo match(String uri);

    /**
     * Matches all possible UrlMappingInfo instances to the given URI and returns them all
     *
     * @param uri The URI to match
     * @return An array of 0 or many UrlMappngInfo instances
     */
    UrlMappingInfo[] matchAll(String uri);

    /**
     * Match all possible UrlMappingInfo instances to the given URI and HTTP method
     *
     * @param uri The URI to match
     * @param httpMethod The HTTP method (GET,POST,PUT,DELETE etc.)
     * @return An array of 0 or many UrlMappingInfo instances
     */
    UrlMappingInfo[] matchAll(String uri, String httpMethod);

    /**
     * Match and return the first UrlMappingInfo instance possible
     *
     * @param responseCode The responseCode to match
     * @return A UrlMappingInfo or null
     */
    UrlMappingInfo matchStatusCode(int responseCode);

}
