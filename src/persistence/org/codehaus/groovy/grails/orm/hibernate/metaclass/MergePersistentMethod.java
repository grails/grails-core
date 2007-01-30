/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import java.util.regex.Pattern;
import java.sql.SQLException;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.hibernate.*;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * The merge() method follows the semantics of merge which attempts to "merge" an object
 * with a long lived session
 * 
 * @author Graeme Rocher
 * @since 0.3
 *
 */
public class MergePersistentMethod extends AbstractSavePersistentMethod {

    public static final String METHOD_SIGNATURE = "merge";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^'+METHOD_SIGNATURE+'$');

   
	public MergePersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader, GrailsApplication application) {
		super(METHOD_PATTERN, sessionFactory, classLoader, application);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractSavePersistentMethod#performSave(java.lang.Object)
	 */
	protected void performSave(final Object target, final boolean flush) {
        HibernateTemplate ht = getHibernateTemplate();

        ht.execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.merge(target);
                session.lock(target, LockMode.NONE);

                if(flush && FlushMode.isManualFlushMode(session.getFlushMode()))
                    getHibernateTemplate().flush();
                return target;
            }
        });
    }

}
