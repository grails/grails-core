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
package grails.orm;

import grails.gorm.DetachedCriteria;
import grails.util.CollectionUtils;
import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MissingMethodException;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateTemplate;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.codehaus.groovy.grails.orm.hibernate.query.HibernateCriterionAdapter;
import org.codehaus.groovy.grails.orm.hibernate.query.HibernateProjectionAdapter;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.TypeHelper;
import org.hibernate.criterion.AggregateProjection;
import org.hibernate.criterion.CountProjection;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.IdentifierProjection;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.PropertyProjection;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.springframework.beans.BeanUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>Wraps the Hibernate Criteria API in a builder. The builder can be retrieved through the "createCriteria()" dynamic static
 * method of Grails domain classes (Example in Groovy):
 *
 * <pre>
 *         def c = Account.createCriteria()
 *         def results = c {
 *             projections {
 *                 groupProperty("branch")
 *             }
 *             like("holderFirstName", "Fred%")
 *             and {
 *                 between("balance", 500, 1000)
 *                 eq("branch", "London")
 *             }
 *             maxResults(10)
 *             order("holderLastName", "desc")
 *         }
 * </pre>
 *
 * <p>The builder can also be instantiated standalone with a SessionFactory and persistent Class instance:
 *
 * <pre>
 *      new HibernateCriteriaBuilder(clazz, sessionFactory).list {
 *         eq("firstName", "Fred")
 *      }
 * </pre>
 *
 * @author Graeme Rocher
 */
public class HibernateCriteriaBuilder extends GroovyObjectSupport implements org.grails.datastore.mapping.query.api.Criteria, org.grails.datastore.mapping.query.api.ProjectionList {

    public static final String AND = "and"; // builder
    public static final String IS_NULL = "isNull"; // builder
    public static final String IS_NOT_NULL = "isNotNull"; // builder
    public static final String NOT = "not";// builder
    public static final String OR = "or"; // builder
    public static final String ID_EQUALS = "idEq"; // builder
    public static final String IS_EMPTY = "isEmpty"; //builder
    public static final String IS_NOT_EMPTY = "isNotEmpty"; //builder
    public static final String RLIKE = "rlike";//method
    public static final String BETWEEN = "between";//method
    public static final String EQUALS = "eq";//method
    public static final String EQUALS_PROPERTY = "eqProperty";//method
    public static final String GREATER_THAN = "gt";//method
    public static final String GREATER_THAN_PROPERTY = "gtProperty";//method
    public static final String GREATER_THAN_OR_EQUAL = "ge";//method
    public static final String GREATER_THAN_OR_EQUAL_PROPERTY = "geProperty";//method
    public static final String ILIKE = "ilike";//method
    public static final String IN = "in";//method
    public static final String LESS_THAN = "lt"; //method
    public static final String LESS_THAN_PROPERTY = "ltProperty";//method
    public static final String LESS_THAN_OR_EQUAL = "le";//method
    public static final String LESS_THAN_OR_EQUAL_PROPERTY = "leProperty";//method
    public static final String LIKE = "like";//method
    public static final String NOT_EQUAL = "ne";//method
    public static final String NOT_EQUAL_PROPERTY = "neProperty";//method
    public static final String SIZE_EQUALS = "sizeEq"; //method
    public static final String ORDER_DESCENDING = "desc";
    public static final String ORDER_ASCENDING = "asc";
    private static final String ROOT_DO_CALL = "doCall";
    private static final String ROOT_CALL = "call";
    private static final String LIST_CALL = "list";
    private static final String LIST_DISTINCT_CALL = "listDistinct";
    private static final String COUNT_CALL = "count";
    private static final String GET_CALL = "get";
    private static final String SCROLL_CALL = "scroll";
    private static final String SET_RESULT_TRANSFORMER_CALL = "setResultTransformer";
    private static final String PROJECTIONS = "projections";

    private SessionFactory sessionFactory;
    private Session hibernateSession;
    private Class<?> targetClass;
    private Criteria criteria;
    private MetaClass criteriaMetaClass;

    private boolean uniqueResult = false;
    private List<LogicalExpression> logicalExpressionStack = new ArrayList<LogicalExpression>();
    private List<String> associationStack = new ArrayList<String>();
    private boolean participate;
    private boolean scroll;
    private boolean count;
    private ProjectionList projectionList = Projections.projectionList();
    private List<String> aliasStack = new ArrayList<String>();
    private List<Criteria> aliasInstanceStack = new ArrayList<Criteria>();
    private Map<String, String> aliasMap = new HashMap<String, String>();
    private static final String ALIAS = "_alias";
    private ResultTransformer resultTransformer;
    private int aliasCount;

    private boolean paginationEnabledList = false;
    private List<Order> orderEntries;
    private GrailsApplication grailsApplication;

    @SuppressWarnings("rawtypes")
    public HibernateCriteriaBuilder(Class targetClass, SessionFactory sessionFactory) {
        this.targetClass = targetClass;
        this.sessionFactory = sessionFactory;
    }

    @SuppressWarnings("rawtypes")
    public HibernateCriteriaBuilder(Class targetClass, SessionFactory sessionFactory, boolean uniqueResult) {
        this.targetClass = targetClass;
        this.sessionFactory = sessionFactory;
        this.uniqueResult = uniqueResult;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }

    /**
     * Returns the criteria instance
     * @return The criteria instance
     */
    public Criteria getInstance() {
        return criteria;
    }

    /**
     * Set whether a unique result should be returned
     * @param uniqueResult True if a unique result should be returned
     */
    public void setUniqueResult(boolean uniqueResult) {
        this.uniqueResult = uniqueResult;
    }

    /**
     * A projection that selects a property name
     * @param propertyName The name of the property
     */
    public org.grails.datastore.mapping.query.api.ProjectionList property(String propertyName) {
        return property(propertyName, null);
    }

    /**
     * A projection that selects a property name
     * @param propertyName The name of the property
     * @param alias The alias to use
     */
    public org.grails.datastore.mapping.query.api.ProjectionList property(String propertyName, String alias) {
        final PropertyProjection propertyProjection = Projections.property(calculatePropertyName(propertyName));
        addProjectionToList(propertyProjection, alias);
        return this;
    }

    /**
     * Adds a projection to the projectList for the given alias
     *
     * @param propertyProjection The projection
     * @param alias The alias
     */
    protected void addProjectionToList(Projection propertyProjection, String alias) {
        if (alias != null) {
            projectionList.add(propertyProjection,alias);
        }
        else {
            projectionList.add(propertyProjection);
        }
    }

    /**
     * Adds a sql projection to the criteria
     *
     * @param sql SQL projecting a single value
     * @param columnAlias column alias for the projected value
     * @param type the type of the projected value
     */
    protected void sqlProjection(String sql, String columnAlias, Type type) {
        sqlProjection(sql, CollectionUtils.newList(columnAlias), CollectionUtils.newList(type));
    }

