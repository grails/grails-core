package org.codehaus.groovy.grails.plugins.orm.hibernate;

import org.springframework.core.io.*
import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.springframework.context.ApplicationContext

class HibernateGrailsPluginTests extends AbstractGrailsMockTests {

    void onSetUp() {
        ExpandoMetaClass.enableGlobally()
        gcl.parseClass(
                """
                class Test {
                   Long id
                   Long version
                }
                """)

    }

    void testHibernatePluginWithDataSource() {
      def config = new ConfigSlurper().parse('''
            dataSource {
                pooled = true
                driverClassName = "org.hsqldb.jdbcDriver"
                username = "sa"
                password = ""
                dbCreate = "create-drop"
            }
''')

        ConfigurationHolder.config = config
        try {
            loadPluginCheckCanSaveDomainClass()
        }
        finally {

            ConfigurationHolder.config = null
        }
    }

    void testConfiguresHibernateWhenDataSourceInExternalSpringXml() {
        ctx.registerIgnoredClassPathLocation(GrailsRuntimeConfigurator.SPRING_RESOURCES_GROOVY)
        ctx.registerMockResource(GrailsRuntimeConfigurator.SPRING_RESOURCES_XML, """\
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN//EN" "http://www.springframework.org/dtd/spring-beans.dtd">
<beans>
    <bean name="dataSource"
        class="org.springframework.jdbc.datasource.DriverManagerDataSource">
        <property name="driverClassName" value="org.hsqldb.jdbcDriver"/>
        <property name="url" value="jdbc:hsqldb:mem:devDB"/>
        <property name="username" value="sa"/>
        <property name="password" value=""/>
    </bean>
</beans>
        """)

        loadPluginCheckCanSaveDomainClass();
        ctx.unregisterMockResource(GrailsRuntimeConfigurator.SPRING_RESOURCES_XML)
        ctx.unregisterIgnoredClassPathLocation(GrailsRuntimeConfigurator.SPRING_RESOURCES_GROOVY)
    }

    void testConfiguresHibernateWhenDataSourceInExternalSpringGroovy() {
        ctx.registerIgnoredClassPathLocation(GrailsRuntimeConfigurator.SPRING_RESOURCES_XML)
        ctx.registerMockResource(GrailsRuntimeConfigurator.SPRING_RESOURCES_GROOVY, """
          beans {
            dataSource(org.springframework.jdbc.datasource.DriverManagerDataSource) {
                driverClassName="org.hsqldb.jdbcDriver"
                url="jdbc:hsqldb:mem:devDB"
                username="sa"
                password=""
            }
        }
        """)

        loadPluginCheckCanSaveDomainClass();
        ctx.unregisterMockResource(GrailsRuntimeConfigurator.SPRING_RESOURCES_GROOVY)
        ctx.unregisterIgnoredClassPathLocation(GrailsRuntimeConfigurator.SPRING_RESOURCES_XML)
    }

    private loadPluginCheckCanSaveDomainClass() {
        gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
        gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
        gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
        gcl.loadClass("org.codehaus.groovy.grails.plugins.orm.hibernate.HibernateGrailsPlugin")

        def configurator = new GrailsRuntimeConfigurator(ga, ctx)
        def appCtx = configurator.configure(ctx.getServletContext())
        checkAppCtxContainsPluginBeans(appCtx)

        def testClass = ga.getDomainClass("Test").clazz

        def testObj = testClass.newInstance()
        testObj.save()
    }

    private void checkAppCtxContainsPluginBeans(appCtx){
        assert appCtx.containsBean("dataSource")
        assert appCtx.containsBean("sessionFactory")
        assert appCtx.containsBean("hibernateProperties")

        def props = appCtx.getBean("hibernateProperties")
        assertEquals "org.hibernate.dialect.HSQLDialect", props."hibernate.dialect"
        
        assert appCtx.containsBean("openSessionInViewInterceptor")
        assert appCtx.containsBean("TestValidator")
        assert appCtx.containsBean("persistenceInterceptor")
    }
}