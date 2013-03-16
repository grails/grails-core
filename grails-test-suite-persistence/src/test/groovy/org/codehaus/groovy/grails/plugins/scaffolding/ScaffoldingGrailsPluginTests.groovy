package org.codehaus.groovy.grails.plugins.scaffolding

import grails.util.GrailsUtil

import org.apache.commons.dbcp.BasicDataSource
import org.codehaus.groovy.grails.commons.AnnotationDomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests
import org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginMetaManager
import org.springframework.core.io.Resource
import org.springframework.jdbc.datasource.DataSourceTransactionManager

class ScaffoldingGrailsPluginTests extends AbstractGrailsMockTests {

    protected void onSetUp() {
        gcl.parseClass('''
            dataSource {
                pooled = true
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                dbCreate = "create-drop"
            }
''', "Config")

        gcl.parseClass(
"""
class Test {
   Long id
   Long version
}
class TestController {
    def scaffold = Test
}
class TestTagLib {
    def myTag = { attrs ->
        out << "Test"
    }
}
""")
    }

    void testScaffoldingPlugin() {

        def mockManager = new MockGrailsPluginManager()
        ctx.registerMockBean("pluginManager", mockManager)
        ctx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]))

        def dependantPluginClasses = []
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
        dependantPluginClasses << MockHibernateGrailsPlugin

        def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}
        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()

        dependentPlugins.each {
            mockManager.registerMockPlugin(it); it.manager = mockManager
        }
        dependentPlugins*.doWithRuntimeConfiguration(springConfig)

        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.scaffolding.ScaffoldingGrailsPlugin")
        def plugin = new DefaultGrailsPlugin(pluginClass, ga)
        plugin.manager = mockManager

        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()
        ga.mainContext = appCtx
        dependentPlugins*.doWithDynamicMethods(appCtx)
        assert appCtx.containsBean("dataSource")
        assert appCtx.containsBean("TestValidator")

        // Check that the plugin does not blow up if a TagLib is modified,
        // as opposed to a controller.
        plugin.notifyOfEvent(
            DefaultGrailsPlugin.EVENT_ON_CHANGE,
            gcl.getLoadedClasses().find { it.name.endsWith("TagLib") })
    }
}

class MockHibernateGrailsPlugin {

    def version = GrailsUtil.grailsVersion
    def dependsOn = [dataSource: version,
                     i18n: version,
                     core: version,
                     domainClass: version]

    def artefacts = [new AnnotationDomainClassArtefactHandler()]
    def loadAfter = ['controllers']
    def doWithSpring = {
        dataSource(BasicDataSource) {
            driverClassName = 'org.h2.Driver'
            url = 'jdbc:h2:mem:grailsDB;MVCC=TRUE;LOCK_TIMEOUT=10000'
            username = 'sa'
            password = ''
        }

        transactionManager(DataSourceTransactionManager, ref('dataSource'))
    }
}
