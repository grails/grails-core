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
import org.grails.datastore.mapping.query.Query;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Graeme Rocher
 * @since 2.0
 */
public class HibernateCriterionAdapter {
    private static final Map<Class<?>, CriterionAdaptor> criterionAdaptors = new HashMap<Class<?>, CriterionAdaptor>();

    static {
        criterionAdaptors.put(Query.Conjunction.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.Junction existing = (Query.Conjunction) criterion;
                Junction conjunction = Restrictions.conjunction();

                applySubCriteriaToJunction(existing, conjunction);
                return conjunction;
            }
        });
       criterionAdaptors.put(Query.Disjunction.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.Junction existing = (Query.Junction) criterion;
                Junction disjunction = Restrictions.disjunction();

                applySubCriteriaToJunction(existing, disjunction);
                return disjunction;
            }
        });
       criterionAdaptors.put(Query.Negation.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.Junction existing = (Query.Junction) criterion;
                Junction disjunction = Restrictions.disjunction();

                applySubCriteriaToJunction(existing, disjunction);
                return Restrictions.not(disjunction);
            }
        });
        criterionAdaptors.put(Query.Between.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.Between eq = (Query.Between) criterion;
                return Restrictions.between(eq.getProperty(), eq.getFrom(), eq.getTo());
            }
        });
        criterionAdaptors.put(Query.SizeEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.SizeEquals eq = (Query.SizeEquals) criterion;
                return Restrictions.sizeEq(eq.getProperty(),(Integer)eq.getValue());
            }
        });
        criterionAdaptors.put(Query.SizeGreaterThan.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.SizeGreaterThan eq = (Query.SizeGreaterThan) criterion;
                return Restrictions.sizeGt(eq.getProperty(), (Integer) eq.getValue());
            }
        });
        criterionAdaptors.put(Query.SizeGreaterThanEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.SizeGreaterThanEquals eq = (Query.SizeGreaterThanEquals) criterion;
                return Restrictions.sizeGe(eq.getProperty(), (Integer) eq.getValue());
            }
        });
        criterionAdaptors.put(Query.SizeLessThan.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.SizeLessThan eq = (Query.SizeLessThan) criterion;
                return Restrictions.sizeLt(eq.getProperty(), (Integer) eq.getValue());
            }
        });
        criterionAdaptors.put(Query.SizeLessThanEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.SizeLessThanEquals eq = (Query.SizeLessThanEquals) criterion;
                return Restrictions.sizeLe(eq.getProperty(), (Integer) eq.getValue());
            }
        });
        criterionAdaptors.put(Query.EqualsProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.EqualsProperty eq = (Query.EqualsProperty) criterion;
                return Restrictions.eqProperty(eq.getProperty(), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.GreaterThanProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.GreaterThanProperty eq = (Query.GreaterThanProperty) criterion;
                return Restrictions.gtProperty(eq.getProperty(), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.GreaterThanEqualsProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.GreaterThanEqualsProperty eq = (Query.GreaterThanEqualsProperty) criterion;
                return Restrictions.geProperty(eq.getProperty(), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.LessThanProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.LessThanProperty eq = (Query.LessThanProperty) criterion;
                return Restrictions.ltProperty(eq.getProperty(), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.LessThanEqualsProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.LessThanEqualsProperty eq = (Query.LessThanEqualsProperty) criterion;
                return Restrictions.leProperty(eq.getProperty(), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.NotEqualsProperty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.NotEqualsProperty eq = (Query.NotEqualsProperty) criterion;
                return Restrictions.neProperty(eq.getProperty(), eq.getOtherProperty());
            }
        });
        criterionAdaptors.put(Query.Equals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.Equals eq = (Query.Equals) criterion;
                return Restrictions.eq(eq.getProperty(), convertStringValue(eq.getValue()));
            }
        });
        criterionAdaptors.put(Query.IdEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.IdEquals eq = (Query.IdEquals) criterion;
                return Restrictions.idEq(eq.getValue());
            }
        });
        criterionAdaptors.put(Query.IsNull.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.IsNull eq = (Query.IsNull) criterion;
                return Restrictions.isNull(eq.getProperty());
            }
        });
        criterionAdaptors.put(Query.IsNotNull.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.IsNotNull eq = (Query.IsNotNull) criterion;
                return Restrictions.isNotNull(eq.getProperty());
            }
        });
        criterionAdaptors.put(Query.IsEmpty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.IsEmpty eq = (Query.IsEmpty) criterion;
                return Restrictions.isEmpty(eq.getProperty());
            }
        });
        criterionAdaptors.put(Query.IsNotEmpty.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.IsNotEmpty eq = (Query.IsNotEmpty) criterion;
                return Restrictions.isNotEmpty(eq.getProperty());
            }
        });

        criterionAdaptors.put(Query.Like.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.Like eq = (Query.Like) criterion;
                return Restrictions.like(eq.getProperty(), convertStringValue(eq.getValue()));
            }
        });
        criterionAdaptors.put(Query.ILike.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.ILike eq = (Query.ILike) criterion;
                return Restrictions.ilike(eq.getProperty(), convertStringValue(eq.getValue()));
            }
        });
        criterionAdaptors.put(Query.RLike.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.RLike eq = (Query.RLike) criterion;
                return new RlikeExpression(eq.getProperty(), eq.getPattern());
            }
        });
        criterionAdaptors.put(Query.NotEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.NotEquals eq = (Query.NotEquals) criterion;
                return Restrictions.ne(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(Query.GreaterThan.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.GreaterThan eq = (Query.GreaterThan) criterion;
                return Restrictions.gt(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(Query.GreaterThanEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.GreaterThanEquals eq = (Query.GreaterThanEquals) criterion;
                return Restrictions.ge(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(Query.LessThan.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.LessThan eq = (Query.LessThan) criterion;
                return Restrictions.lt(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(Query.LessThanEquals.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.LessThanEquals eq = (Query.LessThanEquals) criterion;
                return Restrictions.le(eq.getProperty(), eq.getValue());
            }
        });
        criterionAdaptors.put(Query.In.class, new CriterionAdaptor() {
            @Override
            public org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion) {
                Query.In eq = (Query.In) criterion;
                return Restrictions.in(eq.getProperty(), eq.getValues());
            }
        });

    }

    private static void applySubCriteriaToJunction(Query.Junction existing, Junction conjunction) {
        for (Query.Criterion subCriterion : existing.getCriteria()) {
            CriterionAdaptor criterionAdaptor = criterionAdaptors.get(subCriterion.getClass());
            if(criterionAdaptor != null) {
                Criterion c = criterionAdaptor.toHibernateCriterion(subCriterion);
                if(c != null)
                    conjunction.add(c);
            }
        }
    }

    private Query.Criterion criterion;

    public HibernateCriterionAdapter(Query.Criterion criterion) {
        this.criterion = criterion;
    }

    public org.hibernate.criterion.Criterion toHibernateCriterion() {
        final CriterionAdaptor criterionAdaptor = criterionAdaptors.get(criterion.getClass());
        if (criterionAdaptor != null) {
            return criterionAdaptor.toHibernateCriterion(criterion);
        }
        return null;
    }
    private static abstract class CriterionAdaptor {
        public abstract org.hibernate.criterion.Criterion toHibernateCriterion(Query.Criterion criterion);

        protected Object convertStringValue(Object o) {
            if ((!(o instanceof String)) && (o instanceof CharSequence)) {
                o = o.toString();
            }
            return o;
        }
    }
}