    /**
     * Adds a sql projection to the criteria
     *
     * @param sql SQL projecting
     * @param columnAliases List of column aliases for the projected values
     * @param types List of types for the projected values
     */
    protected void sqlProjection(String sql, List<String> columnAliases, List<Type> types) {
        projectionList.add(Projections.sqlProjection(sql, columnAliases.toArray(new String[columnAliases.size()]), types.toArray(new Type[types.size()])));
    }

    /**
     * Adds a sql projection to the criteria
     *
     * @param sql SQL projecting
     * @param groupBy group by clause
     * @param columnAliases List of column aliases for the projected values
     * @param types List of types for the projected values
     */
    protected void sqlGroupProjection(String sql, String groupBy, List<String> columnAliases, List<Type> types) {
        projectionList.add(Projections.sqlGroupProjection(sql, groupBy, columnAliases.toArray(new String[columnAliases.size()]), types.toArray(new Type[types.size()])));
    }

    /**
     * A projection that selects a distince property name
     * @param propertyName The property name
     */
    public org.grails.datastore.mapping.query.api.ProjectionList distinct(String propertyName) {
        distinct(propertyName, null);
        return this;
    }

    /**
     * A projection that selects a distince property name
     * @param propertyName The property name
     * @param alias The alias to use
     */
    public org.grails.datastore.mapping.query.api.ProjectionList distinct(String propertyName, String alias) {
        final Projection proj = Projections.distinct(Projections.property(calculatePropertyName(propertyName)));
        addProjectionToList(proj,alias);
        return this;
    }

    /**
     * A distinct projection that takes a list
     *
     * @param propertyNames The list of distince property names
     */
    @SuppressWarnings("rawtypes")
    public org.grails.datastore.mapping.query.api.ProjectionList distinct(Collection propertyNames) {
        return distinct(propertyNames, null);
    }

