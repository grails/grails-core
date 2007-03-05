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
 * <p>Holds information about a parsed URL including such as the tokens that make up the URL, The URLs (plural)
 * that the UrLMapping logically maps to and so forth</p>
 *
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Mar 5, 2007
 *        Time: 7:44:51 AM
 */
public interface UrlMappingData {

    /**
     * <p>Retrieves the tokens that make up a URL. For example the tokens for the URL /blog/2007/* would
     * be "blog", "2007" and "*"
     *
      * @return The tokens as a string array
     */
    String[] getTokens();

    /**
     * <p>Obtains the logical URLs for this URL</p>
     *
     * @return The logical URLs as a string array
     */
    String[] getLogicalUrls();

    /**
     * Retrieves the URL pattern for this UrlMappingData instance
     *
     * @return The URL pattern
     */
    String getUrlPattern();
}
