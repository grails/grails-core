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
 * A UrlCreator is a class whoes implementor is resonposible for creating URL patterns as Strings.
 * A UrlCreator is passed is a set of parameter values and is required to produce a valid relative URI
 *
 * @author Graeme Rocher
 * @since 0.5.5
 *
 *        <p/>
 *        Created: May 30, 2007
 *        Time: 8:34:29 AM
 */
public interface UrlCreator {
    /**
     * Inspects this UrlMapping to discover whether it can be converted into a URL for the given parameter values.
     * If it cannot this method will return null
     *
     * @param parameterValues The parameter values
     *
     * @return Returns the reverse mapping URL for the given arguments or null if it cannot be reverse mapped
     */
    String createURL(Map parameterValues);
}
