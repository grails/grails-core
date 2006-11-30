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
		
		def corePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
		def corePlugin = new DefaultGrailsPlugin(corePluginClass,ga)
		def dataSourcePluginClass = gcl.loadClass("org.codehaus.groovy.grails.datasource.plugins.DataSourceGrailsPlugin")
		def dataSourcePlugin = new DefaultGrailsPlugin(dataSourcePluginClass, ga)

		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()
		
		corePlugin.doWithRuntimeConfiguration(springConfig)
		dataSourcePlugin.doWithRuntimeConfiguration(springConfig)
		
		
		
		
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.orm.hibernate.plugins.HibernateGrailsPlugin")
		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		
		
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext()
		assert appCtx.containsBean("dataSource")
		assert appCtx.containsBean("sessionFactory")
		
		def testClass = ga.getGrailsDomainClass("Test").clazz
		
		def testObj = testClass.newInstance()
		testObj.save()
	}	
}