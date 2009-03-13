package org.codehaus.groovy.grails.plugins.scaffolding;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.springframework.core.io.Resource

class ScaffoldingGrailsPluginTests extends AbstractGrailsMockTests {

	void onSetUp() {
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

    protected void onTearDown() {
        ConfigurationHolder.config = null
    }


	
	void testScaffoldingPlugin() {
		
		def mockManager = new MockGrailsPluginManager()
		ctx.registerMockBean("manager", mockManager )
        ctx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));
		
		def dependantPluginClasses = []
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")			
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.orm.hibernate.MockHibernateGrailsPlugin")
		
		def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}
		def springConfig = new WebRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()
		
		dependentPlugins.each{
			mockManager.registerMockPlugin(it) ; it.manager = mockManager 
		}
		dependentPlugins*.doWithRuntimeConfiguration(springConfig)

	
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.scaffolding.ScaffoldingGrailsPlugin")		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		plugin.manager = mockManager
		
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext()
		dependentPlugins*.doWithDynamicMethods(appCtx)		
		assert appCtx.containsBean("dataSource")
		assert appCtx.containsBean("sessionFactory")
		assert appCtx.containsBean("openSessionInViewInterceptor")
		assert appCtx.containsBean("TestValidator")

        // Check that the plugin does not blow up if a TagLib is modified,
        // as opposed to a controller.
        plugin.notifyOfEvent(
                DefaultGrailsPlugin.EVENT_ON_CHANGE,
                gcl.getLoadedClasses().find { it.name.endsWith("TagLib") })
	}	
}
