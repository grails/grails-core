package org.grails.plugins.services

import org.grails.commons.test.AbstractGrailsMockTests
import org.grails.plugins.DefaultGrailsPlugin
import org.grails.plugins.MockGrailsPluginManager
import org.grails.web.servlet.context.support.WebRuntimeSpringConfiguration
import org.springframework.context.ApplicationContext

class DataSourcesGrailsPluginTests  extends AbstractGrailsMockTests {

    void testSingleDataSource() {
        def appCtx = initializeContext([
                dataSource: [
                        pooled :true,
                        driverClassName:"org.h2.Driver",
                        username :"sa",
                        password :"",
                        dbCreate :"create-drop"
                ]
        ])
        assertTrue appCtx.containsBean("dataSource")
    }
    void testMultipleDataSources() {
        def appCtx = initializeContext([
                dataSource: [
                        pooled :true,
                        driverClassName:"org.h2.Driver",
                        username :"sa",
                        password :"",
                        dbCreate :"create-drop"
                ],
                dataSources: [
                        second:[
                                pooled :true,
                                driverClassName:"org.h2.Driver",
                                username :"sa",
                                password :"",
                                dbCreate :"create-drop"
                        ]
                ]
        ])
        assertTrue appCtx.containsBean("dataSource")
        assertTrue appCtx.containsBean("dataSource_second")
    }


    private ApplicationContext initializeContext(Map dataSources) {
        ga.getConfig().putAll(dataSources)
        def corePluginClass = gcl.loadClass("org.grails.plugins.CoreGrailsPlugin")
        def corePlugin = new DefaultGrailsPlugin(corePluginClass, ga)
        def dataSourcePluginClass = gcl.loadClass("org.grails.plugins.datasource.DataSourceGrailsPlugin")

        def dataSourcePlugin = new DefaultGrailsPlugin(dataSourcePluginClass, ga)
        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()

        corePlugin.doWithRuntimeConfiguration(springConfig)
        dataSourcePlugin.manager = new MockGrailsPluginManager(ga)
        dataSourcePlugin.doWithRuntimeConfiguration(springConfig)

        springConfig.getApplicationContext()
    }
}
