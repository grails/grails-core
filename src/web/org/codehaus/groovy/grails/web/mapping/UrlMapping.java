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

/**
 * An interface that defines a URL mapping. A URL mapping is a mapping between a URI such as /book/list and
 * a controller, action and/or id
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 28, 2007
 *        Time: 5:49:41 PM
 */
public interface UrlMapping {

    /**
     * Matches the given URI and returns an instance of the UrlMappingInfo interface or null
     * if a match couldn't be established
     *
     * @param uri The URI to match
     * @return An instance of UrlMappingInfo or null if the URI doesn't match
     */
    UrlMappingInfo match(String uri);
}
