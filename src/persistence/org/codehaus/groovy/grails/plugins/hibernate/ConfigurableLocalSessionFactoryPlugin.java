package org.codehaus.groovy.grails.plugins.hibernate;

import java.util.Properties;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDataSource;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

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
public class ConfigurableLocalSessionFactoryPlugin extends AbstractGrailsPlugin implements GrailsPlugin {
    public ConfigurableLocalSessionFactoryPlugin(Class pluginClass, GrailsApplication application) {
		super(pluginClass, application);
	}

	public void doWithApplicationContext(ApplicationContext applicationContext) {
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;
		
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
	
	        ctx.registerBeanDefinition("sessionFactory", bd);
    	}
    }

	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		//  do nothing
	}
}
