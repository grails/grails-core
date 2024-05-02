/*
 * Copyright 2024 original authors
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
package org.grails.web.servlet.mvc.exceptions;

import org.grails.core.exceptions.GrailsException;

/**
 * Thrown when an unrecoverable error occurred in the Grails MVC framework.
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public abstract class GrailsMVCException extends GrailsException {

    private static final long serialVersionUID = 8489513412930525030L;

    public GrailsMVCException() {
        super();
    }

    public GrailsMVCException(String message) {
        super(message);
    }

    public GrailsMVCException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrailsMVCException(Throwable cause) {
        super(cause);
    }
}
