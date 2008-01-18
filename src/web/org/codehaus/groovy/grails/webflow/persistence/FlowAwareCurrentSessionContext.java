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

import org.hibernate.HibernateException;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.springframework.orm.hibernate3.SpringSessionContext;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.execution.FlowExecutionContext;
import org.springframework.webflow.execution.FlowExecutionContextHolder;

/**
 * A Hibernate CurrentSessionContext that is aware of flow requests and looks up the Session from the flow
 * instead of the current Thread
 *
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Jan 18, 2008
 */
public class FlowAwareCurrentSessionContext extends SpringSessionContext{
    /**
     * Create a new SpringSessionContext for the given Hibernate SessionFactory.
     *
     * @param sessionFactory the SessionFactory to provide current Sessions for
     */
    public FlowAwareCurrentSessionContext(SessionFactoryImplementor sessionFactory) {
        super(sessionFactory);
    }

    public Session currentSession() throws HibernateException {
        try {
            FlowExecutionContext context = FlowExecutionContextHolder.getFlowExecutionContext();
            MutableAttributeMap flowScope = context.getActiveSession().getScope();
            Session s = (Session)flowScope.get("session", org.hibernate.Session.class);
            if(s == null) return super.currentSession();
            else return s;
        } catch (IllegalStateException e) {
            return super.currentSession();
        }
    }
}
