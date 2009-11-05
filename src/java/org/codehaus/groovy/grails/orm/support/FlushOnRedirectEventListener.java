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
package org.codehaus.groovy.grails.orm.support;

import org.codehaus.groovy.grails.web.servlet.mvc.RedirectEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;

/**
 * Flushes the session on a redirect
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class FlushOnRedirectEventListener implements RedirectEventListener{
    private SessionFactory sessionFactory;

    public FlushOnRedirectEventListener(SessionFactory sf) {
        this.sessionFactory = sf;
    }

    public void responseRedirected(String url) {
        HibernateTemplate ht = new HibernateTemplate(sessionFactory);
        ht.execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.flush();
                return null;
            }
        });
    }
}
