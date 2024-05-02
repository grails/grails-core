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
package org.grails.plugins.web.servlet.mvc

import groovy.transform.CompileStatic
import org.grails.web.servlet.mvc.AbstractTokenResponseHandler


/**
 * Handles a valid token response. See {@link org.grails.web.servlet.mvc.TokenResponseHandler}
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class ValidResponseHandler extends AbstractTokenResponseHandler {

    def model

    ValidResponseHandler(model) {
        super(true)
        this.model = model
    }

    protected Object invalidTokenInternal(Closure callable) { model }
}
