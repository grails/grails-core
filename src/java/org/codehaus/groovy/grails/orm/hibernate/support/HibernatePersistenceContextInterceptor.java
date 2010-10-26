/*
 * Copyright 2004-2006 Graeme Rocher
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Graeme Rocher
 * @since 0.4
 */
public class HibernatePersistenceContextInterceptor implements PersistenceContextInterceptor {

    private static final Log LOG = LogFactory.getLog(HibernatePersistenceContextInterceptor.class);
    private SessionFactory sessionFactory;
    private boolean participate;
    private int nestingCount = 0;

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.PersistenceContextInterceptor#destroy()
     */
    public void destroy() {
        if (--nestingCount > 0 || participate) {
            return;
        }

        // single session mode
        SessionHolder holder = (SessionHolder)TransactionSynchronizationManager.unbindResource(sessionFactory);
        LOG.debug("Closing single Hibernate session in GrailsDispatcherServlet");
        try {
            Session session = holder.getSession();
            SessionFactoryUtils.closeSession(session);
        }
        catch (RuntimeException ex) {
            LOG.error("Unexpected exception on closing Hibernate Session", ex);
        }
    }

    public void disconnect() {
        try {
            getSession(false).disconnect();
        }
        catch (IllegalStateException e) {
            // no session ignore
        }
    }

    @SuppressWarnings("deprecation")
    public void reconnect() {
        getSession().reconnect();
    }

    public void flush() {
        getSession().flush();
    }

    public void clear() {
        getSession().clear();
    }

    public void setReadOnly() {
        getSession().setFlushMode(FlushMode.MANUAL);
    }

    public void setReadWrite() {
        getSession().setFlushMode(FlushMode.AUTO);
    }

    public boolean isOpen() {
        try {
            return getSession(false).isOpen();
        }
        catch (IllegalStateException e) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.support.PersistenceContextInterceptor#init()
     */
    public void init() {
        if (++nestingCount > 1) {
            return;
        }
        if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
            // Do not modify the Session: just set the participate flag.
            participate = true;
        }
        else {
            LOG.debug("Opening single Hibernate session in HibernatePersistenceContextInterceptor");
            Session session = getSession();
            session.enableFilter("dynamicFilterEnabler"); // work around for HHH-2624
            session.setFlushMode(FlushMode.AUTO);
            TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
        }
    }

    private Session getSession() {
        return getSession(true);
    }

    private Session getSession(boolean allowCreate) {
        return SessionFactoryUtils.getSession(sessionFactory, allowCreate);
    }

    /**
     * @return the sessionFactory
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * @param sessionFactory the sessionFactory to set
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
}
