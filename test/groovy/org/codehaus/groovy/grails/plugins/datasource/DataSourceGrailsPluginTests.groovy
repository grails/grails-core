package org.codehaus.groovy.grails.plugins.datasource;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import grails.spring.BeanBuilder
import javax.sql.DataSource

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

	void testJndiDataSource() {
        def config = new ConfigSlurper().parse ('''
dataSource {
    jndiName = "java:comp/env/myDataSource"
}
        ''')


        def mock = [application:[config:config]]
        def plugin = new DataSourceGrailsPlugin()

        def beans = plugin.doWithSpring

        def bb = new BeanBuilder()
        bb.setBinding(new Binding(mock))
        bb.beans(beans)

        def beanDef = bb.getBeanDefinition('dataSource')

        assertEquals "org.springframework.jndi.JndiObjectFactoryBean",beanDef.beanClassName
        assertEquals "java:comp/env/myDataSource",beanDef.getPropertyValues().getPropertyValue('jndiName').getValue()
         assertEquals DataSource,beanDef.getPropertyValues().getPropertyValue('expectedType').getValue()

    }
}