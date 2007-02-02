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
package org.codehaus.groovy.grails.orm.hibernate.support;

import org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.FlushMode;

/**
 * An interceptor that extends the default spring OSIVI and doesn't flush the session if it has been set
 * to MANUAL on the session itself
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 2, 2007
 *        Time: 12:24:11 AM
 */
public class GrailsOpenSessionInViewInterceptor extends OpenSessionInViewInterceptor {

    protected void flushIfNecessary(Session session, boolean existingTransaction) throws HibernateException {
        if(session.getFlushMode() != FlushMode.MANUAL) {
            super.flushIfNecessary(session, existingTransaction);
        }
    }
}
