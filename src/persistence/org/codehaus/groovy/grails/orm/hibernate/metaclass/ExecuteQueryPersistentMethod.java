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

import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.SessionFactory;

/**
 * Allows the executing of abituary HQL queries {@link http://www.hibernate.org/hib_docs/reference/en/html/queryhql.html}
 * 
 * 
 * eg. Account.executeQuery( "select distinct a.number from Account a where a.branch = ?", 'London' );
 * 
 * @author Graeme Rocher
 * @since 30-Apr-2006
 */
public class ExecuteQueryPersistentMethod extends
		AbstractStaticPersistentMethod {
	
	private static final String METHOD_SIGNATURE = "executeQuery";
	private static final Pattern METHOD_PATTERN = Pattern.compile("^executeQuery$");

	public ExecuteQueryPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader) {
		super(sessionFactory, classLoader, METHOD_PATTERN);
	}

	protected Object doInvokeInternal(Class clazz, String methodName,
			Object[] arguments) {
		// if no arguments passed throw exception
		if(arguments.length == 0)
			throw new MissingMethodException(METHOD_SIGNATURE, clazz,arguments);
		
		if(arguments.length == 1) {
			return getHibernateTemplate().find(arguments[0].toString());			
		}
		else if(arguments.length == 2) {
			if(arguments[1] instanceof List) {
				List params = (List)arguments[1];
				return getHibernateTemplate().find(arguments[0].toString(), params.toArray(new Object[params.size()]));
			}
			else {
				return getHibernateTemplate().find(arguments[0].toString(),arguments[1]);
			}
		}		
		throw new MissingMethodException(METHOD_SIGNATURE, clazz,arguments);
	}

}
