package org.codehaus.groovy.grails.plugins.hibernate;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactory;

/**
 * <p>Lazy-loading implementation class that loads a bean from the Spring
 * <code>BeanFactory</code>. This may look like a problematic implementation
 * since it implements the <code>BeanFactoryAware</code> interface.</p>
 *
 * <p>However, this class is meant to be configured as a <code>bean definition</code>
 * while the <code>BeanFactoryAware</code> interface is handled by the
 * {@link org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory} class,
 * so not by a <code>BeanPostProcessor</code>.<p>
 *
 * @author Steven Devijver
 * @since 0.2
 * @see SessionFactoryAware
 */
public class LazyLoadingSessionFactoryAwareBeanPostProcessor extends AbstractSessionFactoryAwareBeanPostProcessor
    implements BeanFactoryAware {
    private String sessionFactoryBeanName;
    private BeanFactory beanFactory;

    public void setSessionFactoryBeanName(String sessionFactoryBeanName) {
        this.sessionFactoryBeanName = sessionFactoryBeanName;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * <p>This method loads the <code>SessionFactory</code> bean from
     * the <code>BeanFactory</code> with the name set on the
     * <code>sessionFactoryBeanName</code> property every time this method is called.</p>
     *
     * @return the <code>SessionFactory</code> bean
     * @throws org.springframework.beans.BeansException if the <code>SessionFactory</code> bean
     * cannot be loaded from the <code>BeanFactory</code>.
     */
    protected SessionFactory getSessionFactory() {
        return (SessionFactory)beanFactory.getBean(sessionFactoryBeanName, SessionFactory.class);
    }
}
