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

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.springframework.orm.hibernate3.HibernateCallback;

/**
 * The "listOrderBy*" static persistent method. Allows ordered listing of instances based on their properties.
 *
 * eg.
 * Account.listOrderByHolder();
 * Account.listOrderByHolder(max); // max results
 *
 * @author Graeme
 */
public class ListOrderByPersistentMethod extends AbstractStaticPersistentMethod {

    private static final String METHOD_PATTERN = "(listOrderBy)(\\w+)";
    private final HibernateDatastore datastore;

    public ListOrderByPersistentMethod(HibernateDatastore datastore, GrailsApplication grailsApplication, SessionFactory sessionFactory, ClassLoader classLoader) {
        super(sessionFactory, classLoader, Pattern.compile(METHOD_PATTERN), grailsApplication);
        this.datastore = datastore;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object doInvokeInternal(Class clazz, String methodName, DetachedCriteria additionalCriteria, Object[] arguments) {
        return doInvokeInternal(clazz,methodName, (Closure) null,arguments) ;
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractStaticPersistentMethod#doInvokeInternal(java.lang.Class, java.lang.String, java.lang.Object[])
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Object doInvokeInternal(final Class clazz, String methodName, final Closure additionalCriteria, final Object[] arguments) {

        Matcher match = getPattern().matcher(methodName);
        match.find();

        String nameInSignature = match.group(2);
        final String propertyName = nameInSignature.substring(0,1).toLowerCase(Locale.ENGLISH) + nameInSignature.substring(1);

        return getHibernateTemplate().executeFind(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria crit = getCriteria(datastore, application, session, null, additionalCriteria, clazz);

                if (arguments != null && arguments.length > 0) {
                    if (arguments[0] instanceof Map) {
                        Map argMap = (Map)arguments[0];
                        argMap.put(GrailsHibernateUtil.ARGUMENT_SORT,propertyName);
                        GrailsHibernateUtil.populateArgumentsForCriteria(application, clazz, crit,argMap);
                    }
                    else {
                        crit.addOrder(Order.asc(propertyName));
                    }
                }
                else {
                    crit.addOrder(Order.asc(propertyName));
                }
                crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
                return crit.list();
            }
        });
    }
}
