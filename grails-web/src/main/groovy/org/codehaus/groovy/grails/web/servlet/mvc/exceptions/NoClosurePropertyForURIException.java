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
package org.codehaus.groovy.grails.web.servlet.mvc.exceptions;

/**
 * Thrown when no closure property has been mapped to a given URI.
 *
 * @author Steven Devijver
 */
public class NoClosurePropertyForURIException extends GrailsMVCException {

    private static final long serialVersionUID = -5736020052704063202L;

    public NoClosurePropertyForURIException() {
        super();
    }

    public NoClosurePropertyForURIException(String message) {
        super(message);
    }

    public NoClosurePropertyForURIException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoClosurePropertyForURIException(Throwable cause) {
        super(cause);
    }
}
