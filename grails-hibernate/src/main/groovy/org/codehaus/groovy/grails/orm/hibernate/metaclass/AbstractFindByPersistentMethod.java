package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import grails.gorm.DetachedCriteria;
import groovy.lang.Closure;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.*;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.impl.CriteriaImpl;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public abstract class AbstractFindByPersistentMethod extends AbstractClausedStaticPersistentMethod {
    public static final String OPERATOR_OR = "Or";
    public static final String OPERATOR_AND = "And";
    public static final String[] OPERATORS = new String[]{ OPERATOR_AND, OPERATOR_OR };
    private HibernateDatastore datastore;
    private static final Map<String, Boolean> useLimitCache = new ConcurrentHashMap<String, Boolean>();

    public AbstractFindByPersistentMethod(HibernateDatastore datastore, GrailsApplication application,
                                          SessionFactory sessionFactory, ClassLoader classLoader,
                                          Pattern pattern, String[] operators) {
        super(application, sessionFactory, classLoader, pattern, operators);
        this.datastore = datastore;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Object doInvokeInternalWithExpressions(final Class clazz, String methodName, final Object[] arguments, final List expressions, String operatorInUse, final DetachedCriteria detachedCriteria, final Closure additionalCriteria) {
        final String operator = OPERATOR_OR.equals(operatorInUse) ? OPERATOR_OR : OPERATOR_AND;
        return getHibernateTemplate().execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                Criteria crit = buildCriteria(session, detachedCriteria, additionalCriteria, clazz, arguments, operator, expressions);

                boolean useLimit = establishWhetherToUseLimit(clazz);
                return getResult(crit, useLimit);
            }
        });
    }

    private boolean establishWhetherToUseLimit(Class<?> clazz) {
        boolean useLimit = true;
        GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE, clazz.getName());
        if (domainClass != null) {
            Boolean aBoolean = useLimitCache.get(domainClass.getName());
            if (aBoolean != null) {
                useLimit = aBoolean;
            }
            else {

                for (GrailsDomainClassProperty property : domainClass.getPersistentProperties()) {
                    if ((property.isOneToMany()||property.isManyToMany()) && property.getFetchMode() == GrailsDomainClassProperty.FETCH_EAGER) {
                        useLimit = false;
                        useLimitCache.put(domainClass.getName(), useLimit);
                        break;
                    }
                }
            }
        }
        return useLimit;
    }

    protected Object getResult(Criteria crit) {
        CriteriaImpl impl = (CriteriaImpl) crit;
        String entityOrClassName = impl.getEntityOrClassName();
        GrailsClass domainClass = application.getArtefact(DomainClassArtefactHandler.TYPE, entityOrClassName);
        boolean useLimit = establishWhetherToUseLimit(domainClass.getClazz());

        return getResult(crit, useLimit);
    }

    protected Object getResult(Criteria crit, boolean useLimit) {
        if (useLimit) {
            final List<?> list = crit.list();
            if (!list.isEmpty()) {
                return GrailsHibernateUtil.unwrapIfProxy(list.get(0));
            }
        }
        else {
            try {
                return crit.uniqueResult();
            } catch (NonUniqueResultException e) {
                return null;
            }
        }
        return null;
    }

    protected Criteria buildCriteria(Session session, DetachedCriteria<?> detachedCriteria,
            Closure<?> additionalCriteria, Class<?> clazz, Object[] arguments,
            String operator, List<?> expressions) {
        Criteria crit = getCriteria(datastore, application, session, detachedCriteria, additionalCriteria, clazz);

        boolean useLimit = establishWhetherToUseLimit(clazz);

        if (arguments.length > 0) {
            if (arguments[0] instanceof Map<?, ?>) {
                Map<?, ?> argMap = (Map<?, ?>)arguments[0];
                GrailsHibernateUtil.populateArgumentsForCriteria(application, clazz, crit, argMap);
                if (!argMap.containsKey(GrailsHibernateUtil.ARGUMENT_FETCH)) {
                    if (useLimit) {
                        crit.setMaxResults(1);
                    }
                }
            }
            else {
                if (useLimit) {
                    crit.setMaxResults(1);
                }
            }
        }
        else {
            if (useLimit) {
                crit.setMaxResults(1);
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
        return crit;
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
