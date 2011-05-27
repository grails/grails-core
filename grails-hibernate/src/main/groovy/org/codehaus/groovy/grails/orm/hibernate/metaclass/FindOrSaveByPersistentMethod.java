package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.hibernate.SessionFactory;

public class FindOrSaveByPersistentMethod extends FindOrCreateByPersistentMethod {

    private static final String METHOD_PATTERN = "(findOrSaveBy)([A-Z]\\w*)";

    /**
     * Constructor.
     * @param application
     * @param sessionFactory
     * @param classLoader
     */
    public FindOrSaveByPersistentMethod(GrailsApplication application,SessionFactory sessionFactory, ClassLoader classLoader) {
        super(application,sessionFactory, classLoader, METHOD_PATTERN);
    }

    @Override
    protected boolean shouldSaveOnCreate() {
        return true;
    }
}
