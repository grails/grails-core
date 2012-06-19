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
 * <p>Thrown when a URI cannot be matched to a controller.
 *
 * @author Steven Devijver
 */
public class UnknownControllerException extends GrailsMVCException {

    private static final long serialVersionUID = -6726546157832604224L;

    public UnknownControllerException() {
        super();
    }

    public UnknownControllerException(String message) {
        super(message);
    }

    public UnknownControllerException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownControllerException(Throwable cause) {
        super(cause);
    }
}
