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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import org.hibernate.SessionFactory;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;

import java.util.regex.Pattern;
import java.util.Collection;

import groovy.lang.MissingMethodException;

/**
 * Allows the execution of update queries such as DML updates
 *
 *
 * eg. Account.executeUpdate( "delete Account"); // deletes all accounts (eek!)
 *
 * @author Graeme Rocher
 * @since 13-Sep-2006
 */
public class ExecuteUpdatePersistentMethod extends AbstractStaticPersistentMethod {

    public static final Pattern METHOD_PATTERN = Pattern.compile("^executeUpdate$");

    public ExecuteUpdatePersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader) {
        super(sessionFactory, classLoader, METHOD_PATTERN);
    }

    protected Object doInvokeInternal(Class clazz, String methodName, Object[] arguments) {
        if(arguments.length == 0)
            throw new MissingMethodException("executeUpdate",clazz,arguments);

        int result;
        switch(arguments.length) {
            case 1:
                 result = getHibernateTemplate().bulkUpdate(arguments[0].toString());
                 return new Integer(result);
            case 2:
                String query = arguments[0].toString();
                Object args = arguments[1];
                if(args.getClass().isArray()) {
                   result = getHibernateTemplate().bulkUpdate(query, (Object[])args);
                }
                else if(args instanceof Collection) {
                   result = getHibernateTemplate().bulkUpdate(query, GrailsClassUtils.collectionToObjectArray((Collection)args)); 
                }
                else {
                   result = getHibernateTemplate().bulkUpdate(query, args);
                }
                return new Integer(result);

        }
        throw new MissingMethodException("executeUpdate",clazz,arguments);
    }
}
