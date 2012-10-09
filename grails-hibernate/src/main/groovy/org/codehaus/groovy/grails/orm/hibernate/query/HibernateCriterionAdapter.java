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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.grails.datastore.gorm.query.criteria.DetachedAssociationCriteria;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.AssociationQuery;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.QueryableCriteria;
import org.grails.datastore.mapping.query.criteria.FunctionCallingCriterion;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

/**
 * @author Graeme Rocher
 * @since 2.0
 */
public class HibernateCriterionAdapter {
    private static final Map<Class<?>, CriterionAdaptor> criterionAdaptors = new HashMap<Class<?>, CriterionAdaptor>();
    private String alias;
    private PersistentEntity entity;

    public HibernateCriterionAdapter(PersistentEntity entity, Query.Criterion criterion, String alias) {
        this.criterion = criterion;
        this.alias = alias;
        this.entity = entity;
    }


    static {
        criterionAdaptors.put(DetachedAssociationCriteria.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                DetachedAssociationCriteria<?> existing = (DetachedAssociationCriteria<?>) criterion;
                alias = hibernateQuery.handleAssociationQuery(existing.getAssociation(), existing.getCriteria());
                Junction conjunction = Restrictions.conjunction();
                applySubCriteriaToJunction(existing.getAssociation().getAssociatedEntity(), hibernateQuery, existing.getCriteria(), conjunction, alias);
                return conjunction;
            }
        });
        criterionAdaptors.put(AssociationQuery.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                AssociationQuery existing = (AssociationQuery) criterion;
                Junction conjunction = Restrictions.conjunction();
                alias = hibernateQuery.handleAssociationQuery(existing.getAssociation(), existing.getCriteria().getCriteria());
                applySubCriteriaToJunction(existing.getAssociation().getAssociatedEntity(), hibernateQuery, existing.getCriteria().getCriteria(), conjunction, alias);
                return conjunction;
            }
        });
        criterionAdaptors.put(Query.Conjunction.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.Junction existing = (Query.Conjunction) criterion;
                Junction conjunction = Restrictions.conjunction();

                applySubCriteriaToJunction(hibernateQuery.getEntity(), hibernateQuery, existing.getCriteria(), conjunction, alias);
                return conjunction;
            }
        });
       criterionAdaptors.put(Query.Disjunction.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.Junction existing = (Query.Junction) criterion;
                Junction disjunction = Restrictions.disjunction();

                applySubCriteriaToJunction(hibernateQuery.getEntity(), hibernateQuery, existing.getCriteria(), disjunction, alias);
                return disjunction;
            }
        });
       criterionAdaptors.put(Query.Negation.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.Junction existing = (Query.Junction) criterion;
                Junction disjunction = Restrictions.disjunction();

                applySubCriteriaToJunction(hibernateQuery.getEntity(), hibernateQuery, existing.getCriteria(), disjunction, alias);
                return Restrictions.not(disjunction);
            }
        });
        criterionAdaptors.put(Query.Between.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.Between eq = (Query.Between) criterion;
                return Restrictions.between(calculatePropertyName(calculatePropertyName(eq.getProperty(), alias), alias), eq.getFrom(), eq.getTo());
            }
        });
        criterionAdaptors.put(Query.SizeEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.SizeEquals eq = (Query.SizeEquals) criterion;
                return Restrictions.sizeEq(calculatePropertyName(eq.getProperty(), alias),(Integer)eq.getValue());
            }
        });
        criterionAdaptors.put(Query.SizeGreaterThan.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.SizeGreaterThan eq = (Query.SizeGreaterThan) criterion;
                return Restrictions.sizeGt(calculatePropertyName(eq.getProperty(), alias), (Integer) eq.getValue());
            }
        });
        criterionAdaptors.put(Query.SizeGreaterThanEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.SizeGreaterThanEquals eq = (Query.SizeGreaterThanEquals) criterion;
                return Restrictions.sizeGe(calculatePropertyName(eq.getProperty(), alias), (Integer) eq.getValue());
            }
        });
        criterionAdaptors.put(Query.SizeLessThan.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.SizeLessThan eq = (Query.SizeLessThan) criterion;
                return Restrictions.sizeLt(calculatePropertyName(eq.getProperty(), alias), (Integer) eq.getValue());
            }
        });
        criterionAdaptors.put(Query.SizeLessThanEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.SizeLessThanEquals eq = (Query.SizeLessThanEquals) criterion;
                return Restrictions.sizeLe(calculatePropertyName(eq.getProperty(), alias), (Integer) eq.getValue());
            }
        });
        criterionAdaptors.put(Query.EqualsProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.EqualsProperty eq = (Query.EqualsProperty) criterion;
                return Restrictions.eqProperty(calculatePropertyName(eq.getProperty(), alias), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.GreaterThanProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.GreaterThanProperty eq = (Query.GreaterThanProperty) criterion;
                return Restrictions.gtProperty(calculatePropertyName(eq.getProperty(), alias), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.GreaterThanEqualsProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.GreaterThanEqualsProperty eq = (Query.GreaterThanEqualsProperty) criterion;
                return Restrictions.geProperty(calculatePropertyName(eq.getProperty(), alias), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.LessThanProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.LessThanProperty eq = (Query.LessThanProperty) criterion;
                return Restrictions.ltProperty(calculatePropertyName(eq.getProperty(), alias), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.LessThanEqualsProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.LessThanEqualsProperty eq = (Query.LessThanEqualsProperty) criterion;
                return Restrictions.leProperty(calculatePropertyName(eq.getProperty(), alias), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.NotEqualsProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.NotEqualsProperty eq = (Query.NotEqualsProperty) criterion;
                return Restrictions.neProperty(calculatePropertyName(eq.getProperty(), alias), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.Equals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.Equals eq = (Query.Equals) criterion;
                return Restrictions.eq(calculatePropertyName(eq.getProperty(), alias), convertStringValue(eq.getValue()));
            }
        });
        criterionAdaptors.put(Query.IdEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.IdEquals eq = (Query.IdEquals) criterion;
                return Restrictions.idEq(eq.getValue());
            }
        });
        criterionAdaptors.put(Query.IsNull.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.IsNull eq = (Query.IsNull) criterion;
                return Restrictions.isNull(calculatePropertyName(eq.getProperty(), alias));
            }
        });
        criterionAdaptors.put(Query.IsNotNull.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.IsNotNull eq = (Query.IsNotNull) criterion;
                return Restrictions.isNotNull(calculatePropertyName(eq.getProperty(), alias));
            }
        });
        criterionAdaptors.put(Query.IsEmpty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.IsEmpty eq = (Query.IsEmpty) criterion;
                return Restrictions.isEmpty(calculatePropertyName(eq.getProperty(), alias));
            }
        });
        criterionAdaptors.put(Query.IsNotEmpty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.IsNotEmpty eq = (Query.IsNotEmpty) criterion;
                return Restrictions.isNotEmpty(calculatePropertyName(eq.getProperty(), alias));
            }
        });

        criterionAdaptors.put(Query.Like.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.Like eq = (Query.Like) criterion;
                return Restrictions.like(calculatePropertyName(eq.getProperty(), alias), convertStringValue(eq.getValue()));
            }
        });
        criterionAdaptors.put(Query.ILike.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.ILike eq = (Query.ILike) criterion;
                return Restrictions.ilike(calculatePropertyName(eq.getProperty(), alias), convertStringValue(eq.getValue()));
            }
        });
        criterionAdaptors.put(Query.RLike.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.RLike eq = (Query.RLike) criterion;
                return new RlikeExpression(calculatePropertyName(eq.getProperty(), alias), eq.getPattern());
            }
        });
        criterionAdaptors.put(Query.NotEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.NotEquals eq = (Query.NotEquals) criterion;
                Object value = eq.getValue();
                if (value instanceof DetachedCriteria) {
                    return Property.forName(calculatePropertyName(eq.getProperty(), alias)).ne((DetachedCriteria)value);
                }
                return Restrictions.ne(calculatePropertyName(eq.getProperty(), alias), value);
            }
        });
        criterionAdaptors.put(Query.GreaterThan.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.GreaterThan eq = (Query.GreaterThan) criterion;
                Object value = eq.getValue();
                if (value instanceof DetachedCriteria) {
                    return Property.forName(calculatePropertyName(eq.getProperty(), alias)).gt((DetachedCriteria)value);
                }
                return Restrictions.gt(calculatePropertyName(eq.getProperty(), alias), value);
            }
        });
        criterionAdaptors.put(Query.GreaterThanEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.GreaterThanEquals eq = (Query.GreaterThanEquals) criterion;
                Object value = eq.getValue();
                if (value instanceof DetachedCriteria) {
                    return Property.forName(calculatePropertyName(eq.getProperty(), alias)).ge((DetachedCriteria) value);
                }
                return Restrictions.ge(calculatePropertyName(eq.getProperty(), alias), value);
            }
        });
        criterionAdaptors.put(Query.LessThan.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.LessThan eq = (Query.LessThan) criterion;
                Object value = eq.getValue();
                if (value instanceof DetachedCriteria) {
                    return Property.forName(calculatePropertyName(eq.getProperty(), alias)).lt((DetachedCriteria) value);
                }
                return Restrictions.lt(calculatePropertyName(eq.getProperty(), alias), value);
            }
        });
        criterionAdaptors.put(Query.LessThanEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.LessThanEquals eq = (Query.LessThanEquals) criterion;
                Object value = eq.getValue();
                if (value instanceof DetachedCriteria) {
                    return Property.forName(calculatePropertyName(eq.getProperty(), alias)).le((DetachedCriteria) value);
                }
                return Restrictions.le(calculatePropertyName(eq.getProperty(), alias), value);
            }
        });
        criterionAdaptors.put(Query.In.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.In eq = (Query.In) criterion;
                return Restrictions.in(calculatePropertyName(eq.getProperty(), alias), eq.getValues());
            }
        });
    }

    private static String calculatePropertyName(String property, String alias) {
        if (alias != null) {
            return alias + '.' + property;
        }
        return property;
    }

    private static void applySubCriteriaToJunction(PersistentEntity entity, HibernateQuery hibernateCriteria, List<Query.Criterion> existing, Junction conjunction, String alias) {
        for (Query.Criterion subCriterion : existing) {

            if (subCriterion instanceof Query.PropertyCriterion) {
                Query.PropertyCriterion pc = (Query.PropertyCriterion) subCriterion;
                if (pc.getValue() instanceof QueryableCriteria) {
                    pc.setValue(
                        HibernateCriteriaBuilder.getHibernateDetachedCriteria((QueryableCriteria<?>) pc.getValue())
                    );
                }
                else {
                    HibernateQuery.doTypeConversionIfNeccessary(entity, pc);
                }
            }
            CriterionAdaptor criterionAdaptor = criterionAdaptors.get(subCriterion.getClass());
            if (criterionAdaptor != null) {
                Criterion c = criterionAdaptor.toHibernateCriterion(hibernateCriteria, subCriterion, alias);
                if (c != null)
                    conjunction.add(c);
            }
            else if (subCriterion instanceof FunctionCallingCriterion) {
                Criterion sqlRestriction = hibernateCriteria.getRestrictionForFunctionCall((FunctionCallingCriterion) subCriterion, entity);
                if (sqlRestriction != null) {
                   conjunction.add(sqlRestriction);
                }
            }
        }
    }


    private Query.Criterion criterion;

    public HibernateCriterionAdapter(Query.Criterion criterion) {
        this.criterion = criterion;
    }

    public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery) {
        final CriterionAdaptor criterionAdaptor = criterionAdaptors.get(criterion.getClass());
        if (criterionAdaptor != null) {
            return criterionAdaptor.toHibernateCriterion(hibernateQuery, criterion, alias);
        }
        return null;
    }


    public static abstract class CriterionAdaptor {
        public abstract org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias);

        protected Object convertStringValue(Object o) {
            if ((!(o instanceof String)) && (o instanceof CharSequence)) {
                o = o.toString();
            }
            return o;
        }
    }
}
