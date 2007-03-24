package org.codehaus.groovy.grails.plugins.services;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class ServicesGrailsPluginTests extends AbstractGrailsMockTests {

	void onSetUp() {
		gcl.parseClass(
"""
class TransactionalService {
	boolean transactional = true
    def serviceMethod() {
         return "hello"
    }			
}
class NonTransactionalService {
	boolean transactional = false
    def serviceMethod() {
		return "goodbye"
    }
} 
class ApplicationDataSource {
   boolean pooling = true
   String dbCreate = 'create-drop'
   String url = 'jdbc:hsqldb:mem:devDB'
   String driverClassName = 'org.hsqldb.jdbcDriver'
   String username = 'sa'
   String password = ''
}
""")
	}
	
	void testServicesPlugin() {
		
		def corePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
		def corePlugin = new DefaultGrailsPlugin(corePluginClass,ga)
		def dataSourcePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
		def dataSourcePlugin = new DefaultGrailsPlugin(dataSourcePluginClass, ga)
		def hibernatePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.orm.hibernate.HibernateGrailsPlugin")
		def hibernatePlugin = new DefaultGrailsPlugin(hibernatePluginClass, ga)
		
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()
		
		corePlugin.doWithRuntimeConfiguration(springConfig)
		dataSourcePlugin.doWithRuntimeConfiguration(springConfig)
		hibernatePlugin.doWithRuntimeConfiguration(springConfig)
		
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.services.ServicesGrailsPlugin")
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)	
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext()
		assert appCtx.containsBean("dataSource")
		assert appCtx.containsBean("sessionFactory")
		assert appCtx.containsBean("transactionalService")
		assert appCtx.containsBean("nonTransactionalService")
	}	
}