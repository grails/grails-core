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
 * An abstract class that implements the behavior of wasInvoked in the TokenResponseHandler interface
 *
 * @see TokenResponseHandler#wasInvoked()
 * 
 * @author Graeme Rocher
 * @since 1.1
 *        <p/>
 *        Created: Jan 8, 2009
 */
public abstract class AbstractTokenResponseHandler implements TokenResponseHandler{

    private boolean invoked = false;
    private boolean valid;

    public AbstractTokenResponseHandler(boolean valid) {
        super();
        this.valid = valid;
    }


    final public Object invalidToken(Closure callable) {
        invoked = true;
        return invalidTokenInternal(callable);
    }

    protected abstract Object invalidTokenInternal(Closure callable);

    public boolean wasInvoked() {
        return invoked;
    }

    public boolean wasInvalidToken() {
        return !valid;
    }
}
