package org.codehaus.groovy.grails.plugins.datasource;

import org.apache.commons.dbcp.BasicDataSource;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDataSource;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>Register a pooled data source if the Grails application is configured accordingly.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class PooledDatasourcePlugin extends AbstractGrailsPlugin implements GrailsPlugin {
    public PooledDatasourcePlugin(GrailsApplication application) {
		super(PooledDatasourcePlugin.class, application);
	}

	public void doWithApplicationContext(ApplicationContext applicationContext) {
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;
	
	        GrailsDataSource dataSource = application.getGrailsDataSource();
	
	        if (dataSource != null && dataSource.isPooled()) {
	            RootBeanDefinition bd = new RootBeanDefinition(BasicDataSource.class);
	            MutablePropertyValues mpv = new MutablePropertyValues();
	            mpv.addPropertyValue("driverClassName", dataSource.getDriverClassName());
	            mpv.addPropertyValue("url", dataSource.getUrl());
	            mpv.addPropertyValue("username", dataSource.getUsername());
	            mpv.addPropertyValue("password", dataSource.getPassword());
	            bd.setPropertyValues(mpv);
	            bd.setDestroyMethodName("close");
	
	            ctx.registerBeanDefinition("dataSource", bd);
	        }
    	}
    }

	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// do nothing
		
	}
}
