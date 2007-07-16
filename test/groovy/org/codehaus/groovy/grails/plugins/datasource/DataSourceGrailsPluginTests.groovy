package org.codehaus.groovy.grails.plugins.datasource;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class DataSourceGrailsPluginTests extends AbstractGrailsMockTests {

	void onSetUp() {
		def config = new ConfigSlurper("test").parse(
'''
dataSource {
	pooling = false                          
	driverClassName = "org.hsqldb.jdbcDriver"	
	username = "sa"
	password = ""				
}
environments {
	development {
		dataSource {
			dbCreate = "create-drop" 
			url = "jdbc:hsqldb:mem:devDB"
		}
	}   
	test {
		dataSource {
			dbCreate = "update"
			url = "jdbc:hsqldb:mem:testDb"
		}
	}   
	production {
		dataSource {
			dbCreate = "update"
			url = "jdbc:hsqldb:file:prodDb;shutdown=true"
		}
	}
}
''')
        ConfigurationHolder.setConfig(config)
	} 
	
	void onTearDown() {
		ConfigurationHolder.setConfig(null)
	}
	
	void testDataSourcePlugin() {
            def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")

            def plugin = new DefaultGrailsPlugin(pluginClass, ga)

            def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
            springConfig.servletContext = createMockServletContext()

            plugin.doWithRuntimeConfiguration(springConfig)

            def appCtx = springConfig.getApplicationContext()


            assert appCtx.containsBean("dataSource")
	}
}