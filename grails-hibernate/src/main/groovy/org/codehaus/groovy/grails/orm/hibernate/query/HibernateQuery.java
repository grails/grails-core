/* Copyright (C) 2011 SpringSource
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

package org.codehaus.groovy.grails.orm.hibernate.query;

import grails.orm.HibernateCriteriaBuilder;
import grails.orm.RlikeExpression;

import java.lang.reflect.Field;
import java.util.*;

import org.codehaus.groovy.grails.orm.hibernate.HibernateSession;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.query.AssociationQuery;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.datastore.mapping.query.criteria.FunctionCallingCriterion;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.*;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.BasicType;
import org.hibernate.type.TypeResolver;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.util.ReflectionUtils;

/**
 * Bridges the Query API with the Hibernate Criteria API
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class HibernateQuery extends Query {

    private Criteria criteria;
    private HibernateQuery.HibernateProjectionList hibernateProjectionList = null;
    private String alias;
    private int aliasCount;
    private Map<String, CriteriaAndAlias> createdAssociationPaths = new HashMap<String, CriteriaAndAlias> ();
    private static final String ALIAS = "_alias";
    private static Field opField = ReflectionUtils.findField(SimpleExpression.class, "op");

    public HibernateQuery(Criteria criteria, HibernateSession session, PersistentEntity entity) {
        super(session, entity);
        this.criteria = criteria;
        ReflectionUtils.makeAccessible(opField);
    }

    public HibernateQuery(Criteria subCriteria, HibernateSession session, PersistentEntity associatedEntity, String newAlias) {
        this(subCriteria, session, associatedEntity);
        alias = newAlias;
    }

    @Override
    public Query isEmpty(String property) {
        org.hibernate.criterion.Criterion criterion = Restrictions.isEmpty(calculatePropertyName(property));
        addToCriteria(criterion);
        return this;
    }

    @Override
    public Query isNotEmpty(String property) {
        addToCriteria(Restrictions.isNotEmpty(calculatePropertyName(property)));
        return this;
    }

    @Override
    public Query isNull(String property) {
        addToCriteria(Restrictions.isNull(calculatePropertyName(property)));
        return this;
    }

    @Override
    public Query isNotNull(String property) {
        addToCriteria(Restrictions.isNotNull(calculatePropertyName(property)));
        return this;
    }

    @Override
    public void add(Criterion criterion) {
        if (criterion instanceof FunctionCallingCriterion) {
            org.hibernate.criterion.Criterion sqlRestriction = getRestrictionForFunctionCall((FunctionCallingCriterion) criterion, entity);
            if (sqlRestriction != null) {
                addToCriteria(sqlRestriction);
            }
        }
        else if (criterion instanceof PropertyCriterion) {
            PropertyCriterion pc = (PropertyCriterion) criterion;
            if (pc.getValue() instanceof QueryableCriteria) {
                DetachedCriteria hibernateDetachedCriteria = HibernateCriteriaBuilder.getHibernateDetachedCriteria((QueryableCriteria) pc.getValue());
                pc.setValue(hibernateDetachedCriteria);
            }
        }
        final org.hibernate.criterion.Criterion hibernateCriterion = new HibernateCriterionAdapter(criterion, alias).toHibernateCriterion(this);
        if (hibernateCriterion != null) {
            addToCriteria(hibernateCriterion);
        }
    }

    org.hibernate.criterion.Criterion getRestrictionForFunctionCall(FunctionCallingCriterion criterion, @SuppressWarnings("hiding") PersistentEntity entity) {
        org.hibernate.criterion.Criterion sqlRestriction;HibernateTemplate hibernateSession = (HibernateTemplate)session.getNativeInterface();

        SessionFactory sessionFactory = hibernateSession.getSessionFactory();
        String property = criterion.getProperty();
        Criterion datastoreCriterion = criterion.getPropertyCriterion();
        PersistentProperty pp = entity.getPropertyByName(property);

        if (pp == null) throw new InvalidDataAccessResourceUsageException(
             "Cannot execute function defined in query [" + criterion.getFunctionName() +
             "] on non-existent property [" + property + "] of [" + entity.getJavaClass() + "]");

        String functionName = criterion.getFunctionName();


        SessionFactoryImplementor impl = (SessionFactoryImplementor) sessionFactory;
        Dialect dialect = impl.getDialect();
        SQLFunction sqlFunction = dialect.getFunctions().get(functionName);
        if (sqlFunction != null) {
            TypeResolver typeResolver = impl.getTypeResolver();
            BasicType basic = typeResolver.basic(pp.getType().getName());
            if (basic != null && datastoreCriterion instanceof PropertyCriterion) {

                PropertyCriterion pc = (PropertyCriterion) datastoreCriterion;
                final org.hibernate.criterion.Criterion hibernateCriterion = new HibernateCriterionAdapter(datastoreCriterion, alias).toHibernateCriterion(this);
                if (hibernateCriterion instanceof SimpleExpression) {
                    SimpleExpression expr = (SimpleExpression) hibernateCriterion;
                    Object op = ReflectionUtils.getField(opField, expr);
                    PropertyMapping mapping = (PropertyMapping) impl.getEntityPersister(entity.getJavaClass().getName());
                    String[] columns;
                    if (alias != null)
                        columns = mapping.toColumns(alias, property);
                    else
                        columns = mapping.toColumns(property);
                    String root = sqlFunction.render(basic, Arrays.asList(columns), impl);
                    Object value = pc.getValue();
                    if (value != null) {
                        sqlRestriction = Restrictions.sqlRestriction(root + op + "?", value, typeResolver.basic(value.getClass().getName()));
                    }
                    else {
                        sqlRestriction = Restrictions.sqlRestriction(root + op + "?", value, basic);
                    }
                }
                else {
                    throw new InvalidDataAccessResourceUsageException("Unsupported function ["+functionName+"] defined in query for property ["+property+"] with type ["+pp.getType()+"]");
                }
            }
            else {
                throw new InvalidDataAccessResourceUsageException("Unsupported function ["+functionName+"] defined in query for property ["+property+"] with type ["+pp.getType()+"]");
            }
        }
        else {
            throw new InvalidDataAccessResourceUsageException("Unsupported function defined in query ["+functionName+"]");
        }
        return sqlRestriction;
    }

    @Override
    public Junction disjunction() {
        final org.hibernate.criterion.Disjunction disjunction = Restrictions.disjunction();
        addToCriteria(disjunction);
        return new HibernateJunction(disjunction, alias);
    }

    @Override
    public Junction negation() {
        final org.hibernate.criterion.Disjunction disjunction = Restrictions.disjunction();
        addToCriteria(Restrictions.not(disjunction));
        return new HibernateJunction(disjunction, alias);
    }

    @Override
    public Query eq(String property, Object value) {
        addToCriteria(Restrictions.eq(calculatePropertyName(property), value));
        return this;
    }

    @Override
    public Query idEq(Object value) {
        addToCriteria(Restrictions.idEq(value));
        return this;
    }

    @Override
    public Query gt(String property, Object value) {
        addToCriteria(Restrictions.gt(calculatePropertyName(property), value));
        return this;
    }


    @Override
    public Query and(Criterion a, Criterion b) {
        HibernateCriterionAdapter aa = new HibernateCriterionAdapter(a, alias);
        HibernateCriterionAdapter ab = new HibernateCriterionAdapter(a, alias);
        addToCriteria(Restrictions.and(aa.toHibernateCriterion(this), ab.toHibernateCriterion(this)));
        return this;
    }

    @Override
    public Query or(Criterion a, Criterion b) {
        HibernateCriterionAdapter aa = new HibernateCriterionAdapter(a, alias);
        HibernateCriterionAdapter ab = new HibernateCriterionAdapter(a, alias);
        addToCriteria(Restrictions.or(aa.toHibernateCriterion(this), ab.toHibernateCriterion(this)));
        return this;

    }

    @Override
    public Query allEq(Map<String, Object> values) {
        addToCriteria(Restrictions.allEq(values));
        return this;
    }

    @Override
    public Query ge(String property, Object value) {
        addToCriteria(Restrictions.ge(calculatePropertyName(property), value));
        return this;
    }

    @Override
    public Query le(String property, Object value) {
        addToCriteria(Restrictions.le(calculatePropertyName(property), value));
        return this;
    }

    @Override
    public Query gte(String property, Object value) {
        addToCriteria(Restrictions.ge(calculatePropertyName(property), value));
        return this;
    }

    @Override
    public Query lte(String property, Object value) {
        addToCriteria(Restrictions.le(calculatePropertyName(property), value));
        return this;
    }

    @Override
    public Query lt(String property, Object value) {
        addToCriteria(Restrictions.lt(calculatePropertyName(property), value));
        return this;
    }

    @Override
    public Query in(String property, List values) {
        addToCriteria(Restrictions.in(calculatePropertyName(property), values));
        return this;
    }

    @Override
    public Query between(String property, Object start, Object end) {
        addToCriteria(Restrictions.between(calculatePropertyName(property), start, end));
        return this;
    }

    @Override
    public Query like(String property, String expr) {
        addToCriteria(Restrictions.like(calculatePropertyName(property), calculatePropertyName(expr)));
        return this;
    }

    @Override
    public Query ilike(String property, String expr) {
        addToCriteria(Restrictions.ilike(calculatePropertyName(property), calculatePropertyName(expr)));
        return this;
    }

    @Override
    public Query rlike(String property, String expr) {
        addToCriteria(new RlikeExpression(calculatePropertyName(property), calculatePropertyName(expr)));
        return this;
    }

    @Override
    public AssociationQuery createQuery(String associationName) {
        final PersistentProperty property = entity.getPropertyByName(calculatePropertyName(associationName));
        if (property != null && (property instanceof Association)) {
            @SuppressWarnings("hiding") String alias = generateAlias(associationName);
            CriteriaAndAlias subCriteria = getOrCreateAlias(associationName, alias);

            Association association = (Association) property;
            return new HibernateAssociationQuery(subCriteria.criteria, (HibernateSession) getSession(), association.getAssociatedEntity(), association, alias);
        }
        throw new InvalidDataAccessApiUsageException("Cannot query association [" + calculatePropertyName(associationName) + "] of entity [" + entity + "]. Property is not an association!");
    }

    private CriteriaAndAlias getOrCreateAlias(String associationName, @SuppressWarnings("hiding") String alias) {
        CriteriaAndAlias subCriteria;
        if (createdAssociationPaths.containsKey(associationName)) {
            subCriteria = createdAssociationPaths.get(associationName);
        }
        else {
            Criteria sc = criteria.createAlias(associationName, alias);
            subCriteria = new CriteriaAndAlias(sc, alias);
            createdAssociationPaths.put(associationName,subCriteria);
        }
        return subCriteria;
    }

    @Override
    public ProjectionList projections() {
        if(hibernateProjectionList == null)
            hibernateProjectionList = new HibernateProjectionList();
        return hibernateProjectionList;
    }

    @Override
    public Query max(@SuppressWarnings("hiding") int max) {
        criteria.setMaxResults(max);
        return this;
    }

    @Override
    public Query maxResults(@SuppressWarnings("hiding") int max) {
        criteria.setMaxResults(max);
        return this;
    }

    @Override
    public Query offset(@SuppressWarnings("hiding") int offset) {
        criteria.setFirstResult(offset);
        return this;
    }

    @Override
    public Query firstResult(@SuppressWarnings("hiding") int offset) {
        offset(offset);
        return this;
    }

    @Override
    public Query order(Order order) {
        super.order(order);
        criteria.addOrder(order.getDirection() == Order.Direction.ASC ?
                org.hibernate.criterion.Order.asc(calculatePropertyName(order.getProperty())) :
                org.hibernate.criterion.Order.desc(calculatePropertyName(order.getProperty())));
        return this;
    }

    @Override
    public List list() {
        int projectionLength = 0;
        if (hibernateProjectionList != null) {
            org.hibernate.criterion.ProjectionList projectionList = hibernateProjectionList.getHibernateProjectionList();
            projectionLength = projectionList.getLength();
            this.criteria.setProjection(projectionList);
        }


        if(projectionLength<2)
            criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    @Override
    protected void flushBeforeQuery() {
        // do nothing
    }

    @Override
    public Object singleResult() {
        if (hibernateProjectionList != null) {
            criteria.setProjection(hibernateProjectionList.getHibernateProjectionList());
        }
        criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        return criteria.uniqueResult();
    }

    @SuppressWarnings("hiding")
    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        return list();
    }

    String handleAssociationQuery(Association<?> association, @SuppressWarnings("unused") List<Criterion> criteriaList) {
        String associationName = calculatePropertyName(association.getName());
        String newAlias = generateAlias(associationName);
        CriteriaAndAlias criteriaAndAlias = getOrCreateAlias(associationName, newAlias);

        return criteriaAndAlias.alias;
    }

    private void addToCriteria(org.hibernate.criterion.Criterion criterion) {
        if (criterion != null) {
            criteria.add(criterion);
        }
    }

    private String calculatePropertyName(String property) {
        if (alias != null) {
            return alias + '.' + property;
        }
        return property;
    }

    private String generateAlias(String associationName) {
        return calculatePropertyName(associationName) + calculatePropertyName(ALIAS) + aliasCount++;
    }

    private class HibernateJunction extends Junction {

        private org.hibernate.criterion.Junction hibernateJunction;
        private String alias;

        public HibernateJunction(org.hibernate.criterion.Junction junction, String alias) {
            hibernateJunction = junction;
            this.alias = alias;
        }

        @Override
        public Junction add(Criterion c) {
            if (c != null) {
                if (c instanceof FunctionCallingCriterion) {
                    org.hibernate.criterion.Criterion sqlRestriction = getRestrictionForFunctionCall((FunctionCallingCriterion) c, entity);
                    if (sqlRestriction != null) {
                        hibernateJunction.add(sqlRestriction);
                    }
                }
                else {
                    HibernateCriterionAdapter adapter = new HibernateCriterionAdapter(c, alias);
                    org.hibernate.criterion.Criterion criterion = adapter.toHibernateCriterion(HibernateQuery.this);
                    if (criterion != null) {
                        hibernateJunction.add(criterion);
                    }
                }
            }
            return this;
        }
    }

    private class HibernateProjectionList extends ProjectionList {

        org.hibernate.criterion.ProjectionList projectionList = Projections.projectionList();

        public org.hibernate.criterion.ProjectionList getHibernateProjectionList() {
            return projectionList;
        }

        @Override
        public ProjectionList add(Projection p) {
            projectionList.add(new HibernateProjectionAdapter(p).toHibernateProjection());
            return this;
        }

        @Override
        public org.grails.datastore.mapping.query.api.ProjectionList countDistinct(String property) {
            projectionList.add(Projections.countDistinct(calculatePropertyName(property)));
            return this;
        }

        @Override
        public org.grails.datastore.mapping.query.api.ProjectionList distinct(String property) {
            projectionList.add(Projections.distinct(Projections.property(calculatePropertyName(property))));
            return this;
        }

        @Override
        public org.grails.datastore.mapping.query.api.ProjectionList rowCount() {
            projectionList.add(Projections.rowCount());
            return this;
        }

        @Override
        public ProjectionList id() {
            projectionList.add(Projections.id());
            return this;
        }

        @Override
        public ProjectionList count() {
            projectionList.add(Projections.rowCount());
            return this;
        }

        @Override
        public ProjectionList property(String name) {
            projectionList.add(Projections.property(calculatePropertyName(name)));
            return this;
        }

        @Override
        public ProjectionList sum(String name) {
            projectionList.add(Projections.sum(calculatePropertyName(name)));
            return this;
        }

        @Override
        public ProjectionList min(String name) {
            projectionList.add(Projections.min(calculatePropertyName(name)));
            return this;
        }

        @Override
        public ProjectionList max(String name) {
            projectionList.add(Projections.max(calculatePropertyName(name)));
            return this;
        }

        @Override
        public ProjectionList avg(String name) {
            projectionList.add(Projections.avg(calculatePropertyName(name)));
            return this;
        }

        @Override
        public ProjectionList distinct() {
            criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            return this;
        }
    }

    private class HibernateAssociationQuery extends AssociationQuery {

        private String alias;
        private org.hibernate.criterion.Junction hibernateJunction;
        private Criteria assocationCriteria;

        public HibernateAssociationQuery(Criteria criteria, HibernateSession session, PersistentEntity associatedEntity, Association association, String alias) {
            super(session, associatedEntity, association);
            this.alias = alias;
            assocationCriteria = criteria;
        }

        @Override
        public Query isEmpty(String property) {
            org.hibernate.criterion.Criterion criterion = Restrictions.isEmpty(calculatePropertyName(property));
            addToCriteria(criterion);
            return this;
        }

        private void addToCriteria(org.hibernate.criterion.Criterion criterion) {
           if (hibernateJunction != null) {
               hibernateJunction.add(criterion);
           }
           else {
               assocationCriteria.add(criterion);
           }
        }

        @Override
        public Query isNotEmpty(String property) {
            addToCriteria(Restrictions.isNotEmpty(calculatePropertyName(property)));
            return this;
        }

        @Override
        public Query isNull(String property) {
            addToCriteria(Restrictions.isNull(calculatePropertyName(property)));
            return this;
        }

        @Override
        public Query isNotNull(String property) {
            addToCriteria(Restrictions.isNotNull(calculatePropertyName(property)));
            return this;
        }

        @Override
        public void add(Criterion criterion) {
            final org.hibernate.criterion.Criterion hibernateCriterion = new HibernateCriterionAdapter(criterion, alias).toHibernateCriterion(HibernateQuery.this);
            if (hibernateCriterion != null) {
                addToCriteria(hibernateCriterion);
            }
        }

        @Override
        public Junction disjunction() {
            final org.hibernate.criterion.Disjunction disjunction = Restrictions.disjunction();
            addToCriteria(disjunction);
            return new HibernateJunction(disjunction, alias);
        }

        @Override
        public Junction negation() {
            final org.hibernate.criterion.Disjunction disjunction = Restrictions.disjunction();
            addToCriteria(Restrictions.not(disjunction));
            return new HibernateJunction(disjunction, alias);
        }

        @Override
        public Query eq(String property, Object value) {
            addToCriteria(Restrictions.eq(calculatePropertyName(property), value));
            return this;
        }

        @Override
        public Query idEq(Object value) {
            addToCriteria(Restrictions.idEq(value));
            return this;
        }

        @Override
        public Query gt(String property, Object value) {
            addToCriteria(Restrictions.gt(calculatePropertyName(property), value));
            return this;
        }

        @Override
        public Query and(Criterion a, Criterion b) {
            HibernateCriterionAdapter aa = new HibernateCriterionAdapter(a, alias);
            HibernateCriterionAdapter ab = new HibernateCriterionAdapter(a, alias);
            addToCriteria(Restrictions.and(aa.toHibernateCriterion(HibernateQuery.this), ab.toHibernateCriterion(HibernateQuery.this)));
            return this;
        }

        @Override
        public Query or(Criterion a, Criterion b) {
            HibernateCriterionAdapter aa = new HibernateCriterionAdapter(a, alias);
            HibernateCriterionAdapter ab = new HibernateCriterionAdapter(a, alias);
            addToCriteria(Restrictions.or(aa.toHibernateCriterion(HibernateQuery.this), ab.toHibernateCriterion(HibernateQuery.this)));
            return this;
        }

        @Override
        public Query allEq(Map<String, Object> values) {
            addToCriteria(Restrictions.allEq(values));
            return this;
        }

        @Override
        public Query ge(String property, Object value) {
            addToCriteria(Restrictions.ge(calculatePropertyName(property), value));
            return this;
        }

        @Override
        public Query le(String property, Object value) {
            addToCriteria(Restrictions.le(calculatePropertyName(property), value));
            return this;
        }

        @Override
        public Query gte(String property, Object value) {
            addToCriteria(Restrictions.ge(calculatePropertyName(property), value));
            return this;
        }

        @Override
        public Query lte(String property, Object value) {
            addToCriteria(Restrictions.le(calculatePropertyName(property), value));
            return this;
        }

        @Override
        public Query lt(String property, Object value) {
            addToCriteria(Restrictions.lt(calculatePropertyName(property), value));
            return this;
        }

        @Override
        public Query in(String property, List values) {
            addToCriteria(Restrictions.in(calculatePropertyName(property), values));
            return this;
        }

        @Override
        public Query between(String property, Object start, Object end) {
            addToCriteria(Restrictions.between(calculatePropertyName(property), start, end));
            return this;
        }

        @Override
        public Query like(String property, String expr) {
            addToCriteria(Restrictions.like(calculatePropertyName(property), calculatePropertyName(expr)));
            return this;
        }

        @Override
        public Query ilike(String property, String expr) {
            addToCriteria(Restrictions.ilike(calculatePropertyName(property), calculatePropertyName(expr)));
            return this;
        }

        @Override
        public Query rlike(String property, String expr) {
            addToCriteria(new RlikeExpression(calculatePropertyName(property), calculatePropertyName(expr)));
            return this;
        }
    }

    private class CriteriaAndAlias {
        private Criteria criteria;
        private String alias;

        public CriteriaAndAlias(Criteria subCriteria, String alias) {
            criteria = subCriteria;
            this.alias = alias;
        }
    }
}
