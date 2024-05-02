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

/**
 * Throw when an exception occurs during controller execution
 *
 * @author Graeme Rocher
 * @since 0.2
 */
public class ControllerExecutionException extends GrailsMVCException {

    private static final long serialVersionUID = 4625710641559921836L;

    public ControllerExecutionException() {
        super();
    }

    public ControllerExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ControllerExecutionException(String message) {
        super(message);
    }

    public ControllerExecutionException(Throwable cause) {
        super(cause);
    }
}