    /**
     * A distinct projection that takes a list
     *
     * @param propertyNames The list of distince property names
     * @param alias The alias to use
     */
    @SuppressWarnings("rawtypes")
    public org.grails.datastore.mapping.query.api.ProjectionList distinct(Collection propertyNames, String alias) {
        ProjectionList list = Projections.projectionList();
        for (Object o : propertyNames) {
            list.add(Projections.property(calculatePropertyName(o.toString())));
        }
        final Projection proj = Projections.distinct(list);
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * Adds a projection that allows the criteria to return the property average value
     *
     * @param propertyName The name of the property
     */
    public org.grails.datastore.mapping.query.api.ProjectionList avg(String propertyName) {
        return avg(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to return the property average value
     *
     * @param propertyName The name of the property
     * @param alias The alias to use
     */
    public org.grails.datastore.mapping.query.api.ProjectionList avg(String propertyName, String alias) {
        final AggregateProjection aggregateProjection = Projections.avg(calculatePropertyName(propertyName));
        addProjectionToList(aggregateProjection, alias);
        return this;
    }

    /**
     * Use a join query
     *
     * @param associationPath The path of the association
     */
    public void join(String associationPath) {
        criteria.setFetchMode(calculatePropertyName(associationPath), FetchMode.JOIN);
    }

    /**
     * Whether a pessimistic lock should be obtained.
     *
     * @param shouldLock True if it should
     */
    public void lock(boolean shouldLock) {
        String lastAlias = getLastAlias();

        if (shouldLock) {
            if (lastAlias != null) {
                criteria.setLockMode(lastAlias, LockMode.PESSIMISTIC_WRITE);
            }
            else {
                criteria.setLockMode(LockMode.PESSIMISTIC_WRITE);
            }
        }
        else {
            if (lastAlias != null) {
                criteria.setLockMode(lastAlias, LockMode.NONE);
            }
            else {
                criteria.setLockMode(LockMode.NONE);
            }
        }
    }

    /**
     * Use a select query
     *
     * @param associationPath The path of the association
     */
    public void select(String associationPath) {
        criteria.setFetchMode(calculatePropertyName(associationPath), FetchMode.SELECT);
    }

    /**
     * Whether to use the query cache
     * @param shouldCache True if the query should be cached
     */
    public void cache(boolean shouldCache) {
        criteria.setCacheable(shouldCache);
    }

    /**
     * Calculates the property name including any alias paths
     *
     * @param propertyName The property name
     * @return The calculated property name
     */
    private String calculatePropertyName(String propertyName) {
        String lastAlias = getLastAlias();
        if (lastAlias != null) {
            return lastAlias +'.'+propertyName;
        }

        return propertyName;
    }

    private String getLastAlias() {
        if (aliasStack.size() > 0) {
            return aliasStack.get(aliasStack.size() - 1).toString();
        }
        return null;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    /**
     * Calculates the property value, converting GStrings if necessary
     *
     * @param propertyValue The property value
     * @return The calculated property value
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object calculatePropertyValue(Object propertyValue) {
        if (propertyValue instanceof CharSequence) {
            return propertyValue.toString();
        }
        if (propertyValue instanceof QueryableCriteria) {
            propertyValue = getHibernateDetachedCriteria((QueryableCriteria<?>)propertyValue);
        }
        else if (propertyValue instanceof Closure) {
            propertyValue = getHibernateDetachedCriteria(
                  new DetachedCriteria(targetClass).build((Closure<?>)propertyValue));
        }
        return propertyValue;
    }

    public static org.hibernate.criterion.DetachedCriteria getHibernateDetachedCriteria(QueryableCriteria<?> queryableCriteria) {
        org.hibernate.criterion.DetachedCriteria detachedCriteria = org.hibernate.criterion.DetachedCriteria.forClass(
             queryableCriteria.getPersistentEntity().getJavaClass());
        populateHibernateDetachedCriteria(detachedCriteria, queryableCriteria);
        return detachedCriteria;
    }

    private static void populateHibernateDetachedCriteria(org.hibernate.criterion.DetachedCriteria detachedCriteria, QueryableCriteria<?> queryableCriteria) {
        List<Query.Criterion> criteriaList = queryableCriteria.getCriteria();
        for (Query.Criterion criterion : criteriaList) {
            Criterion hibernateCriterion = new HibernateCriterionAdapter(criterion).toHibernateCriterion(null);
            if (hibernateCriterion != null) {
                detachedCriteria.add(hibernateCriterion);
            }
        }

        List<Query.Projection> projections = queryableCriteria.getProjections();
        ProjectionList projectionList = Projections.projectionList();
        for (Query.Projection projection : projections) {
            Projection hibernateProjection = new HibernateProjectionAdapter(projection).toHibernateProjection();
            if (hibernateProjection != null) {
                 projectionList.add(hibernateProjection);
            }
        }
        detachedCriteria.setProjection(projectionList);
    }

    /**
     * Adds a projection that allows the criteria to return the property count
     *
     * @param propertyName The name of the property
     */
    public void count(String propertyName) {
        count(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to return the property count
     *
     * @param propertyName The name of the property
     * @param alias The alias to use
     */
    public void count(String propertyName, String alias) {
        final CountProjection proj = Projections.count(calculatePropertyName(propertyName));
        addProjectionToList(proj, alias);
    }

    public org.grails.datastore.mapping.query.api.ProjectionList id() {
        final IdentifierProjection proj = Projections.id();
        addProjectionToList(proj, null);
        return this;
    }

    public org.grails.datastore.mapping.query.api.ProjectionList count() {
        return rowCount();
    }

    /**
     * Adds a projection that allows the criteria to return the distinct property count
     *
     * @param propertyName The name of the property
     */
    public org.grails.datastore.mapping.query.api.ProjectionList countDistinct(String propertyName) {
        return countDistinct(propertyName, null);
    }

    public org.grails.datastore.mapping.query.api.ProjectionList distinct() {
        return this;
    }

    /**
     * Adds a projection that allows the criteria to return the distinct property count
     *
     * @param propertyName The name of the property
     * @param alias The alias to use
     */
    public org.grails.datastore.mapping.query.api.ProjectionList countDistinct(String propertyName, String alias) {
        final CountProjection proj = Projections.countDistinct(calculatePropertyName(propertyName));
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * Adds a projection that allows the criteria's result to be grouped by a property
     *
     * @param propertyName The name of the property
     */
    public void groupProperty(String propertyName) {
        groupProperty(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria's result to be grouped by a property
     *
     * @param propertyName The name of the property
     * @param alias The alias to use
     */
    public void groupProperty(String propertyName, String alias) {
        final PropertyProjection proj = Projections.groupProperty(calculatePropertyName(propertyName));
        addProjectionToList(proj, alias);
    }

    /**
     * Adds a projection that allows the criteria to retrieve a  maximum property value
     *
     * @param propertyName The name of the property
     */
    public org.grails.datastore.mapping.query.api.ProjectionList max(String propertyName) {
        return max(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to retrieve a  maximum property value
     *
     * @param propertyName The name of the property
     * @param alias The alias to use
     */
    public org.grails.datastore.mapping.query.api.ProjectionList max(String propertyName, String alias) {
        final AggregateProjection proj = Projections.max(calculatePropertyName(propertyName));
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * Adds a projection that allows the criteria to retrieve a  minimum property value
     *
     * @param propertyName The name of the property
     */
    public org.grails.datastore.mapping.query.api.ProjectionList min(String propertyName) {
        return min(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to retrieve a  minimum property value
     *
     * @param alias The alias to use
     */
    public org.grails.datastore.mapping.query.api.ProjectionList min(String propertyName, String alias) {
        final AggregateProjection aggregateProjection = Projections.min(calculatePropertyName(propertyName));
        addProjectionToList(aggregateProjection, alias);
        return this;
    }

    /**
     * Adds a projection that allows the criteria to return the row count
     *
     */
    public org.grails.datastore.mapping.query.api.ProjectionList rowCount() {
        return rowCount(null);
    }

    /**
     * Adds a projection that allows the criteria to return the row count
     *
     * @param alias The alias to use
     */
    public org.grails.datastore.mapping.query.api.ProjectionList rowCount(String alias) {
        final Projection proj = Projections.rowCount();
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * Adds a projection that allows the criteria to retrieve the sum of the results of a property
     *
     * @param propertyName The name of the property
     */
    public org.grails.datastore.mapping.query.api.ProjectionList sum(String propertyName) {
        return sum(propertyName, null);
    }

    /**
     * Adds a projection that allows the criteria to retrieve the sum of the results of a property
     *
     * @param propertyName The name of the property
     * @param alias The alias to use
     */
    public org.grails.datastore.mapping.query.api.ProjectionList sum(String propertyName, String alias) {
        final AggregateProjection proj = Projections.sum(calculatePropertyName(propertyName));
        addProjectionToList(proj, alias);
        return this;
    }

    /**
     * Sets the fetch mode of an associated path
     *
     * @param associationPath The name of the associated path
     * @param fetchMode The fetch mode to set
     */
    public void fetchMode(String associationPath, FetchMode fetchMode) {
        if (criteria != null) {
            criteria.setFetchMode(associationPath, fetchMode);
        }
    }

    /**
     * Sets the resultTransformer.
     * @param transformer The result transformer to use.
     */
    public void resultTransformer(ResultTransformer transformer) {
        if (criteria == null) {
            throwRuntimeException(new IllegalArgumentException("Call to [resultTransformer] not supported here"));
        }
        resultTransformer = transformer;
    }

    /**
     * Join an association, assigning an alias to the joined association.
     * <p/>
     * Functionally equivalent to createAlias(String, String, int) using
     * CriteriaSpecificationINNER_JOIN for the joinType.
     *
     * @param associationPath A dot-seperated property path
     * @param alias The alias to assign to the joined association (for later reference).
     *
     * @return this (for method chaining)
     * #see {@link #createAlias(String, String, int)}
     * @throws HibernateException Indicates a problem creating the sub criteria
     */
    public Criteria createAlias(String associationPath, String alias) {
        return criteria.createAlias(associationPath, alias);
    }

    /**
     * Join an association using the specified join-type, assigning an alias
     * to the joined association.
     * <p/>
     * The joinType is expected to be one of CriteriaSpecification.INNER_JOIN (the default),
     * CriteriaSpecificationFULL_JOIN, or CriteriaSpecificationLEFT_JOIN.
     *
     * @param associationPath A dot-seperated property path
     * @param alias The alias to assign to the joined association (for later reference).
     * @param joinType The type of join to use.
     *
     * @return this (for method chaining)
     * @see #createAlias(String, String)
     * @throws HibernateException Indicates a problem creating the sub criteria
     */
    public Criteria createAlias(String associationPath, String alias, int joinType) {
        return criteria.createAlias(associationPath, alias, joinType);
    }

    /**
     * Creates a Criterion that compares to class properties for equality
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria eqProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [eqProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(Restrictions.eqProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a Criterion that compares to class properties for !equality
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria neProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [neProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(Restrictions.neProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a Criterion that tests if the first property is greater than the second property
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria gtProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [gtProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(Restrictions.gtProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a Criterion that tests if the first property is greater than or equal to the second property
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria geProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [geProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(Restrictions.geProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a Criterion that tests if the first property is less than the second property
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria ltProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [ltProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(Restrictions.ltProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a Criterion that tests if the first property is less than or equal to the second property
     * @param propertyName The first property name
     * @param otherPropertyName The second property name
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria leProperty(String propertyName, String otherPropertyName) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [leProperty] with propertyName [" +
                    propertyName + "] and other property name [" + otherPropertyName + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        otherPropertyName = calculatePropertyName(otherPropertyName);
        addToCriteria(Restrictions.leProperty(propertyName, otherPropertyName));
        return this;
    }

    /**
     * Creates a subquery criterion that ensures the given property is equal to all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public org.grails.datastore.mapping.query.api.Criteria eqAll(String propertyName, Closure<?> propertyValue) {
        return eqAll(propertyName, new DetachedCriteria(targetClass).build(propertyValue));
    }

    /**
     * Creates a subquery criterion that ensures the given property is greater than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public org.grails.datastore.mapping.query.api.Criteria gtAll(String propertyName, Closure<?> propertyValue) {
        return gtAll(propertyName, new DetachedCriteria(targetClass).build(propertyValue));
    }

    /**
     * Creates a subquery criterion that ensures the given property is less than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public org.grails.datastore.mapping.query.api.Criteria ltAll(String propertyName, Closure<?> propertyValue) {
        return ltAll(propertyName, new DetachedCriteria(targetClass).build(propertyValue));
    }

    /**
     * Creates a subquery criterion that ensures the given property is greater than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public org.grails.datastore.mapping.query.api.Criteria geAll(String propertyName, Closure<?> propertyValue) {
        return geAll(propertyName, new DetachedCriteria(targetClass).build(propertyValue));
    }

    /**
     * Creates a subquery criterion that ensures the given property is less than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public org.grails.datastore.mapping.query.api.Criteria leAll(String propertyName, Closure<?> propertyValue) {
        return leAll(propertyName, new DetachedCriteria(targetClass).build(propertyValue));
    }

    /**
     * Creates a subquery criterion that ensures the given property is equal to all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria eqAll(String propertyName,
             @SuppressWarnings("rawtypes") QueryableCriteria propertyValue) {
        addToCriteria(Property.forName(propertyName).eqAll(getHibernateDetachedCriteria(propertyValue)));
        return this;
    }

    /**
     * Creates a subquery criterion that ensures the given property is greater than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria gtAll(String propertyName,
             @SuppressWarnings("rawtypes") QueryableCriteria propertyValue) {
        addToCriteria(Property.forName(propertyName).gtAll(getHibernateDetachedCriteria(propertyValue)));
        return this;
    }

    /**
     * Creates a subquery criterion that ensures the given property is less than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria ltAll(String propertyName,
             @SuppressWarnings("rawtypes") QueryableCriteria propertyValue) {
        addToCriteria(Property.forName(propertyName).ltAll(getHibernateDetachedCriteria(propertyValue)));
        return this;

    }

    /**
     * Creates a subquery criterion that ensures the given property is greater than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria geAll(String propertyName,
             @SuppressWarnings("rawtypes") QueryableCriteria propertyValue) {
        addToCriteria(Property.forName(propertyName).geAll(getHibernateDetachedCriteria(propertyValue)));
        return this;

    }

    /**
     * Creates a subquery criterion that ensures the given property is less than all the given returned values
     *
     * @param propertyName  The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria leAll(String propertyName,
             @SuppressWarnings("rawtypes") QueryableCriteria propertyValue) {
        addToCriteria(Property.forName(propertyName).leAll(getHibernateDetachedCriteria(propertyValue)));
        return this;

    }

    /**
     * Creates a "greater than" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria gt(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [gt] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);

        Criterion gt;
        if (propertyValue instanceof org.hibernate.criterion.DetachedCriteria) {
            gt = Property.forName(propertyName).gt((org.hibernate.criterion.DetachedCriteria)propertyValue);
        }
        else {
            gt = Restrictions.gt(propertyName, propertyValue);
        }
        addToCriteria(gt);
        return this;
    }

    public org.grails.datastore.mapping.query.api.Criteria lte(String s, Object o) {
        return le(s,o);
    }

    /**
     * Creates a "greater than or equal to" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria ge(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [ge] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);

        Criterion ge;
        if (propertyValue instanceof org.hibernate.criterion.DetachedCriteria) {
            ge = Property.forName(propertyName).ge((org.hibernate.criterion.DetachedCriteria) propertyValue);
        }
        else {
            ge = Restrictions.ge(propertyName, propertyValue);
        }
        addToCriteria(ge);
        return this;
    }

    /**
     * Creates a "less than" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria lt(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [lt] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        Criterion lt;
        if (propertyValue instanceof org.hibernate.criterion.DetachedCriteria) {
            lt = Property.forName(propertyName).lt((org.hibernate.criterion.DetachedCriteria) propertyValue);
        }
        else {
            lt = Restrictions.lt(propertyName, propertyValue);
        }
        addToCriteria(lt);
        return this;
    }

    /**
     * Creates a "less than or equal to" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria le(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [le] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        Criterion le;
        if (propertyValue instanceof org.hibernate.criterion.DetachedCriteria) {
            le = Property.forName(propertyName).le((org.hibernate.criterion.DetachedCriteria) propertyValue);
        }
        else {
            le = Restrictions.le(propertyName, propertyValue);
        }
        addToCriteria(le);
        return this;
    }

    public org.grails.datastore.mapping.query.api.Criteria idEquals(Object o) {
        return idEq(o);
    }

    public org.grails.datastore.mapping.query.api.Criteria isEmpty(String property) {
        String propertyName = calculatePropertyName(property);
        addToCriteria(Restrictions.isEmpty(propertyName));
        return this;
    }

    public org.grails.datastore.mapping.query.api.Criteria isNotEmpty(String property) {
        String propertyName = calculatePropertyName(property);
        addToCriteria(Restrictions.isNotEmpty(propertyName));
        return this;
    }

    public org.grails.datastore.mapping.query.api.Criteria isNull(String property) {
        String propertyName = calculatePropertyName(property);
        addToCriteria(Restrictions.isNull(propertyName));
        return this;
    }

    public org.grails.datastore.mapping.query.api.Criteria isNotNull(String property) {
        String propertyName = calculatePropertyName(property);
        addToCriteria(Restrictions.isNotNull(propertyName));
        return this;
    }

    /**
     * Creates an "equals" Criterion based on the specified property name and value. Case-sensitive.
     * @param propertyName The property name
     * @param propertyValue The property value
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria eq(String propertyName, Object propertyValue) {
        return eq(propertyName, propertyValue, Collections.emptyMap());
    }

    public org.grails.datastore.mapping.query.api.Criteria idEq(Object o) {
        return eq("id", o);
    }

    /**
     * Groovy moves the map to the first parameter if using the idiomatic form, e.g.
     * <code>eq 'firstName', 'Fred', ignoreCase: true</code>.
     * @param params optional map with customization parameters; currently only 'ignoreCase' is supported.
     * @param propertyName
     * @param propertyValue
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public org.grails.datastore.mapping.query.api.Criteria eq(Map params, String propertyName, Object propertyValue) {
        return eq(propertyName, propertyValue, params);
    }

    /**
     * Creates an "equals" Criterion based on the specified property name and value.
     * Supports case-insensitive search if the <code>params</code> map contains <code>true</code>
     * under the 'ignoreCase' key.
     * @param propertyName The property name
     * @param propertyValue The property value
     * @param params optional map with customization parameters; currently only 'ignoreCase' is supported.
     *
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public org.grails.datastore.mapping.query.api.Criteria eq(String propertyName, Object propertyValue, Map params) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [eq] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        Criterion eq;
        if (propertyValue instanceof org.hibernate.criterion.DetachedCriteria) {
            eq = Property.forName(propertyName).eq((org.hibernate.criterion.DetachedCriteria) propertyValue);
        }
        else {
            eq =  Restrictions.eq(propertyName, propertyValue);
        }
        if (params != null && (eq instanceof SimpleExpression)) {
            Object ignoreCase = params.get("ignoreCase");
            if (ignoreCase instanceof Boolean && (Boolean)ignoreCase) {
                eq = ((SimpleExpression)eq).ignoreCase();
            }
        }
        addToCriteria(eq);
        return this;
    }

    /**
     * Applies a sql restriction to the results to allow something like:
      <pre>
       def results = Person.withCriteria {
           sqlRestriction "char_length(first_name) <= 4"
       }
      </pre>
     *
     * @param sqlRestriction the sql restriction
     * @return a Criteria instance
     */
    public org.grails.datastore.mapping.query.api.Criteria sqlRestriction(String sqlRestriction) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sqlRestriction] with value [" +
                    sqlRestriction + "] not allowed here."));
        }
        return sqlRestriction(sqlRestriction, Collections.EMPTY_LIST);
    }

    /**
     * Applies a sql restriction to the results to allow something like:
      <pre>
       def results = Person.withCriteria {
           sqlRestriction "char_length(first_name) < ? AND char_length(first_name) > ?", [4, 9]
       }
      </pre>
     *
     * @param sqlRestriction the sql restriction
     * @param values jdbc parameters
     * @return a Criteria instance
     */
    public org.grails.datastore.mapping.query.api.Criteria sqlRestriction(String sqlRestriction, List<?> values) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sqlRestriction] with value [" +
                    sqlRestriction + "] not allowed here."));
        }
        final int numberOfParameters = values.size();

        final Type[] typesArray = new Type[numberOfParameters];
        final Object[] valuesArray = new Object[numberOfParameters];

        if (numberOfParameters > 0) {
            final TypeHelper typeHelper = sessionFactory.getTypeHelper();
            for (int i = 0; i < typesArray.length; i++) {
                final Object value = values.get(i);
                typesArray[i] =  typeHelper.basic(value.getClass());
                valuesArray[i] = value;
            }
        }
        addToCriteria(Restrictions.sqlRestriction(sqlRestriction, valuesArray, typesArray));
        return this;
    }

    /**
     * Creates a Criterion with from the specified property name and "like" expression
     * @param propertyName The property name
     * @param propertyValue The like value
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria like(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [like] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        addToCriteria(Restrictions.like(propertyName, propertyValue));
        return this;
    }

    /**
     * Creates a Criterion with from the specified property name and "rlike" (a regular expression version of "like") expression
     * @param propertyName The property name
     * @param propertyValue The ilike value
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria rlike(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [rlike] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        addToCriteria(new RlikeExpression(propertyName, propertyValue));
        return this;
    }

    /**
     * Creates a Criterion with from the specified property name and "ilike" (a case sensitive version of "like") expression
     * @param propertyName The property name
     * @param propertyValue The ilike value
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria ilike(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [ilike] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        addToCriteria(Restrictions.ilike(propertyName, propertyValue));
        return this;
    }

    /**
     * Applys a "in" contrain on the specified property
     * @param propertyName The property name
     * @param values A collection of values
     *
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public org.grails.datastore.mapping.query.api.Criteria in(String propertyName, Collection values) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [in] with propertyName [" +
                    propertyName + "] and values [" + values + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(Restrictions.in(propertyName, values == null ? Collections.EMPTY_LIST : values));
        return this;
    }

    /**
     * Delegates to in as in is a Groovy keyword
     */
    @SuppressWarnings("rawtypes")
    public org.grails.datastore.mapping.query.api.Criteria inList(String propertyName, Collection values) {
        return in(propertyName, values);
    }

    /**
     * Delegates to in as in is a Groovy keyword
     */
    public org.grails.datastore.mapping.query.api.Criteria inList(String propertyName, Object[] values) {
        return in(propertyName, values);
    }

    /**
     * Applys a "in" contrain on the specified property
     * @param propertyName The property name
     * @param values A collection of values
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria in(String propertyName, Object[] values) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [in] with propertyName [" +
                    propertyName + "] and values [" + values + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(Restrictions.in(propertyName, values));
        return this;
    }

    /**
     * Orders by the specified property name (defaults to ascending)
     *
     * @param propertyName The property name to order by
     * @return A Order instance
     */
    public org.grails.datastore.mapping.query.api.Criteria order(String propertyName) {
        if (criteria == null) {
            throwRuntimeException(new IllegalArgumentException("Call to [order] with propertyName [" +
                    propertyName + "]not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        Order o = Order.asc(propertyName);
        if (paginationEnabledList) {
            orderEntries.add(o);
        }
        else {
            criteria.addOrder(o);
        }
        return this;
    }

    /**
     * Orders by the specified property name (defaults to ascending)
     *
     * @param o The property name to order by
     * @return A Order instance
     */
    public org.grails.datastore.mapping.query.api.Criteria order(Order o) {
        if (criteria == null) {
            throwRuntimeException(new IllegalArgumentException("Call to [order] not allowed here."));
        }
        if (paginationEnabledList) {
            orderEntries.add(o);
        }
        else {
            criteria.addOrder(o);
        }
        return this;
    }

    /**
     * Orders by the specified property name and direction
     *
     * @param propertyName The property name to order by
     * @param direction Either "asc" for ascending or "desc" for descending
     *
     * @return A Order instance
     */
    public org.grails.datastore.mapping.query.api.Criteria order(String propertyName, String direction) {
        if (criteria == null) {
            throwRuntimeException(new IllegalArgumentException("Call to [order] with propertyName [" +
                    propertyName + "]not allowed here."));
        }
        propertyName = calculatePropertyName(propertyName);
        Order o;
        if (direction.equals(ORDER_DESCENDING)) {
            o = Order.desc(propertyName);
        }
        else {
            o = Order.asc(propertyName);
        }
        if (paginationEnabledList) {
            orderEntries.add(o);
        }
        else {
            criteria.addOrder(o);
        }
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property by size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria sizeEq(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeEq] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(Restrictions.sizeEq(propertyName, size));
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property to be greater than the given size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria sizeGt(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeGt] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(Restrictions.sizeGt(propertyName, size));
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property to be greater than or equal to the given size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria sizeGe(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeGe] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(Restrictions.sizeGe(propertyName, size));
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property to be less than or equal to the given size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria sizeLe(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeLe] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(Restrictions.sizeLe(propertyName, size));
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property to be less than to the given size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria sizeLt(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeLt] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(Restrictions.sizeLt(propertyName, size));
        return this;
    }

    /**
     * Creates a Criterion that contrains a collection property to be not equal to the given size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria sizeNe(String propertyName, int size) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [sizeNe] with propertyName [" +
                    propertyName + "] and size [" + size + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(Restrictions.sizeNe(propertyName, size));
        return this;
    }

    /**
     * Creates a "not equal" Criterion based on the specified property name and value
     * @param propertyName The property name
     * @param propertyValue The property value
     * @return The criterion object
     */
    public org.grails.datastore.mapping.query.api.Criteria ne(String propertyName, Object propertyValue) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [ne] with propertyName [" +
                    propertyName + "] and value [" + propertyValue + "] not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        propertyValue = calculatePropertyValue(propertyValue);
        addToCriteria(Restrictions.ne(propertyName, propertyValue));
        return this;
    }

    public org.grails.datastore.mapping.query.api.Criteria notEqual(String propertyName, Object propertyValue) {
        return ne(propertyName, propertyValue);
    }

    /**
     * Creates a "between" Criterion based on the property name and specified lo and hi values
     * @param propertyName The property name
     * @param lo The low value
     * @param hi The high value
     * @return A Criterion instance
     */
    public org.grails.datastore.mapping.query.api.Criteria between(String propertyName, Object lo, Object hi) {
        if (!validateSimpleExpression()) {
            throwRuntimeException(new IllegalArgumentException("Call to [between] with propertyName [" +
                    propertyName + "]  not allowed here."));
        }

        propertyName = calculatePropertyName(propertyName);
        addToCriteria(Restrictions.between(propertyName, lo, hi));
        return this;
    }

    public org.grails.datastore.mapping.query.api.Criteria gte(String s, Object o) {
        return ge(s, o);
    }

    private boolean validateSimpleExpression() {
        return criteria != null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object invokeMethod(String name, Object obj) {
        Object[] args = obj.getClass().isArray() ? (Object[])obj : new Object[]{obj};

        if (paginationEnabledList && SET_RESULT_TRANSFORMER_CALL.equals(name) && args.length == 1 &&
                args[0] instanceof ResultTransformer) {
            resultTransformer = (ResultTransformer) args[0];
            return null;
        }

        if (isCriteriaConstructionMethod(name, args)) {
            if (criteria != null) {
                throwRuntimeException(new IllegalArgumentException("call to [" + name + "] not supported here"));
            }

            if (name.equals(GET_CALL)) {
                uniqueResult = true;
            }
            else if (name.equals(SCROLL_CALL)) {
                scroll = true;
            }
            else if (name.equals(COUNT_CALL)) {
                count = true;
            }
            else if (name.equals(LIST_DISTINCT_CALL)) {
                resultTransformer = CriteriaSpecification.DISTINCT_ROOT_ENTITY;
            }

            createCriteriaInstance();

            // Check for pagination params
            if (name.equals(LIST_CALL) && args.length == 2) {
                paginationEnabledList = true;
                orderEntries = new ArrayList<Order>();
                invokeClosureNode(args[1]);
            }
            else {
                invokeClosureNode(args[0]);
            }

            if (resultTransformer != null) {
                criteria.setResultTransformer(resultTransformer);
            }
            Object result;
            if (!uniqueResult) {
                if (scroll) {
                    result = criteria.scroll();
                }
                else if (count) {
                    criteria.setProjection(Projections.rowCount());
                    result = criteria.uniqueResult();
                }
                else if (paginationEnabledList) {
                    // Calculate how many results there are in total. This has been
                    // moved to before the 'list()' invocation to avoid any "ORDER
                    // BY" clause added by 'populateArgumentsForCriteria()', otherwise
                    // an exception is thrown for non-string sort fields (GRAILS-2690).
                    criteria.setFirstResult(0);
                    criteria.setMaxResults(Integer.MAX_VALUE);

                    // Restore the previous projection, add settings for the pagination parameters,
                    // and then execute the query.
                    if (projectionList != null && projectionList.getLength() > 0) {
                        criteria.setProjection(projectionList);
                    } else {
                        criteria.setProjection(null);
                    }
                    for (Order orderEntry : orderEntries) {
                        criteria.addOrder(orderEntry);
                    }
                    if (resultTransformer == null) {
                        criteria.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);
                    }
                    else if (paginationEnabledList) {
                        // relevant to GRAILS-5692
                        criteria.setResultTransformer(resultTransformer);
                    }
                    // GRAILS-7324 look if we already have association to sort by
                    Map argMap = (Map)args[0];
                    final String sort = (String) argMap.get(GrailsHibernateUtil.ARGUMENT_SORT);
                    if (sort != null) {
                        boolean ignoreCase = true;
                        Object caseArg = argMap.get(GrailsHibernateUtil.ARGUMENT_IGNORE_CASE);
                        if (caseArg instanceof Boolean) {
                            ignoreCase = (Boolean) caseArg;
                        }
                        final String orderParam = (String) argMap.get(GrailsHibernateUtil.ARGUMENT_ORDER);
                        final String order = GrailsHibernateUtil.ORDER_DESC.equalsIgnoreCase(orderParam) ?
                                GrailsHibernateUtil.ORDER_DESC : GrailsHibernateUtil.ORDER_ASC;
                        int lastPropertyPos = sort.lastIndexOf('.');
                        String associationForOrdering = lastPropertyPos >= 0 ? sort.substring(0, lastPropertyPos) : null;
                        if (associationForOrdering != null && aliasMap.containsKey(associationForOrdering)) {
                            addOrder(criteria, aliasMap.get(associationForOrdering) + "." + sort.substring(lastPropertyPos + 1),
                                    order, ignoreCase);
                            // remove sort from arguments map to exclude from default processing.
                            @SuppressWarnings("unchecked") Map argMap2 = new HashMap(argMap);
                            argMap2.remove(GrailsHibernateUtil.ARGUMENT_SORT);
                            argMap = argMap2;
                        }
                    }
                    GrailsHibernateUtil.populateArgumentsForCriteria(grailsApplication, targetClass, criteria, argMap);
                    GrailsHibernateTemplate ght = new GrailsHibernateTemplate(sessionFactory, grailsApplication);
                    PagedResultList pagedRes = new PagedResultList(ght, criteria);
                    result = pagedRes;
                }
                else {
                    result = criteria.list();
                }
            }
            else {
                result = GrailsHibernateUtil.unwrapIfProxy(criteria.uniqueResult());
            }
            if (!participate) {
                hibernateSession.close();
            }
            return result;
        }

        if (criteria == null) createCriteriaInstance();

        MetaMethod metaMethod = getMetaClass().getMetaMethod(name, args);
        if (metaMethod != null) {
            return metaMethod.invoke(this, args);
        }

        metaMethod = criteriaMetaClass.getMetaMethod(name, args);
        if (metaMethod != null) {
            return metaMethod.invoke(criteria, args);
        }
        metaMethod = criteriaMetaClass.getMetaMethod(GrailsClassUtils.getSetterName(name), args);
        if (metaMethod != null) {
            return metaMethod.invoke(criteria, args);
        }

        if (isAssociationQueryMethod(args) || isAssociationQueryWithJoinSpecificationMethod(args)) {
            final boolean hasMoreThanOneArg = args.length > 1;
            Object callable = hasMoreThanOneArg ? args[1] : args[0];
            int joinType = hasMoreThanOneArg ? (Integer)args[0] : CriteriaSpecification.INNER_JOIN;

            if (name.equals(AND) || name.equals(OR) || name.equals(NOT)) {
                if (criteria == null) {
                    throwRuntimeException(new IllegalArgumentException("call to [" + name + "] not supported here"));
                }

                logicalExpressionStack.add(new LogicalExpression(name));
                invokeClosureNode(callable);

                LogicalExpression logicalExpression = logicalExpressionStack.remove(logicalExpressionStack.size()-1);
                addToCriteria(logicalExpression.toCriterion());

                return name;
            }

            if (name.equals(PROJECTIONS) && args.length == 1 && (args[0] instanceof Closure)) {
                if (criteria == null) {
                    throwRuntimeException(new IllegalArgumentException("call to [" + name + "] not supported here"));
                }

                projectionList = Projections.projectionList();
                invokeClosureNode(callable);

                if (projectionList != null && projectionList.getLength() > 0) {
                    criteria.setProjection(projectionList);
                }

                return name;
            }

            final PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(targetClass, name);
            if (pd != null && pd.getReadMethod() != null) {
                ClassMetadata meta = sessionFactory.getClassMetadata(targetClass);
                Type type = meta.getPropertyType(name);
                if (type.isAssociationType()) {
                    String otherSideEntityName =
                        ((AssociationType) type).getAssociatedEntityName((SessionFactoryImplementor) sessionFactory);
                    Class oldTargetClass = targetClass;
                    targetClass = sessionFactory.getClassMetadata(otherSideEntityName).getMappedClass(EntityMode.POJO);
                    if (targetClass.equals(oldTargetClass) && !hasMoreThanOneArg) {
                        joinType = CriteriaSpecification.LEFT_JOIN; // default to left join if joining on the same table
                    }
                    associationStack.add(name);
                    final String associationPath = getAssociationPath();
                    createAliasIfNeccessary(name, associationPath,joinType);
                    // the criteria within an association node are grouped with an implicit AND
                    logicalExpressionStack.add(new LogicalExpression(AND));
                    invokeClosureNode(callable);
                    aliasStack.remove(aliasStack.size() - 1);
                    if (!aliasInstanceStack.isEmpty()) {
                        aliasInstanceStack.remove(aliasInstanceStack.size() - 1);
                    }
                    LogicalExpression logicalExpression = logicalExpressionStack.remove(logicalExpressionStack.size()-1);
                    if (!logicalExpression.args.isEmpty()) {
                        addToCriteria(logicalExpression.toCriterion());
                    }
                    associationStack.remove(associationStack.size()-1);
                    targetClass = oldTargetClass;

                    return name;
                }
                if (type instanceof EmbeddedComponentType) {
                    associationStack.add(name);
                    logicalExpressionStack.add(new LogicalExpression(AND));
                    Class oldTargetClass = targetClass;
                    targetClass = pd.getPropertyType();
                    invokeClosureNode(callable);
                    targetClass = oldTargetClass;
                    LogicalExpression logicalExpression = logicalExpressionStack.remove(logicalExpressionStack.size()-1);
                    if (!logicalExpression.args.isEmpty()) {
                        addToCriteria(logicalExpression.toCriterion());
                    }
                    associationStack.remove(associationStack.size()-1);
                    return name;
                }
            }
        }
        else if (args.length == 1 && args[0] != null) {
            if (criteria == null) {
                throwRuntimeException(new IllegalArgumentException("call to [" + name + "] not supported here"));
            }

            Object value = args[0];
            Criterion c = null;
            if (name.equals(ID_EQUALS)) {
                return eq("id", value);
            }

            if (name.equals(IS_NULL) ||
                    name.equals(IS_NOT_NULL) ||
                    name.equals(IS_EMPTY) ||
                    name.equals(IS_NOT_EMPTY)) {
                if (!(value instanceof String)) {
                    throwRuntimeException(new IllegalArgumentException("call to [" + name + "] with value [" +
                            value + "] requires a String value."));
                }
                String propertyName = calculatePropertyName((String)value);
                if (name.equals(IS_NULL)) {
                    c = Restrictions.isNull(propertyName);
                }
                else if (name.equals(IS_NOT_NULL)) {
                    c = Restrictions.isNotNull(propertyName);
                }
                else if (name.equals(IS_EMPTY)) {
                    c = Restrictions.isEmpty(propertyName);
                }
                else if (name.equals(IS_NOT_EMPTY)) {
                    c = Restrictions.isNotEmpty(propertyName);
                }
            }

            if (c != null) {
                return addToCriteria(c);
            }
        }

        throw new MissingMethodException(name, getClass(), args);
    }

    private boolean isAssociationQueryMethod(Object[] args) {
        return args.length == 1 && args[0] instanceof Closure;
    }

    private boolean isAssociationQueryWithJoinSpecificationMethod(Object[] args) {
        return args.length == 2 && (args[0] instanceof Number) && (args[1] instanceof Closure);
    }

    @SuppressWarnings("unused")
    private void addToCurrentOrAliasedCriteria(Criterion criterion) {
        if (!aliasInstanceStack.isEmpty()) {
            Criteria c = aliasInstanceStack.get(aliasInstanceStack.size()-1);
            c.add(criterion);
        }
        else {
            criteria.add(criterion);
        }
    }

    private void createAliasIfNeccessary(String associationName, String associationPath, int joinType) {
        String newAlias;
        if (aliasMap.containsKey(associationPath)) {
            newAlias = aliasMap.get(associationPath);
        }
        else {
            aliasCount++;
            newAlias = associationName + ALIAS + aliasCount;
            aliasMap.put(associationPath, newAlias);
            aliasInstanceStack.add(criteria.createAlias(associationPath, newAlias,
                    joinType));
        }
        aliasStack.add(newAlias);
    }

    private String getAssociationPath() {
        StringBuilder fullPath = new StringBuilder();
        for (Object anAssociationStack : associationStack) {
            String propertyName = (String) anAssociationStack;
            if (fullPath.length() > 0) fullPath.append(".");
            fullPath.append(propertyName);
        }
        return fullPath.toString();
    }

    private boolean isCriteriaConstructionMethod(String name, Object[] args) {
        return (name.equals(LIST_CALL) && args.length == 2 && args[0] instanceof Map && args[1] instanceof Closure) ||
                  (name.equals(ROOT_CALL) ||
                    name.equals(ROOT_DO_CALL) ||
                    name.equals(LIST_CALL) ||
                    name.equals(LIST_DISTINCT_CALL) ||
                    name.equals(GET_CALL) ||
                    name.equals(COUNT_CALL) ||
                    name.equals(SCROLL_CALL) && args.length == 1 && args[0] instanceof Closure);
    }

    public Criteria buildCriteria(Closure<?> criteriaClosure) {
        createCriteriaInstance();
        criteriaClosure.setDelegate(this);
        criteriaClosure.call();
        return criteria;
    }

    private void createCriteriaInstance() {
        if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
            participate = true;
            hibernateSession = ((SessionHolder)TransactionSynchronizationManager.getResource(sessionFactory)).getSession();
        }
        else {
            hibernateSession = sessionFactory.openSession();
        }

        criteria = hibernateSession.createCriteria(targetClass);
        GrailsHibernateUtil.cacheCriteriaByMapping(grailsApplication, targetClass, criteria);
        criteriaMetaClass = GroovySystem.getMetaClassRegistry().getMetaClass(criteria.getClass());
    }

    private void invokeClosureNode(Object args) {
        Closure<?> callable = (Closure<?>)args;
        callable.setDelegate(this);
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.call();
    }

    /**
     * Throws a runtime exception where necessary to ensure the session gets closed
     */
    private void throwRuntimeException(RuntimeException t) {
        closeSessionFollowingException();
        throw t;
    }

    private void closeSessionFollowingException() {
        if (hibernateSession != null && hibernateSession.isOpen() && !participate) {
            hibernateSession.close();
        }
        criteria = null;
    }

    /**
     * adds and returns the given criterion to the currently active criteria set.
     * this might be either the root criteria or a currently open
     * LogicalExpression.
     */
    private Criterion addToCriteria(Criterion c) {
        if (!logicalExpressionStack.isEmpty()) {
            logicalExpressionStack.get(logicalExpressionStack.size() - 1).args.add(c);
        }
        else {
            criteria.add(c);
        }
        return c;
    }

    /**
     * instances of this class are pushed onto the logicalExpressionStack
     * to represent all the unfinished "and", "or", and "not" expressions.
     */
    private class LogicalExpression {
        final Object name;
        final List<Criterion> args = new ArrayList<Criterion>();

        LogicalExpression(Object name) {
            this.name = name;
        }

        Criterion toCriterion() {
            if (name.equals(NOT)) {
                switch (args.size()) {
                    case 0:
                        throwRuntimeException(new IllegalArgumentException("Logical expression [not] must contain at least 1 expression"));
                        return null;

                    case 1:
                        return Restrictions.not(args.get(0));

                    default:
                        // treat multiple sub-criteria as an implicit "OR"
                        return Restrictions.not(buildJunction(Restrictions.disjunction(), args));
                }
            }

            if (name.equals(AND)) {
                return buildJunction(Restrictions.conjunction(), args);
            }

            if (name.equals(OR)) {
                return buildJunction(Restrictions.disjunction(), args);
            }

            throwRuntimeException(new IllegalStateException("Logical expression [" + name + "] not handled!"));
            return null;
        }

        // add the Criterion objects in the given list to the given junction.
        Junction buildJunction(Junction junction, List<Criterion> criterions) {
            for (Criterion c : criterions) {
                junction.add(c);
            }

            return junction;
        }
    }

    /**
     * Add order directly to criteria.
     */
    private static void addOrder(Criteria c, String sort, String order, boolean ignoreCase) {
        if (GrailsHibernateUtil.ORDER_DESC.equals(order)) {
            c.addOrder( ignoreCase ? Order.desc(sort).ignoreCase() : Order.desc(sort));
        }
        else {
            c.addOrder( ignoreCase ? Order.asc(sort).ignoreCase() : Order.asc(sort) );
        }
    }

    /*
     * Define constants which may be used inside of criteria queries
     * to refer to standard Hibernate Type instances.
     */
    public static final Type BOOLEAN = StandardBasicTypes.BOOLEAN;
    public static final Type YES_NO = StandardBasicTypes.YES_NO;
    public static final Type BYTE = StandardBasicTypes.BYTE;
    public static final Type CHARACTER = StandardBasicTypes.CHARACTER;
    public static final Type SHORT = StandardBasicTypes.SHORT;
    public static final Type INTEGER = StandardBasicTypes.INTEGER;
    public static final Type LONG = StandardBasicTypes.LONG;
    public static final Type FLOAT = StandardBasicTypes.FLOAT;
    public static final Type DOUBLE = StandardBasicTypes.DOUBLE;
    public static final Type BIG_DECIMAL = StandardBasicTypes.BIG_DECIMAL;
    public static final Type BIG_INTEGER = StandardBasicTypes.BIG_INTEGER;
    public static final Type STRING = StandardBasicTypes.STRING;
    public static final Type NUMERIC_BOOLEAN = StandardBasicTypes.NUMERIC_BOOLEAN;
    public static final Type TRUE_FALSE = StandardBasicTypes.TRUE_FALSE;
    public static final Type URL = StandardBasicTypes.URL;
    public static final Type TIME = StandardBasicTypes.TIME;
    public static final Type DATE = StandardBasicTypes.DATE;
    public static final Type TIMESTAMP = StandardBasicTypes.TIMESTAMP;
    public static final Type CALENDAR = StandardBasicTypes.CALENDAR;
    public static final Type CALENDAR_DATE = StandardBasicTypes.CALENDAR_DATE;
    public static final Type CLASS = StandardBasicTypes.CLASS;
    public static final Type LOCALE = StandardBasicTypes.LOCALE;
    public static final Type CURRENCY = StandardBasicTypes.CURRENCY;
    public static final Type TIMEZONE = StandardBasicTypes.TIMEZONE;
    public static final Type UUID_BINARY = StandardBasicTypes.UUID_BINARY;
    public static final Type UUID_CHAR = StandardBasicTypes.UUID_CHAR;
    public static final Type BINARY = StandardBasicTypes.BINARY;
    public static final Type WRAPPER_BINARY = StandardBasicTypes.WRAPPER_BINARY;
    public static final Type IMAGE = StandardBasicTypes.IMAGE;
    public static final Type BLOB = StandardBasicTypes.BLOB;
    public static final Type MATERIALIZED_BLOB = StandardBasicTypes.MATERIALIZED_BLOB;
    public static final Type WRAPPER_MATERIALIZED_BLOB = StandardBasicTypes.WRAPPER_MATERIALIZED_BLOB;
    public static final Type CHAR_ARRAY = StandardBasicTypes.CHAR_ARRAY;
    public static final Type CHARACTER_ARRAY = StandardBasicTypes.CHARACTER_ARRAY;
    public static final Type TEXT = StandardBasicTypes.TEXT;
    public static final Type CLOB = StandardBasicTypes.CLOB;
    public static final Type MATERIALIZED_CLOB = StandardBasicTypes.MATERIALIZED_CLOB;
    public static final Type WRAPPER_CHARACTERS_CLOB = StandardBasicTypes.WRAPPER_CHARACTERS_CLOB;
    public static final Type CHARACTERS_CLOB = StandardBasicTypes.CHARACTERS_CLOB;
    public static final Type SERIALIZABLE = StandardBasicTypes.SERIALIZABLE;
}
