package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import groovy.lang.Closure;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;

public abstract class AbstractFindByPersistentMethod extends AbstractClausedStaticPersistentMethod {
    public static final String OPERATOR_OR = "Or";
    public static final String OPERATOR_AND = "And";
    public static final String[] OPERATORS = new String[]{ OPERATOR_AND, OPERATOR_OR };

    public AbstractFindByPersistentMethod(GrailsApplication application,
            SessionFactory sessionFactory, ClassLoader classLoader,
            Pattern pattern, String[] operators) {
        super(application, sessionFactory, classLoader, pattern, operators);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object doInvokeInternalWithExpressions(final Class clazz, String methodName, final Object[] arguments, final List expressions, String operatorInUse, final Closure additionalCriteria) {
        final String operator = OPERATOR_OR.equals(operatorInUse) ? OPERATOR_OR : OPERATOR_AND;
        return getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {

                Criteria crit = getCriteria(application, session, additionalCriteria, clazz);
                if (arguments.length > 0) {
                    if (arguments[0] instanceof Map<?, ?>) {
                        Map<?, ?> argMap = (Map<?, ?>)arguments[0];
                        GrailsHibernateUtil.populateArgumentsForCriteria(application, clazz, crit,argMap);
                        if (!argMap.containsKey(GrailsHibernateUtil.ARGUMENT_FETCH)) {
                            crit.setMaxResults(1);
                        }
                    }
                }

                if (operator.equals(OPERATOR_OR)) {
                    if (firstExpressionIsRequiredBoolean()) {
                        GrailsMethodExpression expression = (GrailsMethodExpression) expressions.remove(0);
                        crit.add(expression.getCriterion());
                    }
                    Disjunction dis = Restrictions.disjunction();
                    for (Object expression : expressions) {
                        GrailsMethodExpression current = (GrailsMethodExpression) expression;
                        dis.add(current.getCriterion());
                    }
                    crit.add(dis);
                }
                else {
                    for (Object expression : expressions) {
                        GrailsMethodExpression current = (GrailsMethodExpression) expression;
                        crit.add(current.getCriterion());
                    }
                }

                final List<?> list = crit.list();
                if (!list.isEmpty()) {
                    return GrailsHibernateUtil.unwrapIfProxy(list.get(0));
                }
                return null;
            }
        });
    }

    /**
     * Indicates if the first expression in the query is a required boolean property and as such should
     * be ANDed to the other expressions, not ORed.
     *
     * @return true if the first expression is a required boolean property, false otherwise
     * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.FindByBooleanPropertyPersistentMethod
     */
    protected boolean firstExpressionIsRequiredBoolean() {
        return false;
    }
}
