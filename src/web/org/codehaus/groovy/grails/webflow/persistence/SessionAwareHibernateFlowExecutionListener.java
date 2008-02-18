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
package org.codehaus.groovy.grails.webflow.persistence;

import org.springframework.webflow.persistence.HibernateFlowExecutionListener;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.FlowSession;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.orm.hibernate3.SessionHolder;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.FlushMode;

/**
 * Extends the HibernateFlowExecutionListener and doesn't bind a session if one is already present
 *
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Feb 12, 2008
 */
public class SessionAwareHibernateFlowExecutionListener extends HibernateFlowExecutionListener {
    private SessionFactory localSessionFactory;
    private TransactionTemplate localTransactionTemplate;
    private static final String HIBERNATE_SESSION_ATTRIBUTE = "session";
    private static final String PERSISTENCE_CONTEXT_ATTRIBUTE = "persistenceContext";

    /**
     * Create a new Hibernate Flow Execution Listener using giving Hibernate session factory and transaction manager.
     *
     * @param sessionFactory     the session factory to use
     * @param transactionManager the transaction manager to drive transactions
     */
    public SessionAwareHibernateFlowExecutionListener(SessionFactory sessionFactory, PlatformTransactionManager transactionManager) {
        super(sessionFactory, transactionManager);
        this.localSessionFactory = sessionFactory;
        this.localTransactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void sessionCreated(RequestContext context, FlowSession session) {
        if (!TransactionSynchronizationManager.hasResource(localSessionFactory)) {
            super.sessionCreated(context, session);
        }
        else {
            obtainCurrentSession(context);
        }
        
    }

    public void sessionEnded(RequestContext context, FlowSession session, AttributeMap output) {
		if (isPersistenceContext(session.getDefinition())) {
			final Session hibernateSession = (Session) session.getScope().remove(HIBERNATE_SESSION_ATTRIBUTE);
            if(hibernateSession.isOpen()) {
                Boolean commitStatus = session.getState().getAttributes().getBoolean("commit");
                if (Boolean.TRUE.equals(commitStatus)) {
                    try {
                        if(TransactionSynchronizationManager.hasResource(localSessionFactory)) {
                            SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(localSessionFactory);
                            sessionHolder.addSession(hibernateSession);
                        }
                        localTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                localSessionFactory.getCurrentSession();
                                // nothing to do; a flush will happen on commit automatically as this is a read-write
                                // transaction
                            }
                        });
                    } finally {
                        if(TransactionSynchronizationManager.hasResource(localSessionFactory)) {
                            SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(localSessionFactory);
                            sessionHolder.removeSession(hibernateSession);
                        }

                    }
                }
                unbind(hibernateSession);
                hibernateSession.close();
            }
		}
    }

    private boolean isPersistenceContext(FlowDefinition flow) {
        return flow.getAttributes().contains(PERSISTENCE_CONTEXT_ATTRIBUTE);
    }

    private void unbind(Session session) {
		if (TransactionSynchronizationManager.hasResource(localSessionFactory)) {
			TransactionSynchronizationManager.unbindResource(localSessionFactory);
		}
	}

    private Session createSession() {
		Session session = localSessionFactory.openSession();
		session.setFlushMode(FlushMode.MANUAL);
		return session;
	}

    private void obtainCurrentSession(RequestContext context) {
        MutableAttributeMap flowScope = context.getFlowScope();
        if(flowScope.get(HIBERNATE_SESSION_ATTRIBUTE) == null) {
            SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(localSessionFactory);
            if(sessionHolder!=null)
                flowScope.put(HIBERNATE_SESSION_ATTRIBUTE, sessionHolder.getSession());
        }
    }

    public void resumed(RequestContext context) {
        if (!TransactionSynchronizationManager.hasResource(localSessionFactory)) {
            super.resumed(context);
        }
        else {
            obtainCurrentSession(context);
        }
    }
}
