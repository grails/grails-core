package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class GrailsHibernateTemplate extends HibernateTemplate {
    public GrailsHibernateTemplate() {
        super();
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
        if(application != null) {
            setCacheQueries(GrailsHibernateUtil.isCacheQueriesByDefault(application));
        }
    }

    public void applySettings(Query queryObject) {
        if(isExposeNativeSession()) {
            super.prepareQuery(queryObject);
        }
    }

    public void applySettings(Criteria criteria) {
        if(isExposeNativeSession()) {
            super.prepareCriteria(criteria);
        }
    }
}
