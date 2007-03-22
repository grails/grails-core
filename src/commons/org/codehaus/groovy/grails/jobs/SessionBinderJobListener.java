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
package org.codehaus.groovy.grails.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * JobListener implementation which binds Hibernate Session to thread
 * before execution of job and flushes it after job execution.
 * 
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
public class SessionBinderJobListener extends JobListenerSupport {
	private static final transient Log LOG = LogFactory.getLog(SessionBinderJobListener.class);
	
	public static final String NAME = "sessionBinderListener";

	private SessionFactory sessionFactory;

	public String getName() {
		return NAME;
	}

	public void jobToBeExecuted(JobExecutionContext context) {
		Session session = SessionFactoryUtils.getSession(sessionFactory, true);
        session.setFlushMode(FlushMode.AUTO);
		TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		if( LOG.isDebugEnabled()) LOG.debug("Hibernate Session is bounded to Job thread");
	}

	public void jobWasExecuted(JobExecutionContext context, JobExecutionException exception) {
		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
        if(!FlushMode.MANUAL.equals(sessionHolder.getSession().getFlushMode())) {
            sessionHolder.getSession().flush();
        }
		SessionFactoryUtils.closeSession(sessionHolder.getSession());				
		if( LOG.isDebugEnabled()) LOG.debug("Hibernate Session is unbounded from Job thread and closed");
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	
}
