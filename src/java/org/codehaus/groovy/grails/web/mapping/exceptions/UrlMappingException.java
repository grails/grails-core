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
package org.codehaus.groovy.grails.web.mapping.exceptions;

import org.codehaus.groovy.grails.exceptions.GrailsException;

/**
 * An exception thrown when an error occurs evaluating an URL mapping
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *
 *        <p/>
 *        Created: Feb 28, 2007
 *        Time: 6:22:38 PM
 */
public class UrlMappingException extends GrailsException {
    public UrlMappingException(String message) {
        super(message);
    }

    public UrlMappingException(String message, Exception e) {
        super(message,e);
    }
}
