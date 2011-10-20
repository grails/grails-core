/*
 * Copyright 2011 SpringSource.
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
package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class GrailsHibernateTemplate extends HibernateTemplate {

    public GrailsHibernateTemplate() {
        initialize(null);
    }

    public GrailsHibernateTemplate(SessionFactory sessionFactory, boolean allowCreate) {
        super(sessionFactory, allowCreate);
        initialize(null);
    }

    public GrailsHibernateTemplate(SessionFactory sessionFactory) {
        super(sessionFactory);
        initialize(null);
    }

    public GrailsHibernateTemplate(SessionFactory sessionFactory, GrailsApplication application) {
        super(sessionFactory);
        initialize(application);
    }

    private void initialize(GrailsApplication application) {
        setExposeNativeSession(true);
        if (application != null) {
            setCacheQueries(GrailsHibernateUtil.isCacheQueriesByDefault(application));
        }
    }

    public void applySettings(Query queryObject) {
        if (isExposeNativeSession()) {
            super.prepareQuery(queryObject);
        }
    }

    public void applySettings(Criteria criteria) {
        if (isExposeNativeSession()) {
            super.prepareCriteria(criteria);
        }
    }
}
