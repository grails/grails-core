package org.codehaus.groovy.grails.plugins.hibernate;

import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDataSource;
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

/**
 * <p>Plugin that registers {@link ConfigurableLocalSessionFactoryBean}.</p>
 *
 * <p>This plugin will use <code>hibernate.cfg.xml</code> if it's available
 * on the root of the classpath. If this file is loaded it's possible to use
 * Java and Grails domain classes combined.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class ConfigurableLocalSessionFactoryPlugin extends OrderedAdapter implements GrailsPlugin {
    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
        GrailsDataSource dataSource = application.getGrailsDataSource();

        RootBeanDefinition bd = new RootBeanDefinition(ConfigurableLocalSessionFactoryBean.class);
        MutablePropertyValues mpv = new MutablePropertyValues();
        mpv.addPropertyValue("dataSource", new RuntimeBeanReference("dataSource"));
        Properties hibernateProperties = new Properties();
        if (dataSource != null && dataSource.getDbCreate() != null) {
            hibernateProperties.setProperty("hibernate.hbm2ddl.auto", dataSource.getDbCreate());
        }
        mpv.addPropertyValue("hibernateProperties", hibernateProperties);
        Resource hibernateConfigFile = new ClassPathResource("hibernate.cfg.xml");
        if (hibernateConfigFile.exists()) {
            mpv.addPropertyValue("configLocation", "classpath:hibernate.cfg.xml");
        }
        bd.setPropertyValues(mpv);

        applicationContext.registerBeanDefinition("sessionFactory", bd);
    }
}
