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
import org.codehaus.groovy.grails.orm.hibernate.HibernateSession;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.query.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridges the Query API with the Hibernate Criteria API
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class HibernateQuery extends Query {

    private static final Map<Class<?>, CriterionAdaptor> criterionAdaptors = new HashMap<Class<?>, CriterionAdaptor>();

    static {
        criterionAdaptors.put(IdEquals.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                IdEquals eq = (IdEquals) criterion;
                return Restrictions.idEq(eq.getValue());
            }
        });
        criterionAdaptors.put(IsNull.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                IsNull eq = (IsNull) criterion;
                return Restrictions.isNull(eq.getProperty());
            }
        });
        criterionAdaptors.put(IsNotNull.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                IsNotNull eq = (IsNotNull) criterion;
                return Restrictions.isNotNull(eq.getProperty());
            }
        });
        criterionAdaptors.put(IsEmpty.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                IsEmpty eq = (IsEmpty) criterion;
                return Restrictions.isEmpty(eq.getProperty());
            }
        });
        criterionAdaptors.put(IsNotEmpty.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                IsNotEmpty eq = (IsNotEmpty) criterion;
                return Restrictions.isNotEmpty(eq.getProperty());
            }
        });
        criterionAdaptors.put(Equals.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                Equals eq = (Equals) criterion;
                return Restrictions.eq(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(Like.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                Like eq = (Like) criterion;
                return Restrictions.like(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(RLike.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                RLike eq = (RLike) criterion;
                return new RlikeExpression(eq.getProperty(), eq.getPattern());
            }
        });
        criterionAdaptors.put(NotEquals.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                NotEquals eq = (NotEquals) criterion;
                return Restrictions.ne(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(GreaterThan.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                GreaterThan eq = (GreaterThan) criterion;
                return Restrictions.gt(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(GreaterThanEquals.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                GreaterThanEquals eq = (GreaterThanEquals) criterion;
                return Restrictions.ge(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(LessThan.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                LessThan eq = (LessThan) criterion;
                return Restrictions.lt(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(LessThanEquals.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                LessThanEquals eq = (LessThanEquals) criterion;
                return Restrictions.le(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(In.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                In eq = (In) criterion;
                return Restrictions.in(eq.getProperty(), eq.getValues());
            }
        });
        criterionAdaptors.put(Between.class, new CriterionAdaptor() {
            public org.hibernate.criterion.Criterion toHibernateCriterion(
                    Criterion criterion) {
                Between eq = (Between) criterion;
                return Restrictions.between(eq.getProperty(), eq.getFrom(), eq.getTo());
            }
        });
    }
    private Criteria criteria;


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
        return new HibernateProjectionList();
    }

    @Override
    public void add(Criterion criterion) {
        final org.hibernate.criterion.Criterion hibernateCriterion = new HibernateCriterionAdaptor(criterion).toHibernateCriterion();
        if (hibernateCriterion != null) {
            criteria.add(hibernateCriterion);
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
        return this.criteria.list();
    }

    private class HibernateCriterionAdaptor {

        private Criterion criterion;

        public HibernateCriterionAdaptor(Criterion criterion) {
            this.criterion = criterion;
        }

        public org.hibernate.criterion.Criterion toHibernateCriterion() {
            final CriterionAdaptor criterionAdaptor = criterionAdaptors.get(criterion.getClass());
            if (criterionAdaptor != null) {
                criterionAdaptor.toHibernateCriterion(criterion);
            }
            return null;
        }
    }

    private static interface CriterionAdaptor {
        public org.hibernate.criterion.Criterion toHibernateCriterion(Criterion criterion);
    }

    private class HibernateJunction extends Junction {

        private org.hibernate.criterion.Junction hibernateJunction;

        public HibernateJunction(org.hibernate.criterion.Junction junction) {
            this.hibernateJunction = junction;
        }

        @Override
        public Junction add(Criterion c) {
            if (c != null) {
                final CriterionAdaptor criterionAdaptor = criterionAdaptors.get(c.getClass());
                if (criterionAdaptor != null) {
                    hibernateJunction.add(criterionAdaptor.toHibernateCriterion(c));
                }
            }
            return this;
        }
    }

    private class HibernateProjectionList extends ProjectionList {

        @Override
        public ProjectionList id() {
            criteria.setProjection(Projections.id());
            return this;
        }

        @Override
        public ProjectionList count() {
            criteria.setProjection(Projections.rowCount());
            return this;
        }

        @Override
        public ProjectionList property(String name) {
            criteria.setProjection(Projections.property(name));
            return this;
        }

        @Override
        public ProjectionList sum(String name) {
            criteria.setProjection(Projections.sum(name));
            return this;
        }

        @Override
        public ProjectionList min(String name) {
            criteria.setProjection(Projections.min(name));
            return this;
        }

        @Override
        public ProjectionList max(String name) {
            criteria.setProjection(Projections.max(name));
            return this;
        }

        @Override
        public ProjectionList avg(String name) {
            criteria.setProjection(Projections.avg(name));
            return this;
        }
    }
}
