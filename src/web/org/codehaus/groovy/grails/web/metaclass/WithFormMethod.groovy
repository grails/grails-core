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
package org.codehaus.groovy.grails.web.metaclass

import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerToken
import org.codehaus.groovy.grails.web.servlet.mvc.TokenResponseHandler
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractTokenResponseHandler

/**
 * Implementation of the "Synchronizer Token Pattern" for Grails that handles duplicate form submissions
 * by inspecting a token stored in the user session
 *
 * @author Graeme Rocher
 * @since 1.1
 * 
 * Created: Jan 8, 2009
 */

class WithFormMethod {


    /**
     * <p>Main entry point, this method will check the request for the necessary TOKEN and if it is valid
     * will call the passed closure.
     *
     * <p>For an invalid response an InvalidResponseHandler is returned which will invoke the closure passed
     * to the handleInvalid method. The idea here is to allow code like:
     *
     * <pre><code>
     * withForm {
     *   // handle valid form submission
     * }.invalidToken {
     *    // handle invalid form submission
     * }
     * </code></pre>
     */
    TokenResponseHandler withForm(HttpServletRequest request, Closure callable) {
        TokenResponseHandler handler
        if(isTokenValid(request)) {
            resetToken(request)
            handler = new ValidResponseHandler(callable?.call())

        }
        else {
            handler = new InvalidResponseHandler()
        }

        request.setAttribute(TokenResponseHandler.KEY, handler)
        return handler
    }

    /**
     * Checks whether the token in th request is valid
     *
     * @param request The servlet request
     */
    protected synchronized boolean isTokenValid(HttpServletRequest request) {
        SynchronizerToken tokenInSession = request.getSession(false)?.getAttribute(SynchronizerToken.KEY)
        if(!tokenInSession) return false

        def tokenInRequest = request.getParameter(SynchronizerToken.KEY)
        if(!tokenInRequest) return false

        try {
            return tokenInSession.isValid(tokenInRequest)            
        }
        catch (IllegalArgumentException ) {
            return false
        }
    }

    /**
     * Resets the token in the request
     */
    protected synchronized resetToken(HttpServletRequest request) {
        request.getSession(false)?.removeAttribute(SynchronizerToken.KEY)
    }
}


class InvalidResponseHandler extends AbstractTokenResponseHandler {

    public InvalidResponseHandler() {
        super(false)
    }


    protected Object invalidTokenInternal(Closure callable) {
        callable?.call()
    }
}
class ValidResponseHandler extends AbstractTokenResponseHandler{

    def model

    public ValidResponseHandler(model) {
        super(true);
        this.model = model
    }
    protected Object invalidTokenInternal(Closure callable) {
        return model
    }
}
