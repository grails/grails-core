package org.codehaus.groovy.grails.plugins.hibernate;

import java.util.Properties;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.support.HibernateDialectDetectorFactoryBean;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>Plugin that registers {@link HibernateDialectDetectorFactoryBean}.</p>
 *
 * <p>The mappings between database name as reported by JDBC's {@link java.sql.DatabaseMetaData}
 * and Hibernate dialect classes must be configured via the <code>vendorMappings</code> property
 * of this class.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class HibernateDialectDetectorPlugin extends AbstractGrailsPlugin implements GrailsPlugin {
    public HibernateDialectDetectorPlugin(Class pluginClass, GrailsApplication application) {
		super(pluginClass, application);
	}

	private Properties vendorMappings;

    public void setVendorMappings(Properties vendorMappings) {
        this.vendorMappings = vendorMappings;
    }

    public void doWithApplicationContext(ApplicationContext applicationContext) {
        RootBeanDefinition bd = new RootBeanDefinition(HibernateDialectDetectorFactoryBean.class);
        MutablePropertyValues mpv = new MutablePropertyValues();
        mpv.addPropertyValue("dataSource", new RuntimeBeanReference("dataSource"));
        mpv.addPropertyValue("vendorNameDialectMappings", vendorMappings);
        bd.setPropertyValues(mpv);

        if(applicationContext instanceof GenericApplicationContext)
        	((GenericApplicationContext)applicationContext).registerBeanDefinition("dialectDetector", bd);
    }

	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// do nothing
	}
}
