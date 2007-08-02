package org.codehaus.groovy.grails.plugins.orm.hibernate;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

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
        gcl.parseClass(
                """
                dataSource {
                    pooled = false
                    driverClassName = "org.hsqldb.jdbcDriver"
                    username = "sa"
                    password = ""
                    dbCreate = "create-drop" // one of 'create', 'create-drop','update'
                    url = "jdbc:hsqldb:mem:devDB"

                }
                """, "DataSource")
           loadPluginCheckCanSaveDomainClass();
    }

    void testConfiguresHibernateWhenDataSourceInExternalSpringXml(){
       
        loadPluginCheckCanSaveDomainClass();
    }

    void testConfiguresHibernateWhenDataSourceInExternalSpringGroovyl(){


        loadPluginCheckCanSaveDomainClass();
    }

    private loadPluginCheckCanSaveDomainClass(){
           def mockManager = new MockGrailsPluginManager()
        ctx.registerMockBean("manager", mockManager)

        def dependantPluginClasses = []
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")

        def dependentPlugins = dependantPluginClasses.collect {new DefaultGrailsPlugin(it, ga)}
        def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()

        dependentPlugins*.doWithRuntimeConfiguration(springConfig)
        dependentPlugins.each {mockManager.registerMockPlugin(it); it.manager = mockManager}


        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.orm.hibernate.HibernateGrailsPlugin")
        def plugin = new DefaultGrailsPlugin(pluginClass, ga)
        plugin.manager = mockManager

        plugin.doWithRuntimeConfiguration(springConfig)


        def appCtx = springConfig.getApplicationContext()
        assert appCtx.containsBean("dataSource")
        assert appCtx.containsBean("sessionFactory")
        assert appCtx.containsBean("openSessionInViewInterceptor")
        assert appCtx.containsBean("TestValidator")
        assert appCtx.containsBean("persistenceInterceptor")
        dependentPlugins*.doWithDynamicMethods(appCtx)
        plugin.doWithDynamicMethods(appCtx)

        def testClass = ga.getDomainClass("Test").clazz

        def testObj = testClass.newInstance()
        testObj.save()

    }
}