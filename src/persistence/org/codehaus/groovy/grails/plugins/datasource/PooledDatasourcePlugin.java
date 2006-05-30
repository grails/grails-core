package org.codehaus.groovy.grails.plugins.datasource;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDataSource;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.MutablePropertyValues;
import org.apache.commons.dbcp.BasicDataSource;

/**
 * <p>Register a pooled data source if the Grails application is configured accordingly.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class PooledDatasourcePlugin extends OrderedAdapter implements GrailsPlugin {
    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
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

            applicationContext.registerBeanDefinition("dataSource", bd);
        }
    }
}
