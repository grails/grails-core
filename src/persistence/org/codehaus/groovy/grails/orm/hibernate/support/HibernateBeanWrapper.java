package org.codehaus.groovy.grails.orm.hibernate.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * BeanWrapper implementaion that will not lazy initialize entity properties.
 */
public class HibernateBeanWrapper extends BeanWrapperImpl {
// ------------------------------ FIELDS ------------------------------

    private static final Log log = LogFactory.getLog(HibernateBeanWrapper.class);

// --------------------------- CONSTRUCTORS ---------------------------

    public HibernateBeanWrapper() {
    }

    public HibernateBeanWrapper(boolean b) {
        super(b);
    }

    public HibernateBeanWrapper(Object o) {
        super(o);
    }

    public HibernateBeanWrapper(Class aClass) {
        super(aClass);
    }

    public HibernateBeanWrapper(Object o, String s, Object o1) {
        super(o, s, o1);
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface PropertyAccessor ---------------------

    /**
     * Checks Hibernate.isInitialized before calling super method.
     *
     * @param name target property
     * @return null if false or super'name value if true
     * @throws BeansException
     */
    public Object getPropertyValue(String name) throws BeansException {
        PropertyDescriptor desc = getPropertyDescriptor(name);
        Method method = desc.getReadMethod();
        Object owner = getWrappedInstance();
        try {
            if (Hibernate.isInitialized(method.invoke(owner, null))) {
                return super.getPropertyValue(name);
            }
        } catch (Exception e) {
            log.error("Error checking Hibernate initialization on method " +
                    method.getName() + " for class " + owner.getClass(), e);
        }
        return null;
    }
}
