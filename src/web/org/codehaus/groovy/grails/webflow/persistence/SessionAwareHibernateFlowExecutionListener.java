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

import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.execution.FlowSession;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.persistence.HibernateFlowExecutionListener;

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


    /**
     * Create a new Hibernate Flow Execution Listener using giving Hibernate session factory and transaction manager.
     *
     * @param sessionFactory     the session factory to use
     * @param transactionManager the transaction manager to drive transactions
     */
    public SessionAwareHibernateFlowExecutionListener(SessionFactory sessionFactory, PlatformTransactionManager transactionManager) {
        super(sessionFactory, transactionManager);
        this.localSessionFactory = sessionFactory;
    }

    public void sessionStarting(RequestContext context, FlowSession session, MutableAttributeMap input) {
        if (!TransactionSynchronizationManager.hasResource(localSessionFactory)) {
            super.sessionStarting(context, session, input);    
        }
        else {
            obtainCurrentSession(context);
       }
    }

	public void sessionEnded(RequestContext context, FlowSession session, String outcome, AttributeMap output) {
		if (isPersistenceContext(session.getDefinition() ) && session.isRoot()) {
            super.sessionEnded(context, session, outcome, output);
        }
        else {
            super.paused(context);
        }
	}


    private boolean isPersistenceContext(FlowDefinition flow) {
        return flow.getAttributes().contains(PERSISTENCE_CONTEXT_ATTRIBUTE);
    }


    private void obtainCurrentSession(RequestContext context) {
        MutableAttributeMap flowScope = context.getFlowScope();
        if(flowScope.get(PERSISTENCE_CONTEXT_ATTRIBUTE) == null) {
            SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(localSessionFactory);
            if(sessionHolder!=null)
                flowScope.put(PERSISTENCE_CONTEXT_ATTRIBUTE, sessionHolder.getSession());
        }
    }

    public void resuming(RequestContext context) {
        if (!TransactionSynchronizationManager.hasResource(localSessionFactory)) {
             super.resuming(context);
        }
        else {
             obtainCurrentSession(context);
        }

    }
}
