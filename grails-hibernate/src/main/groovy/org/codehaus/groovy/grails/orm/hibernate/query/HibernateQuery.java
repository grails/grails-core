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

import grails.orm.RlikeExpression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.orm.hibernate.HibernateSession;
import org.grails.datastore.gorm.query.criteria.DetachedAssociationCriteria;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.query.AssociationQuery;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.ProjectionList;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Bridges the Query API with the Hibernate Criteria API
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class HibernateQuery extends Query {


    @SuppressWarnings("hiding") private Criteria criteria;
    private HibernateQuery.HibernateProjectionList hibernateProjectionList = null;

    public HibernateQuery(Criteria criteria, HibernateSession session, PersistentEntity entity) {
        super(session, entity);
        this.criteria = criteria;
    }

    @Override
    public Query isEmpty(String property) {
        criteria.add(Restrictions.isEmpty(property));
        return this;
    }

    @Override
    public Query isNotEmpty(String property) {
        criteria.add(Restrictions.isNotEmpty(property));
        return this;
    }

    @Override
    public Query isNull(String property) {
        criteria.add(Restrictions.isNull(property));
        return this;
    }

    @Override
    public Query isNotNull(String property) {
        criteria.add(Restrictions.isNotNull(property));
        return this;
    }

    @Override
    public ProjectionList projections() {
        hibernateProjectionList = new HibernateProjectionList();
        return hibernateProjectionList;
    }

    @Override
    public void add(Criterion criterion) {
        final org.hibernate.criterion.Criterion hibernateCriterion = new HibernateCriterionAdapter(criterion).toHibernateCriterion();
        if (hibernateCriterion != null) {
            criteria.add(hibernateCriterion);
        }
        else if(criterion instanceof AssociationQuery) {
            AssociationQuery associationQuery = (AssociationQuery) criterion;
            Association<?> association = associationQuery.getAssociation();
            Junction criteriaList = associationQuery.getCriteria();
            handleAssociationQuery(association, criteriaList.getCriteria());
        }
        else if(criterion instanceof DetachedAssociationCriteria) {
            DetachedAssociationCriteria associationCriteria = (DetachedAssociationCriteria)criterion;
            Association<?> association = associationCriteria.getAssociation();
            handleAssociationQuery(association, associationCriteria.getCriteria());
        }
    }

    private void handleAssociationQuery(Association<?> association, List<Criterion> criteriaList) {
        Criteria subCriteria = this.criteria.createCriteria(association.getName());
        HibernateQuery subQuery = new HibernateQuery(subCriteria, (HibernateSession) getSession(), association.getAssociatedEntity());
        for (Criterion c : criteriaList) {
            subQuery.add(c);
        }
    }

    @Override
    public Junction disjunction() {
        final org.hibernate.criterion.Disjunction disjunction = Restrictions.disjunction();
        criteria.add(disjunction);
        return new HibernateJunction(disjunction);
    }

    @Override
    public Junction negation() {
        final org.hibernate.criterion.Disjunction disjunction = Restrictions.disjunction();
        criteria.add(Restrictions.not(disjunction));
        return new HibernateJunction(disjunction);
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
                            org.hibernate.criterion.Order.asc(order.getProperty()) :
                                org.hibernate.criterion.Order.desc(order.getProperty()));
        return this;
    }


    @Override
    public Query eq(String property, Object value) {
        criteria.add(Restrictions.eq(property, value));
        return this;
    }

    @Override
    public Query createQuery(String associationName) {
        final PersistentProperty property = entity.getPropertyByName(associationName);
        if (property != null && (property instanceof Association)) {
            final Criteria subCriteria = criteria.createCriteria(associationName);
            Association association = (Association) property;
            return new HibernateQuery(subCriteria, (HibernateSession) getSession(), association.getAssociatedEntity());
        }
        throw new InvalidDataAccessApiUsageException("Cannot query association ["+associationName+"] of entity ["+entity+"]. Property is not an association!");
    }

    @Override
    public Query idEq(Object value) {
        criteria.add(Restrictions.idEq(value));
        return this;
    }

    @Override
    public Query gt(String property, Object value) {
        criteria.add(Restrictions.gt(property, value));
        return this;
    }

    @Override
    public Query and(Criterion a, Criterion b) {
        HibernateCriterionAdapter aa = new HibernateCriterionAdapter(a);
        HibernateCriterionAdapter ab = new HibernateCriterionAdapter(a);
        criteria.add(Restrictions.and(aa.toHibernateCriterion(), ab.toHibernateCriterion()));
        return this;
    }

    @Override
    public Query or(Criterion a, Criterion b) {
        HibernateCriterionAdapter aa = new HibernateCriterionAdapter(a);
        HibernateCriterionAdapter ab = new HibernateCriterionAdapter(a);
        criteria.add(Restrictions.or(aa.toHibernateCriterion(), ab.toHibernateCriterion()));
        return this;

    }

    @Override
    public Query allEq(Map<String, Object> values) {
        criteria.add(Restrictions.allEq(values));
        return this;
    }

    @Override
    public Query ge(String property, Object value) {
        criteria.add(Restrictions.ge(property,value));
        return this;
    }

    @Override
    public Query le(String property, Object value) {
        criteria.add(Restrictions.le(property, value));
        return this;
    }

    @Override
    public Query gte(String property, Object value) {
        criteria.add(Restrictions.ge(property, value));
        return this;
    }

    @Override
    public Query lte(String property, Object value) {
        criteria.add(Restrictions.le(property, value));
        return this;
    }

    @Override
    public Query lt(String property, Object value) {
        criteria.add(Restrictions.lt(property, value));
        return this;
    }

    @Override
    public Query in(String property, List values) {
        criteria.add(Restrictions.in(property, values));
        return this;
    }

    @Override
    public Query between(String property, Object start, Object end) {
        criteria.add(Restrictions.between(property, start, end));
        return this;
    }

    @Override
    public Query like(String property, String expr) {
        criteria.add(Restrictions.like(property, expr));
        return this;
    }

    @Override
    public Query ilike(String property, String expr) {
        criteria.add(Restrictions.ilike(property, expr));
        return this;
    }

    @Override
    public Query rlike(String property, String expr) {
        criteria.add(new RlikeExpression(property, expr));
        return this;
    }

    @Override
    public List list() {
        return criteria.list();
    }

    @Override
    protected void flushBeforeQuery() {
        // do nothing
    }

    @Override
    public Object singleResult() {
        return criteria.uniqueResult();
    }

    @SuppressWarnings("hiding")
    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        if(hibernateProjectionList != null) {
            this.criteria.setProjection(hibernateProjectionList.getHibernateProjectionList());
        }
        return this.criteria.list();
    }


    private class HibernateJunction extends Junction {

        private org.hibernate.criterion.Junction hibernateJunction;

        public HibernateJunction(org.hibernate.criterion.Junction junction) {
            this.hibernateJunction = junction;
        }

        @Override
        public Junction add(Criterion c) {
            if (c != null) {
                HibernateCriterionAdapter adapter = new HibernateCriterionAdapter(c);
                hibernateJunction.add(adapter.toHibernateCriterion());
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
        public org.grails.datastore.mapping.query.api.ProjectionList countDistinct(String property) {
            projectionList.add(Projections.countDistinct(property));
            return this;
        }

        @Override
        public org.grails.datastore.mapping.query.api.ProjectionList distinct(String property) {
            projectionList.add(Projections.distinct(Projections.property(property)));
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
            projectionList.add(Projections.property(name));
            return this;
        }

        @Override
        public ProjectionList sum(String name) {
            projectionList.add(Projections.sum(name));
            return this;
        }

        @Override
        public ProjectionList min(String name) {
            projectionList.add(Projections.min(name));
            return this;
        }

        @Override
        public ProjectionList max(String name) {
            projectionList.add(Projections.max(name));
            return this;
        }

        @Override
        public ProjectionList avg(String name) {
            projectionList.add(Projections.avg(name));
            return this;
        }

        @Override
        public ProjectionList distinct() {
            criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
            return this;
        }
    }
}
