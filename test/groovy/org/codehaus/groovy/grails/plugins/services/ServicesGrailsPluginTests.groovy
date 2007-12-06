package org.codehaus.groovy.grails.plugins.services;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class ServicesGrailsPluginTests extends AbstractGrailsMockTests {

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
class SomeTransactionalService {
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
""")
	}

    protected void onTearDown() {
        ConfigurationHolder.config = null
    }


	void testServicesPlugin() {
		
		def corePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
		def corePlugin = new DefaultGrailsPlugin(corePluginClass,ga)
		def dataSourcePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
		def dataSourcePlugin = new DefaultGrailsPlugin(dataSourcePluginClass, ga)
		def hibernatePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.orm.hibernate.HibernateGrailsPlugin")
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
		assert appCtx.containsBean("dataSource")
		assert appCtx.containsBean("sessionFactory")
		assert appCtx.containsBean("someTransactionalService")
		assert appCtx.containsBean("nonTransactionalService")
	}	
}