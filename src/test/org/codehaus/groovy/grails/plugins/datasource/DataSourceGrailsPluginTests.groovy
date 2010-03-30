package org.codehaus.groovy.grails.plugins.datasource;

import grails.spring.BeanBuilder

import javax.sql.DataSource

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

class DataSourceGrailsPluginTests extends AbstractGrailsMockTests {

    void testDataSourceWithEncryptedPassword() {
        def encryptedPassword = MockCodec.encode("")
        def config = new ConfigSlurper().parse("""
            dataSource {
                pooled = true
                driverClassName = "org.hsqldb.jdbcDriver"
                url="jdbc:hsqldb:mem:devDB"
                username = "sa"
                password = "$encryptedPassword"
                passwordEncryptionCodec = 'org.codehaus.groovy.grails.plugins.datasource.MockCodec'
                dbCreate = "create-drop"
            }
""")

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

        def config = new ConfigSlurper("test").parse('''
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

        assertEquals "org.apache.commons.dbcp.BasicDataSource", beanDef.beanClassName

        assertNotNull beanDef.propertyValues.getPropertyValue('username')
        assertNotNull beanDef.propertyValues.getPropertyValue('password')
        assertEquals "foo", beanDef.propertyValues.getPropertyValue('username').value
        assertEquals "blah", beanDef.propertyValues.getPropertyValue('password').value

        assertEquals "com.oracle.jdbcDriver", beanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "jdbc:oracle::someserver", beanDef.propertyValues.getPropertyValue('url').value
    }

    void testDataSourcePluginOtherDriverNoUserAndPass() {

        def config = new ConfigSlurper("test").parse('''
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

        assertEquals "org.apache.commons.dbcp.BasicDataSource", beanDef.beanClassName

        assertNull beanDef.propertyValues.getPropertyValue('username')
        assertNull beanDef.propertyValues.getPropertyValue('password')
        assertEquals "com.oracle.jdbcDriver", beanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "jdbc:oracle::someserver", beanDef.propertyValues.getPropertyValue('url').value
    }

    void testDataSourcePluginHSQLDBNoUserAndPass() {

        def config = new ConfigSlurper("test").parse('''
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

        assertEquals "org.apache.commons.dbcp.BasicDataSource", beanDef.beanClassName

        assertEquals "org.hsqldb.jdbcDriver", beanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", beanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", beanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:hsqldb:mem:grailsDB", beanDef.propertyValues.getPropertyValue('url').value
    }

    void testDataSourcePluginPoolingOn() {

        def config = new ConfigSlurper("test").parse('''
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

        assertEquals "org.apache.commons.dbcp.BasicDataSource", beanDef.beanClassName

        assertEquals "org.hsqldb.jdbcDriver", beanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", beanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", beanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:hsqldb:mem:testDb", beanDef.propertyValues.getPropertyValue('url').value
    }

    void testDataSourcePluginPoolingOff() {

        def config = new ConfigSlurper("test").parse('''
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

        assertEquals "org.springframework.jdbc.datasource.DriverManagerDataSource", beanDef.beanClassName

        assertEquals "org.hsqldb.jdbcDriver", beanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", beanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", beanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:hsqldb:mem:testDb", beanDef.propertyValues.getPropertyValue('url').value
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

        assertEquals "org.springframework.jndi.JndiObjectFactoryBean", beanDef.beanClassName
        assertEquals "java:comp/env/myDataSource", beanDef.propertyValues.getPropertyValue('jndiName').value
        assertEquals DataSource, beanDef.propertyValues.getPropertyValue('expectedType').value
    }
}

class MockCodec {

    static encode(theTarget) {
        "3003923982734o3273"
    }

    static decode(theTarget) {
        ""
    }
}
