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
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The "findBy*" static persistent method. This method allows querying for
 * instances of grails domain classes based on their properties. This method returns a list of all found results
 *
 * eg.
 * Account.findAllByHolder("Joe Blogs"); // Where class "Account" has a property called "holder"
 * Account.findAllByHolderAndBranch("Joe Blogs", "London"); // Where class "Account" has a properties called "holder" and "branch"
 *
 * @author Graeme Rocher
 */
public class FindAllByPersistentMethod extends AbstractClausedStaticPersistentMethod {

    private static final String OPERATOR_OR = "Or";
    private static final String OPERATOR_AND = "And";
    private static final String METHOD_PATTERN = "(findAllBy)([A-Z]\\w*)";
    private static final String[] OPERATORS = new String[]{ OPERATOR_AND, OPERATOR_OR };

    /**
     * Constructor.
     * @param application
     * @param sessionFactory
     * @param classLoader
     */
    public FindAllByPersistentMethod(GrailsApplication application, SessionFactory sessionFactory, ClassLoader classLoader) {
        super(application, sessionFactory, classLoader, Pattern.compile(METHOD_PATTERN), OPERATORS);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object doInvokeInternalWithExpressions(final Class clazz, String methodName,
            final Object[] arguments, final List expressions, String operatorInUse,
            final Closure additionalCriteria) {

        final String operator = OPERATOR_OR.equals(operatorInUse) ? OPERATOR_OR : OPERATOR_AND;
        return getHibernateTemplate().executeFind(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {

                final Criteria c = getCriteria(application, session, additionalCriteria, clazz);
                Map argsMap = (arguments.length > 0 && (arguments[0] instanceof Map)) ? (Map) arguments[0] : Collections.EMPTY_MAP;
                GrailsHibernateUtil.populateArgumentsForCriteria(application, clazz, c, argsMap);

                if (operator.equals(OPERATOR_OR)) {
                    Disjunction dis = Restrictions.disjunction();
                    if (firstExpressionIsRequiredBoolean()) {
                        GrailsMethodExpression expression = (GrailsMethodExpression) expressions.remove(0);
                        c.add(expression.getCriterion());
                    }
                    for (Object expression : expressions) {
                        GrailsMethodExpression current = (GrailsMethodExpression) expression;
                        if (GrailsMethodExpression.FORCE_NO_RESULTS != current.getCriterion()) {
                            dis.add(current.getCriterion());
                        }
                    }
                    c.add(dis);
                }
                else {
                    for (Object expression : expressions) {
                        GrailsMethodExpression current = (GrailsMethodExpression) expression;
                        if (GrailsMethodExpression.FORCE_NO_RESULTS == current.getCriterion()) {
                            return Collections.emptyList();
                        }
                        c.add(current.getCriterion());
                    }
                }

                c.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
                return c.list();
            }
        });
    }

    /**
     * Indicates if the first expression in the query is a required boolean property and as such should
     * be ANDed to the other expressions, not ORed.
     *
     * @return true if the first expression is a required boolean property, false otherwise
     * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.FindAllByBooleanPropertyPersistentMethod
     */
    protected boolean firstExpressionIsRequiredBoolean() {
        return false;
    }
}
