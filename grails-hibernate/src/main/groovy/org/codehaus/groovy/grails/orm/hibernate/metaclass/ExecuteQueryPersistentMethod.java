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
import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException;
import org.hibernate.*;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

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
    private static final String METHOD_SIGNATURE = "executeQuery";
    private static final Pattern METHOD_PATTERN = Pattern.compile("^executeQuery$");

    @SuppressWarnings("serial")
    private static final List<String> QUERY_META_PARAMS = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add(GrailsHibernateUtil.ARGUMENT_MAX);
                add(GrailsHibernateUtil.ARGUMENT_OFFSET);
                add(GrailsHibernateUtil.ARGUMENT_CACHE);
                add(GrailsHibernateUtil.ARGUMENT_FLUSH_MODE);
                add(GrailsHibernateUtil.ARGUMENT_TIMEOUT);
                add(GrailsHibernateUtil.ARGUMENT_FETCH_SIZE);
                add(GrailsHibernateUtil.ARGUMENT_READ_ONLY);
            }}
    );

    public ExecuteQueryPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader, GrailsApplication application) {
        super(sessionFactory, classLoader, METHOD_PATTERN, application);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object doInvokeInternal(Class clazz, String methodName, DetachedCriteria additionalCriteria, Object[] arguments) {
        return doInvokeInternal(clazz,methodName, (Closure) null,arguments) ;
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
                getHibernateTemplate().applySettings(q);
                SimpleTypeConverter converter = new SimpleTypeConverter();
                // process paginate params
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_MAX)) {
                    Integer maxParam = converter.convertIfNecessary(queryMetaParams.get(GrailsHibernateUtil.ARGUMENT_MAX), Integer.class);
                    q.setMaxResults(maxParam.intValue());
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_OFFSET)) {
                    Integer offsetParam = converter.convertIfNecessary(queryMetaParams.get(GrailsHibernateUtil.ARGUMENT_OFFSET), Integer.class);
                    q.setFirstResult(offsetParam.intValue());
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_CACHE)) {
                    q.setCacheable((Boolean)queryMetaParams.get(GrailsHibernateUtil.ARGUMENT_CACHE));
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_FETCH_SIZE)) {
                    Integer fetchSizeParam = converter.convertIfNecessary(queryMetaParams.get(GrailsHibernateUtil.ARGUMENT_FETCH_SIZE), Integer.class);
                    q.setFetchSize(fetchSizeParam.intValue());
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_TIMEOUT)) {
                    Integer timeoutParam = converter.convertIfNecessary(queryMetaParams.get(GrailsHibernateUtil.ARGUMENT_TIMEOUT), Integer.class);
                    q.setTimeout(timeoutParam.intValue());
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_READ_ONLY)) {
                    q.setReadOnly((Boolean)queryMetaParams.get(GrailsHibernateUtil.ARGUMENT_READ_ONLY));
                }
                if (queryMetaParams.containsKey(GrailsHibernateUtil.ARGUMENT_FLUSH_MODE)) {
                    q.setFlushMode((FlushMode)queryMetaParams.get(GrailsHibernateUtil.ARGUMENT_FLUSH_MODE));
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
                    if (!QUERY_META_PARAMS.contains(parameterName)) {
                        Object parameterValue = entry.getValue();
                        if (parameterValue == null) {
                            throw new IllegalArgumentException("Named parameter [" + entry.getKey() + "] value may not be null");
                        }
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
            for (String queryMetaParam : QUERY_META_PARAMS) {
                if (sourceMap.containsKey(queryMetaParam)) {
                    result.put(queryMetaParam, sourceMap.get(queryMetaParam));
                }
            }
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
