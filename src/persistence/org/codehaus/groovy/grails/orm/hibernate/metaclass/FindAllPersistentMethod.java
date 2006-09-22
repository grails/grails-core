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

import groovy.lang.MissingMethodException;
import groovy.lang.GString;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;
import org.springframework.orm.hibernate3.HibernateCallback;

/**
 * 
 * 
 * @author Steven Devijver
 * @since Aug 8, 2005
 */
public class FindAllPersistentMethod extends AbstractStaticPersistentMethod {

	public FindAllPersistentMethod(SessionFactory sessionFactory,
			ClassLoader classLoader) {
		super(sessionFactory, classLoader, Pattern.compile("^findAll$"));
	}

	protected Object doInvokeInternal(final Class clazz, String methodName,
			final Object[] arguments) {
		if(arguments.length == 0)
			return getHibernateTemplate().loadAll(clazz);
		
		final Object arg = arguments[0] instanceof GString ? arguments[0].toString() :arguments[0];
		
		// if the arg is an instance of the class find by example
		if(arg instanceof String) {
			final String query = ((String)arg).trim();
			final String shortName = GrailsClassUtils.getShortName(clazz);
			if(!query.matches( "from ["+clazz.getName()+"|"+shortName+"].*" )) {
				throw new GrailsQueryException("Invalid query ["+query+"] for domain class ["+clazz+"]");
			}			
			return super.getHibernateTemplate().executeFind( new HibernateCallback() {

				public Object doInHibernate(Session session) throws HibernateException, SQLException {										
					Query q = session.createQuery(query);
					Object[] queryArgs = null;
					int max;
                    int offset;
                    if(arguments.length > 1) {
						if(arguments[1] instanceof List) {
							queryArgs = ((List)arguments[1]).toArray();
						}
						else if(arguments[1].getClass().isArray()) {
							queryArgs = (Object[])arguments[1];
						}
					}					
					max = retrieveMaxValue(arguments);
                    offset = retrieveOffsetValue(arguments);
                    if(queryArgs != null) {
						for (int i = 0; i < queryArgs.length; i++) {
                            if(queryArgs[0] instanceof GString) {
                                q.setParameter(i,queryArgs[i].toString());
                            }   else {
                                q.setParameter(i, queryArgs[i]);
                            }
						}
					}
					if(max > 0) {
						q.setMaxResults(max);
					}
                    if(offset > 0) {
                        q.setFirstResult(offset);
                    }
                    return q.list();

				}

				private int retrieveMaxValue(Object[] arguments) {
					int result = -1;
					if(arguments.length > 1) {
                        result = retrieveInt(ARGUMENT_MAX, arguments[1]);

                        if(arguments.length > 2 && result == -1) {
                            result = retrieveInt(ARGUMENT_MAX, arguments[2]);
						}
					}
					
					return result;
				}

				private int retrieveOffsetValue(Object[] arguments) {
					int result = -1;
					if(arguments.length > 1) {
                        result = retrieveInt(ARGUMENT_OFFSET, arguments[1]);

                        if(arguments.length > 2 && result == -1) {
                            result = retrieveInt(ARGUMENT_OFFSET, arguments[2]);
						}
					}

					return result;
				}

                private int retrieveInt(String name, Object arg) {
                    if(arg instanceof Integer) {
                        return ((Integer)arg).intValue();
                    }
                    else if(arg instanceof Map) {
                        Object value = ((Map)arg).get(name);
                        if(value instanceof Integer) {
                            return ((Integer)value).intValue();
                        }
                    }
                    return -1;
                }

            });
		}
		if(clazz.isAssignableFrom( arg.getClass() )) {			
			return super.getHibernateTemplate().executeFind( new HibernateCallback() {

				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					
					Example example = Example.create(arg)
							.ignoreCase();
					
					Criteria crit = session.createCriteria(clazz);
					crit.add(example);
					
					return crit.list();					
				}
				
			});			
		}

		throw new MissingMethodException(methodName,clazz,arguments);
	}

}
