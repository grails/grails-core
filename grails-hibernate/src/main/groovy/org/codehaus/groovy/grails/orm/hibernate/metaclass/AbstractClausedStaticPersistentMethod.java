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

import grails.orm.RlikeExpression;
import groovy.lang.Closure;
import groovy.lang.MissingMethodException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.TypedValue;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.util.Assert;

/**
 * @author Graeme Rocher
 * @since 31-Aug-2005
 */
public abstract class AbstractClausedStaticPersistentMethod extends AbstractStaticPersistentMethod {

    private static final Log LOG = LogFactory.getLog(AbstractClausedStaticPersistentMethod.class);

    /**
     * @author Graeme Rocher
     */
    protected abstract static class GrailsMethodExpression {
        protected static final String LESS_THAN = "LessThan";
        protected static final String LESS_THAN_OR_EQUAL = "LessThanEquals";
        protected static final String GREATER_THAN = "GreaterThan";
        protected static final String GREATER_THAN_OR_EQUAL = "GreaterThanEquals";
        protected static final String LIKE = "Like";
        protected static final String ILIKE = "Ilike";
        protected static final String RLIKE = "Rlike";
        protected static final String BETWEEN = "Between";
        protected static final String IN_LIST= "InList";
        protected static final String IS_NOT_NULL = "IsNotNull";
        protected static final String IS_NULL = "IsNull";
        protected static final String NOT = "Not";
        protected static final String EQUAL = "Equal";
        protected static final String NOT_EQUAL = "NotEqual";

        protected String propertyName;
        protected Object[] arguments;
        protected int argumentsRequired;
        protected boolean negation;
        protected String type;
        protected Class<?> targetClass;
        private GrailsApplication application;
        private TypeConverter converter = new SimpleTypeConverter();

        /**
         * Used as an indication that an expression will return no results, so stop processing and return nothing.
         */
        static final Criterion FORCE_NO_RESULTS = new Criterion() {
            private static final long serialVersionUID = 1L;
            public TypedValue[] getTypedValues(Criteria c, CriteriaQuery q) { return null; }
            public String toSqlString(Criteria c, CriteriaQuery q) { return null; }
        };

        GrailsMethodExpression(GrailsApplication application, Class<?> targetClass,
                String propertyName, String type, int argumentsRequired, boolean negation) {
            this.application = application;
            this.targetClass = targetClass;
            this.propertyName = propertyName;
            this.type = type;
            this.argumentsRequired = argumentsRequired;
            this.negation = negation;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Object[] getArguments() {
            Object[] copy = new Object[arguments.length];
            System.arraycopy(arguments, 0, copy, 0, arguments.length);
            return copy;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder("[GrailsMethodExpression] ");
            buf.append(propertyName)
               .append(" ")
               .append(type)
               .append(" ");

            for (int i = 0; i < arguments.length; i++) {
                buf.append(arguments[i]);
                if (i != arguments.length) {
                    buf.append(" and ");
                }
            }
            return buf.toString();
        }

        @SuppressWarnings("unchecked")
        void setArguments(Object[] args) throws IllegalArgumentException {
            if (args.length != argumentsRequired) {
                throw new IllegalArgumentException("Method expression '" + type + "' requires " +
                        argumentsRequired + " arguments");
            }

            GrailsDomainClass dc = (GrailsDomainClass)application.getArtefact(
                    DomainClassArtefactHandler.TYPE, targetClass.getName());
            GrailsDomainClassProperty prop = dc.getPropertyByName(propertyName);

            if (prop == null) {
                throw new IllegalArgumentException("Property " + propertyName +
                        " doesn't exist for method expression '"+ type + "'");
            }

            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) continue;
                // convert GStrings to strings
                if (prop.getType() == String.class && (args[i] instanceof CharSequence)) {
                    args[i] = args[i].toString();
                }
                else if (!prop.getType().isAssignableFrom(args[i].getClass()) && !(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(prop.getType(), args[i].getClass()))) {
                    try {
                        if (type.equals(IN_LIST)) {
                            args[i] = converter.convertIfNecessary(args[i], Collection.class);
                        }
                        else {
                            args[i] = converter.convertIfNecessary(args[i], prop.getType());
                        }
                    }
                    catch (TypeMismatchException tme) {
                        // if we cannot perform direct conversion and argument is subclass of Number
                        // we can try to convert it through its String representation
                        if (Number.class.isAssignableFrom(args[i].getClass())) {
                            try {
                                args[i] = converter.convertIfNecessary(args[i].toString(), prop.getType());
                            }
                            catch(TypeMismatchException tme1) {
                                throw new IllegalArgumentException("Cannot convert value " + args[i] + " of property '"+propertyName+"' to required type " + prop.getType() + ": " + tme1.getMessage());
                            }
                        }
                        else {
                            throw new IllegalArgumentException("Cannot convert value " + args[i] + " of property '"+propertyName+"' to required type " + prop.getType());
                        }
                    }
                }
            }

