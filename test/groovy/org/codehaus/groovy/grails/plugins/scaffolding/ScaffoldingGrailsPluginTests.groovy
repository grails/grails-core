package org.codehaus.groovy.grails.plugins.scaffolding;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class ScaffoldingPluginTests extends AbstractGrailsMockTests {

	void onSetUp() {
        gcl.parseClass("dataSource {\n" +
                "dbCreate = \"create-drop\" \n" +
                "url = \"jdbc:hsqldb:mem:devDB\"\n" +
                "pooling = false                          \n" +
                "driverClassName = \"org.hsqldb.jdbcDriver\"\t\n" +
                "username = \"sa\"\n" +
                "password = \"\"\n" +
                "}", "DataSource")
        gcl.parseClass(
"""
class Test {
   Long id
   Long version			
}
class TestController {
	def scaffold = Test
}
""")
	}
	
	void testScaffoldingPlugin() {
		
		def mockManager = new MockGrailsPluginManager()
		ctx.registerMockBean("manager", mockManager )
		
		def dependantPluginClasses = []
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")			
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.orm.hibernate.HibernateGrailsPlugin")
		
		def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
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
		assert appCtx.containsBean("TestDomain")
		assert appCtx.containsBean("TestControllerScaffolder")
		
		assertNotNull(appCtx.getBean("TestControllerScaffolder").scaffoldRequestHandler)
		
		def testClass = ga.getDomainClass("Test").clazz
		
		def testObj = testClass.newInstance()
		testObj.save()
	}	
}