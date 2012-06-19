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

import grails.gorm.DetachedCriteria;
import grails.util.GrailsNameUtils;
import groovy.lang.Closure;
import groovy.lang.MissingMethodException;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.orm.hibernate3.HibernateCallback;

/**
 * <p>
 * The "find" persistent static method allows searching for instances using
 * either an example instance or an HQL query. This method returns the first
 * result of the query. A GrailsQueryException is thrown if the query is not a
 * valid query for the domain class.
 *
 * <p>
 * Examples in Groovy: <code>
 *         // retrieve the first account ordered by account number
 *         def a = Account.find("from Account as a order by a.number asc")
 *
 *         // with query parameters
 *         def a  = Account.find("from Account as a where a.number = ? and a.branch = ?", [38479, "London"])
 *
 *         // with query named parameters
 *         def a  = Account.find("from Account as a where a.number = :number and a.branch = :branch", [number:38479, branch:"London"])

 *         // query by example
 *         def a = new Account()
 *         a.number = 495749357
 *         def a = Account.find(a)
 *
 * </code>
 *
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 */
public class FindPersistentMethod extends AbstractStaticPersistentMethod {

    private static final String    METHOD_PATTERN    = "^find$";

    public FindPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader, GrailsApplication application) {
        super(sessionFactory, classLoader, Pattern.compile(METHOD_PATTERN), application);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object doInvokeInternal(Class clazz, String methodName, DetachedCriteria additionalCriteria, Object[] arguments) {
        return doInvokeInternal(clazz,methodName, (Closure) null,arguments) ;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Object doInvokeInternal(final Class clazz, String methodName, Closure additionalCriteria, final Object[] arguments) {

        if (arguments.length == 0) {
            throw new MissingMethodException(methodName, clazz, arguments);
        }

        final Object arg = arguments[0] instanceof CharSequence ? arguments[0].toString() : arguments[0];

        if (arg instanceof String) {
            final String query = (String) arg;
            final String shortName = GrailsNameUtils.getShortName(clazz);
            if (!query.matches("(?i)from(?-i)\\s+[" + clazz.getName() + "|" + shortName    + "].*")) {
                throw new GrailsQueryException("Invalid query [" + query + "] for domain class [" + clazz + "]");
            }

            return getHibernateTemplate().execute(new HibernateCallback<Object>() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(query);
                    getHibernateTemplate().applySettings(q);
                    Object[] queryArgs = null;
                    Map queryNamedArgs = null;
                    boolean useCache = useCache(arguments);

                    if (arguments.length > 1) {
                        if (arguments[1] instanceof Collection) {
                            queryArgs = GrailsClassUtils.collectionToObjectArray((Collection) arguments[1]);
                        }
                        else if (arguments[1].getClass().isArray()) {
                            queryArgs = (Object[]) arguments[1];
                        }
                        else if (arguments[1] instanceof Map) {
                            queryNamedArgs = (Map) arguments[1];
                        }
                    }
                    if (queryArgs != null) {
                        for (int i = 0; i < queryArgs.length; i++) {
                            if (queryArgs[i] instanceof CharSequence) {
                                q.setParameter(i, queryArgs[i].toString());
                            }
                            else {
                                q.setParameter(i, queryArgs[i]);
                            }
                        }
                    }
                    if (queryNamedArgs != null) {
                        for (Iterator it = queryNamedArgs.entrySet().iterator(); it.hasNext();) {
                            Map.Entry entry = (Map.Entry) it.next();
                            if (!(entry.getKey() instanceof String)) {
                                throw new GrailsQueryException("Named parameter's name must be String: " +
                                        queryNamedArgs.toString());
                            }
                            String stringKey = (String) entry.getKey();
                            Object value = entry.getValue();

                            if (GrailsHibernateUtil.ARGUMENT_CACHE.equals(stringKey)) {
                                continue;
                            }
                            else if (value instanceof CharSequence) {
                                q.setParameter(stringKey, value.toString());
                            }
                            else if (List.class.isAssignableFrom(value.getClass())) {
                                q.setParameterList(stringKey, (List) value);
                            }
                            else if (value.getClass().isArray()) {
                                q.setParameterList(stringKey, (Object[]) value);
                            }
                            else {
                                q.setParameter(stringKey, value);
                            }
                        }
                    }
                    // only want one result, could have used uniqueObject here
                    // but it throws an exception if its not unique which is undesirable
                    q.setMaxResults(1);
                    q.setCacheable(useCache);
                    List results = q.list();
                    if (results.size() > 0) {
                        return GrailsHibernateUtil.unwrapIfProxy(results.get(0));
                    }
                    return null;
                }

                private boolean useCache(Object[] args) {
                    boolean useCache = getHibernateTemplate().isCacheQueries();
                    if (args.length > 1 && args[args.length - 1] instanceof Map) {
                        Object param = args[args.length - 1];
                        String key = GrailsHibernateUtil.ARGUMENT_CACHE;
                        boolean value = false;
                        if ((param instanceof Map) && ((Map)param).containsKey(key)) {
                            SimpleTypeConverter converter = new SimpleTypeConverter();
                            value = converter.convertIfNecessary(((Map)param).get(key), Boolean.class);
                        }
                        useCache = value;
                    }
                    return useCache;
                }
            });
        }
        else if (clazz.isAssignableFrom(arg.getClass())) {
            // if the arg is an instance of the class find by example
            return getHibernateTemplate().execute(new HibernateCallback<Object>() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {

                    Example example = Example.create(arg).ignoreCase();

                    Criteria crit = session.createCriteria(clazz);
                    getHibernateTemplate().applySettings(crit);
                    crit.add(example);
                    crit.setMaxResults(1);
                    List results = crit.list();
                    if (results.size() > 0) {
                        return results.get(0);
                    }
                    return null;
                }
            });
        }

        throw new MissingMethodException(methodName, clazz, arguments);
    }
}
