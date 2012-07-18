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

import org.grails.datastore.mapping.query.Query;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapts Grails datastore API to Hibernate projections.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class HibernateProjectionAdapter {
    private Query.Projection projection;
    private static Map<Class<?>, ProjectionAdapter> adapterMap = new HashMap<Class<?>, ProjectionAdapter>();

    static {
        adapterMap.put(Query.AvgProjection.class, new ProjectionAdapter() {
            public Projection toHibernateProjection(Query.Projection gormProjection) {
                Query.AvgProjection avg = (Query.AvgProjection) gormProjection;
                return Projections.avg(avg.getPropertyName());
            }
        });
        adapterMap.put(Query.SumProjection.class, new ProjectionAdapter() {
            public Projection toHibernateProjection(Query.Projection gormProjection) {
                Query.SumProjection avg = (Query.SumProjection) gormProjection;
                return Projections.sum(avg.getPropertyName());
            }
        });
        adapterMap.put(Query.DistinctPropertyProjection.class, new ProjectionAdapter() {
            public Projection toHibernateProjection(Query.Projection gormProjection) {
                Query.DistinctPropertyProjection avg = (Query.DistinctPropertyProjection) gormProjection;
                return Projections.distinct(Projections.property(avg.getPropertyName()));
            }
        });
        adapterMap.put(Query.PropertyProjection.class, new ProjectionAdapter() {
            public Projection toHibernateProjection(Query.Projection gormProjection) {
                Query.PropertyProjection avg = (Query.PropertyProjection) gormProjection;
                return Projections.property(avg.getPropertyName());
            }
        });
        adapterMap.put(Query.CountDistinctProjection.class, new ProjectionAdapter() {
            public Projection toHibernateProjection(Query.Projection gormProjection) {
                Query.CountDistinctProjection cd = (Query.CountDistinctProjection) gormProjection;
                return Projections.countDistinct(cd.getPropertyName());
            }
        });
        adapterMap.put(Query.MaxProjection.class, new ProjectionAdapter() {
            public Projection toHibernateProjection(Query.Projection gormProjection) {
                Query.MaxProjection cd = (Query.MaxProjection) gormProjection;
                return Projections.max(cd.getPropertyName());
            }
        });
        adapterMap.put(Query.MinProjection.class, new ProjectionAdapter() {
            public Projection toHibernateProjection(Query.Projection gormProjection) {
                Query.MinProjection cd = (Query.MinProjection) gormProjection;
                return Projections.min(cd.getPropertyName());
            }
        });
    }

    public HibernateProjectionAdapter(Query.Projection projection) {
        this.projection = projection;
    }

    public Projection toHibernateProjection() {
        ProjectionAdapter projectionAdapter = adapterMap.get(projection.getClass());
        return projectionAdapter.toHibernateProjection(projection);
    }

    private static interface ProjectionAdapter {
        Projection toHibernateProjection(Query.Projection gormProjection);
    }
}
