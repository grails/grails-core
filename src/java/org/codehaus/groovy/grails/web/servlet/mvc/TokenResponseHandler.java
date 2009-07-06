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
package org.codehaus.groovy.grails.web.servlet.mvc;

import groovy.lang.Closure;

/**
 * An interface used to invoke user code that handles double or invalid submits
 *
 *
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Jan 8, 2009
 */
public interface TokenResponseHandler {

    String KEY = "org.codehaus.groovy.grails.TOKEN_RESPONSE_HANDLER";

    /**
     * The method clients use in order to specify behavior in the even of an invalid token
     *  
     * @param callable The closure to invoke in the event of an invalid token
     * @return A Grails model or null
     */
    Object invalidToken(Closure callable);

    /**
     * Return whether the response handle was invoked
     * @return True if it was
     */
    boolean wasInvoked();

    /**
     * Return whether the token was invalid
     * @return True if it was
     */
    boolean wasInvalidToken();
}
