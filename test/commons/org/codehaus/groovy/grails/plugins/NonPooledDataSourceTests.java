package org.codehaus.groovy.grails.plugins;

import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class NonPooledDataSourceTests extends TestCase {
    GenericApplicationContext appCtx;

    protected void setUp() throws Exception {
        appCtx = new GenericApplicationContext();

        Resource[] resources = new Resource[1];
        resources[0] = new ClassPathResource("org/codehaus/groovy/grails/plugins/grails-app/conf/NonPooledApplicationDataSource.groovy");

        GrailsApplication application = new DefaultGrailsApplication(resources);
        GrailsPluginLoader.loadPlugins(appCtx, application, "classpath*:org/codehaus/groovy/grails/plugins/*.xml");

        appCtx.refresh();
    }

    public void testPooledDataSourcePresent() {
        DriverManagerDataSource dataSource = (DriverManagerDataSource) appCtx.getBean("dataSource", DriverManagerDataSource.class);
    }
}
