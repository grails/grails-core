package org.codehaus.groovy.grails.plugins.datasource;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import grails.spring.BeanBuilder
import javax.sql.DataSource

class DataSourceGrailsPluginTests extends AbstractGrailsMockTests {

    void testShutdownHSqlDbDataSource() {
          def config = new ConfigSlurper().parse('''
                dataSource {
                    pooled = true
                    driverClassName = "org.hsqldb.jdbcDriver"
                    url="jdbc:hsqldb:mem:devDB"
                    username = "sa"
                    password = ""
                    dbCreate = "create-drop"
                }
    ''')

            ConfigurationHolder.config = config
        gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")

        def configurator = new GrailsRuntimeConfigurator(ga, ctx)
        def appCtx = configurator.configure(ctx.getServletContext())

        DataSource dataSource = appCtx.getBean("dataSource")
        assert dataSource                       

        def pluginManager = PluginManagerHolder.currentPluginManager()

        pluginManager.shutdown()
    }

    void testDataSourcePluginOtherDriverWithUserAndPass() {

            def config = new ConfigSlurper("test").parse(
            '''
            dataSource {
                pooled = true
                driverClassName = "com.oracle.jdbcDriver"
                url = "jdbc:oracle::someserver"
                username = "foo"
                password = "blah"
            }
            ''')

        def mock = [application:[config:config]]
        def plugin = new DataSourceGrailsPlugin()

        def beans = plugin.doWithSpring

        def bb = new BeanBuilder()
        bb.setBinding(new Binding(mock))
        bb.beans(beans)

        def beanDef = bb.getBeanDefinition('dataSource')

        assertEquals "org.apache.commons.dbcp.BasicDataSource",beanDef.beanClassName

        assertNotNull beanDef.getPropertyValues().getPropertyValue('username')
        assertNotNull  beanDef.getPropertyValues().getPropertyValue('password')
        assertEquals  "foo",beanDef.getPropertyValues().getPropertyValue('username').getValue()
        assertEquals  "blah",beanDef.getPropertyValues().getPropertyValue('password').getValue()

        assertEquals  "com.oracle.jdbcDriver",beanDef.getPropertyValues().getPropertyValue('driverClassName').getValue()
        assertEquals  "jdbc:oracle::someserver",beanDef.getPropertyValues().getPropertyValue('url').getValue()
	}

    void testDataSourcePluginOtherDriverNoUserAndPass() {

            def config = new ConfigSlurper("test").parse(
            '''
            dataSource {
                pooled = true
                driverClassName = "com.oracle.jdbcDriver"
                url = "jdbc:oracle::someserver"

            }
            ''')

        def mock = [application:[config:config]]
        def plugin = new DataSourceGrailsPlugin()

        def beans = plugin.doWithSpring

        def bb = new BeanBuilder()
        bb.setBinding(new Binding(mock))
        bb.beans(beans)

        def beanDef = bb.getBeanDefinition('dataSource')

        assertEquals "org.apache.commons.dbcp.BasicDataSource",beanDef.beanClassName

        assertNull beanDef.getPropertyValues().getPropertyValue('username')
        assertNull  beanDef.getPropertyValues().getPropertyValue('password')
        assertEquals  "com.oracle.jdbcDriver",beanDef.getPropertyValues().getPropertyValue('driverClassName').getValue()
        assertEquals  "jdbc:oracle::someserver",beanDef.getPropertyValues().getPropertyValue('url').getValue()
	}

    void testDataSourcePluginHSQLDBNoUserAndPass() {

            def config = new ConfigSlurper("test").parse(
            '''
            dataSource {
                pooled = true
                driverClassName = "org.hsqldb.jdbcDriver"
            }
            ''')

        def mock = [application:[config:config]]
        def plugin = new DataSourceGrailsPlugin()

        def beans = plugin.doWithSpring

        def bb = new BeanBuilder()
        bb.setBinding(new Binding(mock))
        bb.beans(beans)

        def beanDef = bb.getBeanDefinition('dataSource')

        assertEquals "org.apache.commons.dbcp.BasicDataSource",beanDef.beanClassName

        assertEquals  "org.hsqldb.jdbcDriver",beanDef.getPropertyValues().getPropertyValue('driverClassName').getValue()
        assertEquals  "sa",beanDef.getPropertyValues().getPropertyValue('username').getValue()
        assertEquals  "",beanDef.getPropertyValues().getPropertyValue('password').getValue()
        assertEquals  "jdbc:hsqldb:mem:grailsDB",beanDef.getPropertyValues().getPropertyValue('url').getValue()
	}

	void testDataSourcePluginPoolingOn() {

            def config = new ConfigSlurper("test").parse(
            '''
            dataSource {
                pooled = true
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

        def mock = [application:[config:config]]
        def plugin = new DataSourceGrailsPlugin()

        def beans = plugin.doWithSpring

        def bb = new BeanBuilder()
        bb.setBinding(new Binding(mock))
        bb.beans(beans)

        def beanDef = bb.getBeanDefinition('dataSource')

        assertEquals "org.apache.commons.dbcp.BasicDataSource",beanDef.beanClassName

        assertEquals  "org.hsqldb.jdbcDriver",beanDef.getPropertyValues().getPropertyValue('driverClassName').getValue()
        assertEquals  "sa",beanDef.getPropertyValues().getPropertyValue('username').getValue()
        assertEquals  "",beanDef.getPropertyValues().getPropertyValue('password').getValue()
        assertEquals  "jdbc:hsqldb:mem:testDb",beanDef.getPropertyValues().getPropertyValue('url').getValue()


	}

	void testDataSourcePluginPoolingOff() {

            def config = new ConfigSlurper("test").parse(
            '''
            dataSource {
                pooled = false
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

        def mock = [application:[config:config]]
        def plugin = new DataSourceGrailsPlugin()

        def beans = plugin.doWithSpring

        def bb = new BeanBuilder()
        bb.setBinding(new Binding(mock))
        bb.beans(beans)

        def beanDef = bb.getBeanDefinition('dataSource')

        assertEquals "org.springframework.jdbc.datasource.DriverManagerDataSource",beanDef.beanClassName

        assertEquals  "org.hsqldb.jdbcDriver",beanDef.getPropertyValues().getPropertyValue('driverClassName').getValue()
        assertEquals  "sa",beanDef.getPropertyValues().getPropertyValue('username').getValue()
        assertEquals  "",beanDef.getPropertyValues().getPropertyValue('password').getValue()
        assertEquals  "jdbc:hsqldb:mem:testDb",beanDef.getPropertyValues().getPropertyValue('url').getValue()
        

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