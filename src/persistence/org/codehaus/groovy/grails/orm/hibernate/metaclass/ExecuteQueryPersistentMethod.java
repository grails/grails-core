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

import groovy.lang.MissingMethodException;

import java.util.*;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException;
import org.hibernate.SessionFactory;

/**
 * Allows the executing of abituary HQL queries
 * 
 * eg. Account.executeQuery( "select distinct a.number from Account a where a.branch = ?", 'London' )
 * or  Account.executeQuery( "select distinct a.number from Account a where a.branch = :branch", [branch:'London'] )
 * 
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 * @since 30-Apr-2006
 * @see <a href="http://www.hibernate.org/hib_docs/reference/en/html/queryhql.html">http://www.hibernate.org/hib_docs/reference/en/html/queryhql.html</a>
 */
public class ExecuteQueryPersistentMethod
		extends AbstractStaticPersistentMethod {

	private static final String		METHOD_SIGNATURE	= "executeQuery";
	private static final Pattern	METHOD_PATTERN		= Pattern.compile("^executeQuery$");

	public ExecuteQueryPersistentMethod(SessionFactory sessionFactory,
			ClassLoader classLoader) {
		super(sessionFactory, classLoader, METHOD_PATTERN);
	}

	protected Object doInvokeInternal(Class clazz, String methodName, Object[] arguments) {
		// if no arguments passed throw exception
		if (arguments.length == 0)
			throw new MissingMethodException(METHOD_SIGNATURE, clazz, arguments);

		if (arguments.length == 1) {
			return getHibernateTemplate().find(arguments[0].toString());
		} else if (arguments.length == 2) {
			if (arguments[1] instanceof Collection) {
				return getHibernateTemplate().find(arguments[0].toString(), GrailsClassUtils.collectionToObjectArray((Collection) arguments[1]));
			} else if (arguments[1] instanceof Map) {
				Map paramsMap = (Map) arguments[1];
				String[] paramNames = new String[paramsMap.size()];
				Object[] paramValues = new Object[paramsMap.size()];
				int index = 0;
				for (Iterator it = paramsMap.entrySet().iterator(); it.hasNext();) {
					Map.Entry entry = (Map.Entry) it.next();
					if (!(entry.getKey() instanceof String))
						throw new GrailsQueryException("Named parameter's name must be of type String");
					paramNames[index] = (String) entry.getKey();
					paramValues[index++] = entry.getValue();
				}
				return getHibernateTemplate().findByNamedParam(arguments[0].toString(), paramNames, paramValues);
			} else {
				return getHibernateTemplate().find(arguments[0].toString(), arguments[1]);
			}
		}
		throw new MissingMethodException(METHOD_SIGNATURE, clazz, arguments);
	}

}
