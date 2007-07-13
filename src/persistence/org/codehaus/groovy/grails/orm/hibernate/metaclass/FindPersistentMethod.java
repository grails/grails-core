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

import groovy.lang.GString;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException;
import org.hibernate.*;
import org.hibernate.criterion.Example;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * The "find" persistent static method allows searching for instances using
 * either an example instance or an HQL query. This method returns the first
 * result of the query. A GrailsQueryException is thrown if the query is not a
 * valid query for the domain class.
 * 
 * <p>
 * Examples in Groovy: <code>
 * 		// retrieve the first account ordered by account number
 * 		def a = Account.find("from Account as a order by a.number asc" )
 * 
 * 		// with query parameters
 * 		def a  = Account.find("from Account as a where a.number = ? and a.branch = ?", [38479, "London"]) 
 * 
 * 		// with query named parameters
 * 		def a  = Account.find("from Account as a where a.number = :number and a.branch = :branch", [number:38479, branch:"London"]) 

 * 		// query by example
 * 		def a = new Account()
 * 		a.number = 495749357
 * 		def a = Account.find(a)
 * 
 * </code>
 * 
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 * @since 31-Aug-2005
 * 
 */
public class FindPersistentMethod
		extends AbstractStaticPersistentMethod {

	private static final String	METHOD_PATTERN	= "^find$";

	public FindPersistentMethod(SessionFactory sessionFactory,
			ClassLoader classLoader) {
		super(sessionFactory, classLoader, Pattern.compile(METHOD_PATTERN));
	}

	protected Object doInvokeInternal(final Class clazz, String methodName, final Object[] arguments) {

		if (arguments.length == 0)
			throw new MissingMethodException(methodName, clazz, arguments);

		final Object arg = arguments[0] instanceof GString ? arguments[0].toString() : arguments[0];

		if (arg instanceof String) {
			final String query = (String) arg;
			final String shortName = GrailsClassUtils.getShortName(clazz);
			if (!query.matches("from [" + clazz.getName() + "|" + shortName	+ "].*")) {
				throw new GrailsQueryException("Invalid query [" + query + "] for domain class [" + clazz + "]");
			}
			return super.getHibernateTemplate().execute(new HibernateCallback() {

				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					Query q = session.createQuery(query);
					Object[] queryArgs = null;
					Map queryNamedArgs = null;

					if (arguments.length > 1) {
						if (arguments[1] instanceof Collection) {
							queryArgs = GrailsClassUtils.collectionToObjectArray((Collection) arguments[1]);
						} else if (arguments[1].getClass().isArray()) {
							queryArgs = (Object[]) arguments[1];
						} else if (arguments[1] instanceof Map) {
							queryNamedArgs = (Map) arguments[1];
						}
					}
					if (queryArgs != null) {
						for (int i = 0; i < queryArgs.length; i++) {
							if (queryArgs[0] instanceof GString) {
								q.setParameter(i, queryArgs[i].toString());
							} else {
								q.setParameter(i, queryArgs[i]);
							}
						}
					}
					if (queryNamedArgs != null) {
						for (Iterator it = queryNamedArgs.entrySet().iterator(); it.hasNext();) {
							Map.Entry entry = (Map.Entry) it.next();
							if (!(entry.getKey() instanceof String))
								throw new GrailsQueryException("Named parameter's name must be String: "
										+ queryNamedArgs.toString());
							String stringKey = (String) entry.getKey();
							Object value = entry.getValue();
							if (value instanceof GString) {
								q.setParameter(stringKey, value.toString());
							} else if (List.class.isAssignableFrom(value.getClass())) {
								q.setParameterList(stringKey, (List) value);
							} else if (value.getClass().isArray()) {
								q.setParameterList(stringKey, (Object[]) value);
							} else {
								q.setParameter(stringKey, value);
							}
						}
					}
					// only want one result, could have used uniqueObject here
					// but it throws an exception if its not unique which is
					// undesirable
					q.setMaxResults(1);
					List results = q.list();
					if (results.size() > 0)
						return results.get(0);
					return null;
				}
			});
		} else if (clazz.isAssignableFrom(arg.getClass())) {
			// if the arg is an instance of the class find by example
			return super.getHibernateTemplate().execute(new HibernateCallback() {

				public Object doInHibernate(Session session) throws HibernateException, SQLException {

					Example example = Example.create(arg).ignoreCase();

					Criteria crit = session.createCriteria(clazz);
					crit.add(example);
                    crit.setMaxResults(1);
					List results = crit.list();
					if (results.size() > 0)
						return results.get(0);
					return null;
				}

			});
		}

		throw new MissingMethodException(methodName, clazz, arguments);
	}

}
