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

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.sitemesh.GrailsContentBufferingResponse;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;

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

    private static final String IS_FLOW_REQUEST_ATTRIBUTE = "org.codehaus.groovy.grails.webflow.flow_request";

    public void preHandle(WebRequest request) throws DataAccessException {
        GrailsWebRequest webRequest = (GrailsWebRequest) request.getAttribute(GrailsApplicationAttributes.WEB_REQUEST, WebRequest.SCOPE_REQUEST);
        final boolean isFlowRequest = webRequest.isFlowRequest();
        if(isFlowRequest) {
            webRequest.setAttribute(IS_FLOW_REQUEST_ATTRIBUTE, "true", WebRequest.SCOPE_REQUEST);
        }
        else {
            super.preHandle(request);
        }
    }

    public void postHandle(WebRequest request, ModelMap model) throws DataAccessException {
        final boolean isFlowRequest = request.getAttribute(IS_FLOW_REQUEST_ATTRIBUTE, WebRequest.SCOPE_REQUEST) != null;
        if(!isFlowRequest) {

            try {
                super.postHandle(request, model);
            } finally {
                SessionHolder sessionHolder =
                        (SessionHolder) TransactionSynchronizationManager.getResource(getSessionFactory());

                Session session = sessionHolder.getSession();
                session.setFlushMode(FlushMode.MANUAL);
            }

        }
    }

    public void afterCompletion(WebRequest request, Exception ex) throws DataAccessException {
        final boolean isWebRequest = request.getAttribute(IS_FLOW_REQUEST_ATTRIBUTE, WebRequest.SCOPE_REQUEST) != null;
        if(!isWebRequest) {
            request = (WebRequest) RequestContextHolder.currentRequestAttributes();
            if(request instanceof GrailsWebRequest) {
                GrailsWebRequest webRequest = (GrailsWebRequest) request;
                HttpServletResponse response = webRequest.getCurrentResponse();
                if(response instanceof GrailsContentBufferingResponse) {
                    GrailsContentBufferingResponse bufferingResponse = (GrailsContentBufferingResponse) response;
                    // if Sitemesh is still active disconnect the session, but don't close the session 
                    if(bufferingResponse.isActive()) {
                        try {
                            Session session = SessionFactoryUtils.getSession(getSessionFactory(), false);
                            if(session!=null) {
                                session.disconnect();
                            }
                        }
                        catch (IllegalStateException e) {
                            super.afterCompletion(request, ex);
                        }
                    }
                    else {
                        super.afterCompletion(request, ex);
                    }
                }
                else {
                    super.afterCompletion(request, ex);
                }

            }
            else {
                super.afterCompletion(request, ex);
            }
        }
    }

    protected void flushIfNecessary(Session session, boolean existingTransaction) throws HibernateException {
        if(session != null && session.getFlushMode() != FlushMode.MANUAL) {
            super.flushIfNecessary(session, existingTransaction);
        }
    }

}