            arguments = args;
        }

        abstract Criterion createCriterion();

        protected Criterion getCriterion() {
            Assert.notNull(arguments, "Parameters array must be set before retrieving Criterion");
            return negation ? Restrictions.not(createCriterion()) : createCriterion();
        }

        protected static GrailsMethodExpression create(final GrailsApplication application,
                Class<?> clazz, String queryParameter) {

            if (queryParameter.endsWith(LESS_THAN_OR_EQUAL)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, LESS_THAN_OR_EQUAL),
                        LESS_THAN_OR_EQUAL,
                        1,
                        isNegation(queryParameter, LESS_THAN_OR_EQUAL)) {
                    @Override
                    Criterion createCriterion() {
                        return Restrictions.le(propertyName, arguments[0]);
                    }
                };
            }

            if (queryParameter.endsWith(LESS_THAN)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, LESS_THAN),
                        LESS_THAN,
                        1, // argument count
                        isNegation(queryParameter, LESS_THAN)) {
                    @Override
                    Criterion createCriterion() {
                        if (arguments[0] == null) return Restrictions.isNull(propertyName);
                        return Restrictions.lt(propertyName, arguments[0]);
                    }
                };
            }

            if (queryParameter.endsWith(GREATER_THAN_OR_EQUAL)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, GREATER_THAN_OR_EQUAL),
                        GREATER_THAN_OR_EQUAL,
                        1,
                        isNegation(queryParameter, GREATER_THAN_OR_EQUAL)) {
                    @Override
                    Criterion createCriterion() {
                        if (arguments[0] == null) return Restrictions.isNull(propertyName);
                        return Restrictions.ge(propertyName, arguments[0]);
                    }
                };
            }

            if (queryParameter.endsWith(GREATER_THAN)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, GREATER_THAN),
                        GREATER_THAN,
                        1,
                        isNegation(queryParameter, GREATER_THAN)) {
                    @Override
                    Criterion createCriterion() {
                        if (arguments[0] == null) return Restrictions.isNull(propertyName);
                        return Restrictions.gt(propertyName, arguments[0]);
                    }
                };
            }

            if (queryParameter.endsWith(LIKE)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, LIKE),
                        LIKE,
                        1,
                        isNegation(queryParameter, LIKE)) {
                    @Override
                    Criterion createCriterion() {
                        if (arguments[0] == null) return Restrictions.isNull(propertyName);
                        return Restrictions.like(propertyName, arguments[0]);
                    }
                };
            }

            if (queryParameter.endsWith(ILIKE)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, ILIKE),
                        ILIKE,
                        1,
                        isNegation(queryParameter, ILIKE)) {
                    @Override
                    Criterion createCriterion() {
                        if (arguments[0] == null) return Restrictions.isNull(propertyName);
                        return Restrictions.ilike(propertyName, arguments[0]);
                    }
                };
            }

            if (queryParameter.endsWith(RLIKE)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, RLIKE),
                        RLIKE,
                        1,
                        isNegation(queryParameter, RLIKE)) {
                    @Override
                    Criterion createCriterion() {
                        if (arguments[0] == null) return Restrictions.isNull(propertyName);
                        return new RlikeExpression(propertyName, arguments[0]);
                    }
                };
            }

            if (queryParameter.endsWith(IS_NOT_NULL)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, IS_NOT_NULL),
                        IS_NOT_NULL,
                        0,
                        isNegation(queryParameter, IS_NOT_NULL)) {
                    @Override
                    Criterion createCriterion() {
                        return Restrictions.isNotNull(propertyName);
                    }
                };
            }

            if (queryParameter.endsWith(IS_NULL)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, IS_NULL),
                        IS_NULL,
                        0,
                        isNegation(queryParameter, IS_NULL)) {
                    @Override
                    Criterion createCriterion() {
                        return Restrictions.isNull(propertyName);
                    }
                };
            }

            if (queryParameter.endsWith(BETWEEN)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, BETWEEN),
                        BETWEEN,
                        2,
                        isNegation(queryParameter, BETWEEN)) {
                    @Override
                    Criterion createCriterion() {
                        return Restrictions.between(propertyName,arguments[0], arguments[1]);
                    }
                };
            }

            if (queryParameter.endsWith(IN_LIST)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, IN_LIST),
                        IN_LIST,
                        1,
                        isNegation(queryParameter, IN_LIST)) {
                    @SuppressWarnings("rawtypes")
                    @Override
                    Criterion createCriterion() {
                        Collection collection = (Collection)arguments[0];
                        if (collection.isEmpty()) {
                            return FORCE_NO_RESULTS;
                        }
                        return Restrictions.in(propertyName, collection);
                    }
                };
            }

            if (queryParameter.endsWith(NOT_EQUAL)) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, NOT_EQUAL),
                        NOT_EQUAL,
                        1,
                        isNegation(queryParameter, NOT_EQUAL)) {
                    @Override
                    Criterion createCriterion() {
                        if (arguments[0] == null) return Restrictions.isNotNull(propertyName);
                        return Restrictions.ne(propertyName,arguments[0]);
                    }
                };
            }

            return new GrailsMethodExpression(
                    application,
                    clazz,
                    calcPropertyName(queryParameter, null),
                    EQUAL,
                    1,
                    isNegation(queryParameter, EQUAL)) {
                @Override
                Criterion createCriterion() {
                    if (arguments[0] == null) return Restrictions.isNull(propertyName);
                    return Restrictions.eq(propertyName,arguments[0]);
                }
            };
        }

        private static boolean isNegation(String queryParameter, String clause) {
            String propName;
            if (clause != null && !clause.equals(EQUAL)) {
                int i = queryParameter.indexOf(clause);
                propName = queryParameter.substring(0,i);
            }
            else {
                propName = queryParameter;
            }
            return propName.endsWith(NOT);
        }

        private static String calcPropertyName(String queryParameter, String clause) {
            String propName;
            if (clause != null && !clause.equals(EQUAL)) {
                int i = queryParameter.indexOf(clause);
                propName = queryParameter.substring(0,i);
            }
            else {
                propName = queryParameter;
            }
            if (propName.endsWith(NOT)) {
                int i = propName.lastIndexOf(NOT);
                propName = propName.substring(0, i);
            }
            return propName.substring(0,1).toLowerCase(Locale.ENGLISH) + propName.substring(1);
        }
    }

    private final String[] operators;
    private final Pattern[] operatorPatterns;
    protected final GrailsApplication application;

    /**
     * Constructor.
     * @param application
     * @param sessionFactory
     * @param classLoader
     * @param pattern
     * @param operators
     */
    public AbstractClausedStaticPersistentMethod(GrailsApplication application, SessionFactory sessionFactory, ClassLoader classLoader, Pattern pattern, String[] operators) {
        super(sessionFactory, classLoader, pattern);
        this.application = application;
        this.operators = operators;
        operatorPatterns = new Pattern[operators.length];
        for (int i = 0; i < operators.length; i++) {
            operatorPatterns[i] = Pattern.compile("(\\w+)("+operators[i]+")(\\p{Upper})(\\w+)");
        }
    }

    /* (non-Javadoc)
     * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractStaticPersistentMethod#doInvokeInternal(java.lang.Class, java.lang.String, java.lang.Object[])
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Object doInvokeInternal(final Class clazz, String methodName,
                                      Closure additionalCriteria, Object[] arguments) {
        List expressions = new ArrayList();
        if (arguments == null) arguments = new Object[0];
        Matcher match = super.getPattern().matcher(methodName);
        // find match
        match.find();

        String[] queryParameters;
        int totalRequiredArguments = 0;
        // get the sequence clauses
        final String querySequence;
        int groupCount = match.groupCount();
        if (groupCount == 6) {
            String booleanProperty = match.group(3);
            if (booleanProperty == null) {
                booleanProperty = match.group(6);
                querySequence = null;
            }
            else {
                querySequence = match.group(5);
            }
            Boolean arg = Boolean.TRUE;
            if (booleanProperty.matches("Not[A-Z].*")) {
                booleanProperty = booleanProperty.substring(3);
                arg = Boolean.FALSE;
            }
            GrailsMethodExpression booleanExpression = GrailsMethodExpression.create(
                    application, clazz, booleanProperty);
            booleanExpression.setArguments(new Object[]{arg});
            expressions.add(booleanExpression);
        }
        else {
            querySequence = match.group(2);
        }
        // if it contains operator and split
        boolean containsOperator = false;
        String operatorInUse = null;
        if (querySequence != null) {
            for (int i = 0; i < operators.length; i++) {
                Matcher currentMatcher = operatorPatterns[i].matcher(querySequence);
                if (currentMatcher.find()) {
                    containsOperator = true;
                    operatorInUse = operators[i];

                    queryParameters = new String[2];
                    queryParameters[0] = currentMatcher.group(1);
                    queryParameters[1] = currentMatcher.group(3) + currentMatcher.group(4);

                    // loop through query parameters and create expressions
                    // calculating the number of arguments required for the expression
                    int argumentCursor = 0;
                    for (String queryParameter : queryParameters) {
                        GrailsMethodExpression currentExpression = GrailsMethodExpression.create(
                                application, clazz, queryParameter);
                        totalRequiredArguments += currentExpression.argumentsRequired;
                        // populate the arguments into the GrailsExpression from the argument list
                        Object[] currentArguments = new Object[currentExpression.argumentsRequired];
                        if ((argumentCursor + currentExpression.argumentsRequired) > arguments.length) {
                            throw new MissingMethodException(methodName, clazz, arguments);
                        }

                        for (int k = 0; k < currentExpression.argumentsRequired; k++, argumentCursor++) {
                            currentArguments[k] = arguments[argumentCursor];
                        }
                        try {
                            currentExpression.setArguments(currentArguments);
                        }
                        catch (IllegalArgumentException iae) {
                            LOG.debug(iae.getMessage(), iae);
                            throw new MissingMethodException(methodName, clazz, arguments);
                        }
                        // add to list of expressions
                        expressions.add(currentExpression);
                    }
                    break;
                }
            }
        }
        // otherwise there is only one expression
        if (!containsOperator && querySequence != null) {
            GrailsMethodExpression solo = GrailsMethodExpression.create(application, clazz,querySequence);

            if (solo.argumentsRequired > arguments.length) {
                throw new MissingMethodException(methodName,clazz,arguments);
            }

            totalRequiredArguments += solo.argumentsRequired;
            Object[] soloArgs = new Object[solo.argumentsRequired];

            System.arraycopy(arguments, 0, soloArgs, 0, solo.argumentsRequired);
            try {
                solo.setArguments(soloArgs);
            }
            catch (IllegalArgumentException iae) {
                LOG.debug(iae.getMessage(), iae);
                throw new MissingMethodException(methodName,clazz,arguments);
            }
            expressions.add(solo);
        }

        // if the total of all the arguments necessary does not equal the number of arguments
        // throw exception
        if (totalRequiredArguments > arguments.length) {
            throw new MissingMethodException(methodName,clazz,arguments);
        }

        // calculate the remaining arguments
        Object[] remainingArguments = new Object[arguments.length - totalRequiredArguments];
        if (remainingArguments.length > 0) {
            for (int i = 0, j = totalRequiredArguments; i < remainingArguments.length; i++,j++) {
                remainingArguments[i] = arguments[j];
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Calculated expressions: " + expressions);
        }

        return doInvokeInternalWithExpressions(clazz, methodName, remainingArguments, expressions, operatorInUse, additionalCriteria);
    }

    @SuppressWarnings("rawtypes")
    protected abstract Object doInvokeInternalWithExpressions(Class clazz, String methodName, Object[] arguments, List expressions, String operatorInUse, Closure additionalCriteria);
}
