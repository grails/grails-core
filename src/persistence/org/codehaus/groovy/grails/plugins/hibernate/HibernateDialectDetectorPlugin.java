package org.codehaus.groovy.grails.plugins.hibernate;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.support.HibernateDialectDetectorFactoryBean;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Properties;

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
public class HibernateDialectDetectorPlugin extends OrderedAdapter implements GrailsPlugin {
    private Properties vendorMappings;

    public void setVendorMappings(Properties vendorMappings) {
        this.vendorMappings = vendorMappings;
    }

    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
        RootBeanDefinition bd = new RootBeanDefinition(HibernateDialectDetectorFactoryBean.class);
        MutablePropertyValues mpv = new MutablePropertyValues();
        mpv.addPropertyValue("dataSource", new RuntimeBeanReference("dataSource"));
        mpv.addPropertyValue("vendorNameDialectMappings", vendorMappings);
        bd.setPropertyValues(mpv);

        applicationContext.registerBeanDefinition("dialectDetector", bd);
    }
}
