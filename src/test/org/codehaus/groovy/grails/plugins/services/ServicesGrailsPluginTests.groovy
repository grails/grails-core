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
import org.springframework.transaction.annotation.*
import org.springframework.transaction.interceptor.TransactionAspectSupport
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

@Transactional(readOnly = true)
class SpringTransactionalService {
    def serviceMethod() {
        def status = TransactionAspectSupport.currentTransactionStatus()
        return "hasTransaction = \${status!=null}"
    }
}
""")
	}

    protected void onTearDown() {
        ConfigurationHolder.config = null
    }

    void testSpringConfiguredService() {
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

        
        def springService = appCtx.getBean("springTransactionalService")
        assertEquals "hasTransaction = true", springService.serviceMethod()
    }

	void testServicesPlugin() {
		
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
		assert appCtx.containsBean("dataSource")
		assert appCtx.containsBean("sessionFactory")
		assert appCtx.containsBean("someTransactionalService")
		assert appCtx.containsBean("nonTransactionalService")
	}	
}