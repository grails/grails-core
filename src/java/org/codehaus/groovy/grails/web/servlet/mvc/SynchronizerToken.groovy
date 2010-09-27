/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.servlet.mvc

import javax.servlet.http.HttpSession

/**
 * A token used to handle double-submits.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class SynchronizerToken implements Serializable {

    public static final String KEY = "org.codehaus.groovy.grails.SYNCHRONIZER_TOKEN"
    public static final String URI = "org.codehaus.groovy.grails.SYNCHRONIZER_URI"

    UUID currentToken

    SynchronizerToken() {
        generateToken()
    }

    boolean isValid(String token) {
        currentToken?.equals(UUID.fromString(token))
    }

    void generateToken() {
        currentToken = UUID.randomUUID()
    }

    static SynchronizerToken store(HttpSession session) {
        SynchronizerToken token = session.getAttribute(KEY) ?: new SynchronizerToken()
        session.setAttribute(KEY, token)
        return token
    }
}
