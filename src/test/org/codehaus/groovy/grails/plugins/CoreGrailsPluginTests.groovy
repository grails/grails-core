package org.codehaus.groovy.grails.plugins;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*

import grails.spring.BeanBuilder

class CoreGrailsPluginTests extends AbstractGrailsMockTests {

	
	void testCorePlugin() {
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		
		def springConfig = new WebRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()
		
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext()
		
		assert appCtx.containsBean("classLoader")
		assert appCtx.containsBean("customEditors")
	}
	
	void onSetUp() {
		// needed for testBeanPropertyOverride
		gcl.parseClass("""
			class SomeTransactionalService {
				boolean transactional = true
				Integer i
			}
			class NonTransactionalService {
				boolean transactional = false
				Integer i
			}
		""")
	}
	
	/**
	 * Tests the ability to set bean properties via the application config
	 * 
	 * @author Luke Daley
	 */
	void testBeanPropertyOverride() {
		ConfigurationHolder.config = new ConfigSlurper().parse('''
			dataSource {
				pooled = true
				driverClassName = "org.hsqldb.jdbcDriver"
				username = "sa"
				password = ""
				dbCreate = "create-drop"
			}
			beans {
				someTransactionalService {
					i = 1
				}
				nonTransactionalService {
					i = 2
				}
			}
		''')
		
		def corePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
		def corePlugin = new DefaultGrailsPlugin(corePluginClass,ga)
		def dataSourcePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
		def dataSourcePlugin = new DefaultGrailsPlugin(dataSourcePluginClass, ga)
		def hibernatePluginClass = gcl.loadClass("org.codehaus.groovy.grails.orm.hibernate.MockHibernateGrailsPlugin")
		def hibernatePlugin = new DefaultGrailsPlugin(hibernatePluginClass, ga)

		def springConfig = new WebRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()

		corePlugin.doWithRuntimeConfiguration(springConfig)
		dataSourcePlugin.doWithRuntimeConfiguration(springConfig)
		hibernatePlugin.doWithRuntimeConfiguration(springConfig)

		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.services.ServicesGrailsPlugin")
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		plugin.doWithRuntimeConfiguration(springConfig)

		def appCtx = springConfig.getApplicationContext()
		
		assertEquals(1, appCtx.getBean('someTransactionalService').i)
		assertEquals(2, appCtx.getBean('nonTransactionalService').i)
	}
}