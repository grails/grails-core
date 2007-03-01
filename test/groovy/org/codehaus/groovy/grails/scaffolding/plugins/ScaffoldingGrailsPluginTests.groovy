package org.codehaus.groovy.grails.scaffolding.plugins;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class ScaffoldingPluginTests extends AbstractGrailsMockTests {

	void onSetUp() {
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
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.datasource.plugins.DataSourceGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.web.plugins.ControllersGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.i18n.plugins.I18nGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.orm.hibernate.plugins.HibernateGrailsPlugin")
		
		def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()
		
		dependentPlugins.each{
			mockManager.registerMockPlugin(it) ; it.manager = mockManager 
		}
		dependentPlugins*.doWithRuntimeConfiguration(springConfig)

	
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.scaffolding.plugins.ScaffoldingGrailsPlugin")		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		plugin.manager = mockManager
		
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext() 
        mockManager.getGrailsPlugin("hibernate").doWithDynamicMethods(appCtx)		
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