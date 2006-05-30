package org.codehaus.groovy.grails.plugins.hibernate;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.orm.hibernate3.HibernateTransactionManager;

/**
 * <p>Plugin that registers {@link HibernateTransactionManager} as <code>transactionManager</code>
 * if no {@link org.springframework.beans.factory.config.BeanDefinition} named <code>transactionManager</code>
 * is registered.</p>
 *
 * <p>If one is registered this will be used as transaction manager for all transactional operations.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class HibernateTransactionManagerPlugin extends OrderedAdapter implements GrailsPlugin {
    /**
     * <p>This constructor sets the <code>order</code> property to the maximum
     * integer value to make sure other plugins get a chance to register a
     * transaction manager. Obviously the value for the <code>order</code> property
     * can be overwritten in the XML file configuration.</p>
     */
    public HibernateTransactionManagerPlugin() {
        setOrder(Integer.MAX_VALUE);
    }

    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {

        if (!applicationContext.containsBeanDefinition("transactionManager")) {
            RootBeanDefinition bd = new RootBeanDefinition(HibernateTransactionManager.class);
            MutablePropertyValues mpv = new MutablePropertyValues();
            mpv.addPropertyValue("sessionFactory", new RuntimeBeanReference("sessionFactory"));
            bd.setPropertyValues(mpv);

            applicationContext.registerBeanDefinition("transactionManager", bd);
        }
    }
}
