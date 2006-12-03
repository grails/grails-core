package org.codehaus.groovy.grails.orm.hibernate.plugins;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class HibernateGrailsPluginTests extends AbstractGrailsMockTests {

	void onSetUp() {
		gcl.parseClass(
"""
class Test {
   Long id
   Long version			
}
""")
	}
	
	void testHibernatePlugin() {
		
		def mockManager = new MockGrailsPluginManager()
		ctx.registerMockBean("manager", mockManager )
		
		def dependantPluginClasses = []
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")			
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.datasource.plugins.DataSourceGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.web.plugins.ControllersGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.i18n.plugins.I18nGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
		
		def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()		
		
		dependentPlugins*.doWithRuntimeConfiguration(springConfig)
		dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }

	
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.orm.hibernate.plugins.HibernateGrailsPlugin")		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		plugin.manager = mockManager
		
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext()
		assert appCtx.containsBean("dataSource")
		assert appCtx.containsBean("sessionFactory")
		assert appCtx.containsBean("openSessionInViewInterceptor")
		assert appCtx.containsBean("TestValidator")
		assert appCtx.containsBean("persistenceInterceptor")
		def testClass = ga.getGrailsDomainClass("Test").clazz
		
		def testObj = testClass.newInstance()
		testObj.save()
	}	
}