package org.codehaus.groovy.grails.plugins.hibernate;

import org.hibernate.SessionFactory;

/**
 * <p>Special purpose aware interface for classes that want easy
 * access to the Hibernate {@link org.hibernate.SessionFactory}.</p>
 *
 * <p>This level of convenience comes with a risk however. When instantiating
 * the {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * that handles this interface the Hibernate <code>SessionFactory</code> that
 * is configured in the <code>BeanFactory</code> <em>could</em> be instantiated.</p>
 *
 * <p>This should be avoided if possible since the implementation of the
 * <code>SessionFactory</code> interface could and probably will implement other
 * <code>*Aware</code> interfaces which involves other <code>BeanPostProcessor</code>s.</p>
 *
 * <p>Creating normal beans during creation of <code>BeanPostProcessor</code>s is undesirable
 * because it can lead to abnormalities and confusion. As a workaround the <code>BeanPostProcessor</code>
 * that handles this interface should lazy-load the <code>SessionFactory</code> bean.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public interface SessionFactoryAware {
    void setSessionFactory(SessionFactory sessionFactory);
}
