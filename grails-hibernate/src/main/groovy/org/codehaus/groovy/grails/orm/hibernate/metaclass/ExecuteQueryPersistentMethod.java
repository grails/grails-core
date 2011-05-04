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

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.FlushMode;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.orm.hibernate3.HibernateCallback;

/**
 * Allows the executing of arbitrary HQL queries.
 * <p/>
 * eg. Account.executeQuery("select distinct a.number from Account a where a.branch = ?", 'London') or
 * Account.executeQuery("select distinct a.number from Account a where a.branch = :branch", [branch:'London'])
 *
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 * @see <a href="http://www.hibernate.org/hib_docs/reference/en/html/queryhql.html">http://www.hibernate.org/hib_docs/reference/en/html/queryhql.html</a>
 * @since 30-Apr-2006
 */
public class ExecuteQueryPersistentMethod extends AbstractStaticPersistentMethod {

    public static SimpleTypeConverter converter = new SimpleTypeConverter();
    private static final String METHOD_SIGNATURE = "executeQuery";
    private static final Pattern METHOD_PATTERN = Pattern.compile("^executeQuery$");

    public ExecuteQueryPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader) {
        super(sessionFactory, classLoader, METHOD_PATTERN);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object doInvokeInternal(Class clazz, String methodName, Closure additionalCriteria, Object[] arguments) {
        checkMethodSignature(clazz, arguments);

        final String query = arguments[0].toString();
        final Map queryMetaParams = extractQueryMetaParams(arguments);
        final List positionalParams = extractPositionalParams(arguments);
        final Map namedParams = extractNamedParams(arguments);

        return getHibernateTemplate().executeFind(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Query q = session.createQuery(query);

                // process paginate params
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_MAX)) {
                    Integer maxParam = converter.convertIfNecessary(queryMetaParams.get(GrailsHibernateUtil.ARGUMENT_MAX), Integer.class);
                    q.setMaxResults(maxParam.intValue());
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_OFFSET)) {
                    Integer offsetParam = converter.convertIfNecessary(queryMetaParams.remove(GrailsHibernateUtil.ARGUMENT_OFFSET), Integer.class);
                    q.setFirstResult(offsetParam.intValue());
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_CACHE)) {
                    q.setCacheable(((Boolean)queryMetaParams.remove(GrailsHibernateUtil.ARGUMENT_CACHE)).booleanValue());
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_FETCH_SIZE)) {
                    Integer fetchSizeParam = converter.convertIfNecessary(queryMetaParams.remove(GrailsHibernateUtil.ARGUMENT_FETCH_SIZE), Integer.class);
                    q.setFetchSize(fetchSizeParam.intValue());
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_TIMEOUT)) {
                    Integer timeoutParam = converter.convertIfNecessary(queryMetaParams.remove(GrailsHibernateUtil.ARGUMENT_TIMEOUT), Integer.class);
                    q.setFetchSize(timeoutParam.intValue());
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_READ_ONLY)) {
                    q.setReadOnly(((Boolean) queryMetaParams.remove(GrailsHibernateUtil.ARGUMENT_READ_ONLY)).booleanValue());
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_FLUSH_MODE)) {
                    q.setFlushMode((FlushMode) queryMetaParams.remove(GrailsHibernateUtil.ARGUMENT_FLUSH_MODE));
                }
                // process positional HQL params
                int index = 0;
                for (Object parameter : positionalParams) {
                    q.setParameter(index++, parameter instanceof CharSequence ? parameter.toString() : parameter);
                }
                // process named HQL params
                for (Object o : namedParams.entrySet()) {
                    Map.Entry entry = (Map.Entry) o;
                    if (!(entry.getKey() instanceof String)) {
                        throw new GrailsQueryException("Named parameter's name must be of type String");
                    }
                    String parameterName = (String) entry.getKey();
                    Object parameterValue = entry.getValue();
                    if (Collection.class.isAssignableFrom(parameterValue.getClass())) {
                        q.setParameterList(parameterName, (Collection) parameterValue);
                    }
                    else if (parameterValue.getClass().isArray()) {
                        q.setParameterList(parameterName, (Object[]) parameterValue);
                    }
                    else if (parameterValue instanceof CharSequence) {
                        q.setParameter(parameterName, parameterValue.toString());
                    }
                    else {
                        q.setParameter(parameterName, parameterValue);
                    }
                }
                return q.list();
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void checkMethodSignature(Class clazz, Object[] arguments) {
        boolean valid = true;
        if (arguments.length < 1) valid = false;
        else if (arguments.length == 3 && !(arguments[2] instanceof Map)) valid = false;
        else if (arguments.length > 3) valid = false;

        if (!valid) throw new MissingMethodException(METHOD_SIGNATURE, clazz, arguments);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Map extractQueryMetaParams(Object[] arguments) {
        Map result = new HashMap();
        int metaParamsIndex = 0;
        if (arguments.length == 2 && arguments[1] instanceof Map) metaParamsIndex = 1;
        else if (arguments.length == 3) metaParamsIndex = 2;
        if (metaParamsIndex > 0) {
            Map sourceMap = (Map) arguments[metaParamsIndex];
            if (sourceMap.containsKey(GrailsHibernateUtil.ARGUMENT_MAX)) result.put(GrailsHibernateUtil.ARGUMENT_MAX, sourceMap.remove(GrailsHibernateUtil.ARGUMENT_MAX));
            if (sourceMap.containsKey(GrailsHibernateUtil.ARGUMENT_OFFSET)) result.put(GrailsHibernateUtil.ARGUMENT_OFFSET, sourceMap.remove(GrailsHibernateUtil.ARGUMENT_OFFSET));
            if (sourceMap.containsKey(GrailsHibernateUtil.ARGUMENT_CACHE)) result.put(GrailsHibernateUtil.ARGUMENT_CACHE, sourceMap.remove(GrailsHibernateUtil.ARGUMENT_CACHE));
            if (sourceMap.containsKey(GrailsHibernateUtil.ARGUMENT_FLUSH_MODE)) result.put(GrailsHibernateUtil.ARGUMENT_FLUSH_MODE, sourceMap.remove(GrailsHibernateUtil.ARGUMENT_FLUSH_MODE));
            if (sourceMap.containsKey(GrailsHibernateUtil.ARGUMENT_TIMEOUT)) result.put(GrailsHibernateUtil.ARGUMENT_TIMEOUT, sourceMap.remove(GrailsHibernateUtil.ARGUMENT_TIMEOUT));
            if (sourceMap.containsKey(GrailsHibernateUtil.ARGUMENT_FETCH_SIZE)) result.put(GrailsHibernateUtil.ARGUMENT_FETCH_SIZE, sourceMap.remove(GrailsHibernateUtil.ARGUMENT_FETCH_SIZE));
            if (sourceMap.containsKey(GrailsHibernateUtil.ARGUMENT_READ_ONLY)) result.put(GrailsHibernateUtil.ARGUMENT_READ_ONLY, sourceMap.remove(GrailsHibernateUtil.ARGUMENT_READ_ONLY));
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List extractPositionalParams(Object[] arguments) {
        List result = new ArrayList();
        if (arguments.length < 2 || arguments[1] instanceof Map) return result;
        if (arguments[1] instanceof Collection) {
            result.addAll((Collection) arguments[1]);
        }
        else if (arguments[1].getClass().isArray()) {
            result.addAll(Arrays.asList((Object[]) arguments[1]));
        }
        else {
            result.add(arguments[1]);
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Map extractNamedParams(Object[] arguments) {
        Map result = new HashMap();
        if (arguments.length < 2 || !(arguments[1] instanceof Map)) return result;
        result.putAll((Map) arguments[1]);
        return result;
    }
}
