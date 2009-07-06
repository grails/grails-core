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

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * This method follows the semantics of saveOrUpdate of scheduling the object
 * for persistence when a flush occurs.
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
 * 
 * @since 0.1
 *
 * Created: Aug 7, 2005
 */
public class SavePersistentMethod extends AbstractSavePersistentMethod {

    public static final String METHOD_SIGNATURE = "save";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^'+METHOD_SIGNATURE+'$');



    public SavePersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader, GrailsApplication application) {
        super(METHOD_PATTERN,sessionFactory, classLoader, application);
    }

	protected Object performSave(final Object target, final boolean flush) {
        HibernateTemplate ht = getHibernateTemplate();
        return ht.execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.saveOrUpdate(target);
                if(flush)
                    getHibernateTemplate().flush();
                return target;
            }
        });
	}

    protected Object performInsert(final Object target, final boolean shouldFlush) {
        HibernateTemplate ht = getHibernateTemplate();
        return ht.execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.save(target);
                if(shouldFlush)
                    getHibernateTemplate().flush();
                return target;
            }
        });

    }

}
