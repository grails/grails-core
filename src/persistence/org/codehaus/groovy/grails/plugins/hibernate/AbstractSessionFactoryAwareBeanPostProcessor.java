package org.codehaus.groovy.grails.plugins.hibernate;

import org.codehaus.groovy.grails.plugins.support.BeanPostProcessorAdapter;
import org.springframework.beans.BeansException;
import org.hibernate.SessionFactory;

/**
 * <p>Abstract class that detects the {@link SessionFactoryAware} interface.</p>
 *
 * <p>This implementation does not define how the <code>SessionFactory</code> bean
 * is obtained. Ideally this happens through lazy loading, see {@link SessionFactoryAware}
 * for more information.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public abstract class AbstractSessionFactoryAwareBeanPostProcessor extends BeanPostProcessorAdapter {
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SessionFactoryAware) {
            SessionFactory sessionFactory = getSessionFactory();
            ((SessionFactoryAware)bean).setSessionFactory(sessionFactory);
        }
        return bean;
    }

    /**
     * <p>This method returns a Hibernate <code>SessionFactory</code> instance.</p>
     *
     * <p>Implementation classes can implement any strategy although lazy-loading is
     * preferred.</p>
     *
     * @return the Hibernate <code>SessionFactory</code> bean or null if no bean could be determined.
     */
    protected abstract SessionFactory getSessionFactory();
}
