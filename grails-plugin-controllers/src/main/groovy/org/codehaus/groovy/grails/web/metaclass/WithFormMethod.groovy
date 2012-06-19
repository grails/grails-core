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
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractTokenResponseHandler
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.codehaus.groovy.grails.web.servlet.mvc.TokenResponseHandler

/**
 * Implementation of the "Synchronizer Token Pattern" for Grails that handles duplicate form submissions
 * by inspecting a token stored in the user session.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class WithFormMethod {

    /**
     * <p>Main entry point, this method will check the request for the necessary TOKEN and if it is valid
     *     will call the passed closure.
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
    TokenResponseHandler withForm(GrailsWebRequest webRequest, Closure callable) {
        TokenResponseHandler handler
        if (isTokenValid(webRequest)) {
            resetToken(webRequest)
            handler = new ValidResponseHandler(callable?.call())
        }
        else {
            handler = new InvalidResponseHandler()
        }

        webRequest.request.setAttribute(TokenResponseHandler.KEY, handler)
        return handler
    }

    /**
     * Checks whether the token in th request is valid.
     *
     * @param request The servlet request
     */
    protected synchronized boolean isTokenValid(GrailsWebRequest webRequest) {
        final request = webRequest.getCurrentRequest()
        SynchronizerTokensHolder tokensHolderInSession = request.getSession(false)?.getAttribute(SynchronizerTokensHolder.HOLDER)
        if (!tokensHolderInSession) return false

        String tokenInRequest = webRequest.params[SynchronizerTokensHolder.TOKEN_KEY]
        if (!tokenInRequest) return false

        String urlInRequest = webRequest.params[SynchronizerTokensHolder.TOKEN_URI]
        if (!urlInRequest) return false

        try {
            return tokensHolderInSession.isValid(urlInRequest, tokenInRequest)
        }
        catch (IllegalArgumentException) {
            return false
        }
    }

    /**
     * Resets the token in the request
     */
    protected synchronized resetToken(GrailsWebRequest webRequest) {
        final request = webRequest.getCurrentRequest()
        SynchronizerTokensHolder tokensHolderInSession = request.getSession(false)?.getAttribute(SynchronizerTokensHolder.HOLDER)
        String urlInRequest = webRequest.params[SynchronizerTokensHolder.TOKEN_URI]

        tokensHolderInSession.resetToken(urlInRequest)
        if (tokensHolderInSession.isEmpty()) request.getSession(false)?.removeAttribute(SynchronizerTokensHolder.HOLDER)
    }
}

class InvalidResponseHandler extends AbstractTokenResponseHandler {

    InvalidResponseHandler() {
        super(false)
    }

    protected Object invalidTokenInternal(Closure callable) {
        callable?.call()
    }
}

class ValidResponseHandler extends AbstractTokenResponseHandler {

    def model

    ValidResponseHandler(model) {
        super(true)
        this.model = model
    }

    protected Object invalidTokenInternal(Closure callable) { model }
}
