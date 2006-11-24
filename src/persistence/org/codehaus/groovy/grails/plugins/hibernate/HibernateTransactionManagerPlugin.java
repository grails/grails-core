package org.codehaus.groovy.grails.plugins.hibernate;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
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
public class HibernateTransactionManagerPlugin extends AbstractGrailsPlugin implements GrailsPlugin {

    public HibernateTransactionManagerPlugin(GrailsApplication application) {
		super(HibernateTransactionManagerPlugin.class, application);
	}

	public void doWithApplicationContext(ApplicationContext applicationContext) {
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;

	        if (!applicationContext.containsBeanDefinition("transactionManager")) {
	            RootBeanDefinition bd = new RootBeanDefinition(HibernateTransactionManager.class);
	            MutablePropertyValues mpv = new MutablePropertyValues();
	            mpv.addPropertyValue("sessionFactory", new RuntimeBeanReference("sessionFactory"));
	            bd.setPropertyValues(mpv);
	
	            ctx.registerBeanDefinition("transactionManager", bd);
	        }
    	}
    }

	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// do nothing
	}
}
