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
package grails.web.mapping.mvc.exceptions;

import org.grails.web.servlet.mvc.exceptions.ControllerExecutionException;

/**
 * Thrown when the request cannot be redirected.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class CannotRedirectException extends ControllerExecutionException {
    private static final long serialVersionUID = 1L;

    public CannotRedirectException() {}

    public CannotRedirectException(String message, Throwable t) {
        super(message, t);
    }

    public CannotRedirectException(String message) {
        super(message);
    }
}