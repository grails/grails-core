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
 * A simple holder interface to be registered in the ApplicationContext that should hold a reference to all
 * UrlMappings.
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
}
