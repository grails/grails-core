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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
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
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * @author Graeme Rocher
 * @since 2.0
 */
public class HibernateCriterionAdapter {
    private static final Map<Class<?>, CriterionAdaptor> criterionAdaptors = new HashMap<Class<?>, CriterionAdaptor>();
    private String alias;

    public HibernateCriterionAdapter(PersistentEntity entity, Query.Criterion criterion, String alias) {
        this.criterion = criterion;
        this.alias = alias;
    }

    static {
        //add simple property criterions
        addPropertyCriterionAdaptor(Query.Equals.class, "eq");
        addPropertyCriterionAdaptor(Query.NotEquals.class, "ne");
        addPropertyCriterionAdaptor(Query.GreaterThan.class, "gt");
        addPropertyCriterionAdaptor(Query.GreaterThanEquals.class, "ge");
        addPropertyCriterionAdaptor(Query.LessThan.class, "lt");
        addPropertyCriterionAdaptor(Query.LessThanEquals.class, "le");

        //add simple size criterions
        addPropertySizeCriterionAdaptor(Query.SizeEquals.class, "sizeEq");
        addPropertySizeCriterionAdaptor(Query.SizeGreaterThan.class, "sizeGt");
        addPropertySizeCriterionAdaptor(Query.SizeGreaterThanEquals.class, "sizeGe");
        addPropertySizeCriterionAdaptor(Query.SizeLessThan.class, "sizeLt");
        addPropertySizeCriterionAdaptor(Query.SizeLessThanEquals.class, "sizeLe");

        //add simple criterions
        addPropertyNameCriterionAdaptor(Query.IsNull.class, "isNull");
        addPropertyNameCriterionAdaptor(Query.IsNotNull.class, "isNotNull");
        addPropertyNameCriterionAdaptor(Query.IsEmpty.class, "isEmpty");
        addPropertyNameCriterionAdaptor(Query.IsNotEmpty.class, "isNotEmpty");

        //add simple property comparison criterions
        addPropertyComparisonCriterionAdaptor(Query.EqualsProperty.class, "eqProperty");
        addPropertyComparisonCriterionAdaptor(Query.GreaterThanProperty.class, "gtProperty");
        addPropertyComparisonCriterionAdaptor(Query.GreaterThanEqualsProperty.class, "geProperty");
        addPropertyComparisonCriterionAdaptor(Query.LessThanProperty.class, "ltProperty");
        addPropertyComparisonCriterionAdaptor(Query.LessThanEqualsProperty.class, "leProperty");
        addPropertyComparisonCriterionAdaptor(Query.NotEqualsProperty.class, "neProperty");

        addJunctionCriterionAdaptor(Query.Conjunction.class, "conjunction");
        addJunctionCriterionAdaptor(Query.Disjunction.class, "disjunction");

        addPropertyLikeCriterionAdaptor(Query.Like.class, "like");
        addPropertyLikeCriterionAdaptor(Query.ILike.class, "ilike");

        criterionAdaptors.put(DetachedAssociationCriteria.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                DetachedAssociationCriteria<?> existing = (DetachedAssociationCriteria<?>) criterion;
                String newAlias = hibernateQuery.handleAssociationQuery(existing.getAssociation(), existing.getCriteria());
                if (alias == null) {
                    alias = newAlias;
                }
                else {
                    alias = alias + '.' + newAlias;
                }
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
                String newAlias = hibernateQuery.handleAssociationQuery(existing.getAssociation(), existing.getCriteria().getCriteria());
                if (alias == null) {
                    alias = newAlias;
                }
                else {
                    alias += '.' + newAlias;
                }
                applySubCriteriaToJunction(existing.getAssociation().getAssociatedEntity(), hibernateQuery, existing.getCriteria().getCriteria(), conjunction, alias);
                return conjunction;
            }
        });
        criterionAdaptors.put(Query.Negation.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                return Restrictions.not(criterionAdaptors.get(Query.Disjunction.class).toHibernateCriterion(hibernateQuery, criterion, alias));
            }
        });
        criterionAdaptors.put(Query.Between.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.Between btwCriterion = (Query.Between) criterion;
                return Restrictions.between(calculatePropertyName(calculatePropertyName(btwCriterion.getProperty(), alias), alias), btwCriterion.getFrom(), btwCriterion.getTo());
            }
        });
        criterionAdaptors.put(Query.IdEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                return Restrictions.idEq(((Query.IdEquals) criterion).getValue());
            }
        });
        criterionAdaptors.put(Query.RLike.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                return new RlikeExpression(getPropertyName(criterion, alias), ((Query.RLike) criterion).getPattern());
            }
        });
        criterionAdaptors.put(Query.In.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                return Restrictions.in(getPropertyName(criterion, alias), ((Query.In) criterion).getValues());
            }
        });
    }

    /** utility methods to group and clean up the initialization of the Criterion Adapters**/

    //used for PropertyCriterions with a specified value ( which can be a subquery)
    private static void addPropertyCriterionAdaptor(final Class<?> clazz, final String constraintName) {
        addCriterionAdaptor(clazz, constraintName, Object.class);
    }

    //used for collection size PropertyCriterions
    private static void addPropertySizeCriterionAdaptor(final Class<?> clazz, final String constraintName) {
        addCriterionAdaptor(clazz, constraintName, int.class);
    }

    private static void addCriterionAdaptor(final Class<?> clazz, final String constraintName, final Class<?> valueClass) {
        criterionAdaptors.put(clazz, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Query.PropertyCriterion propertyCriterion = (Query.PropertyCriterion) criterion;
                Object value = propertyCriterion.getValue();
                String propertyName = getPropertyName(criterion, alias);
                if (value instanceof DetachedCriteria) {
                    Method propertyMethod = ReflectionUtils.findMethod(Property.class, constraintName, new Class[]{DetachedCriteria.class});
                    if (propertyMethod != null) { // if supports subqueries
                        return (Criterion) ReflectionUtils.invokeMethod(propertyMethod, Property.forName(propertyName), new Object[]{value});
                    }
                }
                return callRestrictionsMethod(constraintName, new Class[]{String.class, valueClass}, new Object[]{propertyName, value});
            }
        });
    }

    // use for criterions without a value
    private static void addPropertyNameCriterionAdaptor(final Class<?> clazz, final String constraintName) {
        criterionAdaptors.put(clazz, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                return callRestrictionsMethod(constraintName, new Class[]{String.class}, new Object[]{getPropertyName(criterion, alias)});
            }
        });
    }

    // use for criterions used to compare 2 properties
    private static void addPropertyComparisonCriterionAdaptor(final Class<?> clazz, final String constraintName) {
        criterionAdaptors.put(clazz, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                return callRestrictionsMethod(constraintName, new Class[]{String.class, String.class}, new Object[]{getPropertyName(criterion, alias), ((Query.PropertyComparisonCriterion) criterion).getOtherProperty()});
            }
        });
    }

    // use for regular expression criterions
    private static void addPropertyLikeCriterionAdaptor(final Class<?> clazz, final String constraintName) {
        criterionAdaptors.put(clazz, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                return callRestrictionsMethod(constraintName, new Class[]{String.class, Object.class}, new Object[]{getPropertyName(criterion, alias), convertStringValue(((Query.Like) criterion).getValue())});
            }
        });
    }

    // used for junctions
    private static void addJunctionCriterionAdaptor(final Class<?> clazz, final String constraintName) {
        criterionAdaptors.put(clazz, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(HibernateQuery hibernateQuery, Query.Criterion criterion, String alias) {
                Junction junction = (Junction) callRestrictionsMethod(constraintName, new Class[0], new Object[0]);
                applySubCriteriaToJunction(hibernateQuery.getEntity(), hibernateQuery, ((Query.Junction) criterion).getCriteria(), junction, alias);
                return junction;
            }
        });
    }

    /**
     * utility method that generically returns a criterion using methods in Restrictions
     *
     * @param constraintName - the criteria
     */
    private static Criterion callRestrictionsMethod(String constraintName, Class<?>[] paramTypes, Object[] params) {
        final Method restrictionsMethod = ReflectionUtils.findMethod(Restrictions.class, constraintName, paramTypes);
        Assert.notNull(restrictionsMethod, "Could not find method: " + constraintName + " in class Restrictions for parameters: " + ArrayUtils.toString(params) + " with types: " + ArrayUtils.toString(paramTypes));
        return (Criterion) ReflectionUtils.invokeMethod(restrictionsMethod, null, params);
    }

    private static String getPropertyName(Query.Criterion criterion, String alias) {
        return calculatePropertyName(((Query.PropertyNameCriterion) criterion).getProperty(), alias);
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
