package org.codehaus.groovy.grails.plugins.datasource;

import grails.spring.BeanBuilder

import javax.sql.DataSource

import org.apache.commons.dbcp.BasicDataSource
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.jndi.JndiObjectFactoryBean

class DataSourceGrailsPluginTests extends AbstractGrailsMockTests {

    void testDataSourceWithEncryptedPassword() {
        def encryptedPassword = MockCodec.encode("")
        def config = new ConfigSlurper().parse("""
            dataSource {
                pooled = true
                driverClassName = "org.h2.Driver"
                url="jdbc:h2:mem:devDB"
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

    void testShutdownH2DbDataSource() {
        def config = new ConfigSlurper().parse('''
                dataSource {
                    pooled = true
                    driverClassName = "org.h2.Driver"
                    url="jdbc:h2:mem:devDB"
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
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName
        
        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals BasicDataSource.name, beanDef.beanClassName

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
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals BasicDataSource.name, beanDef.beanClassName

        assertNull beanDef.propertyValues.getPropertyValue('username')
        assertNull beanDef.propertyValues.getPropertyValue('password')
        assertEquals "com.oracle.jdbcDriver", beanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "jdbc:oracle::someserver", beanDef.propertyValues.getPropertyValue('url').value
    }

    void testDataSourcePluginH2DBNoUserAndPass() {

        def config = new ConfigSlurper("test").parse('''
                dataSource {
                    pooled = true
                    driverClassName = "org.h2.Driver"
                }
        ''')

        def mock = [application:[config:config]]
        def plugin = new DataSourceGrailsPlugin()

        def beans = plugin.doWithSpring

        def bb = new BeanBuilder()
        bb.setBinding(new Binding(mock))
        bb.beans(beans)

        def beanDef = bb.getBeanDefinition('dataSource')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals BasicDataSource.name, beanDef.beanClassName

        assertEquals "org.h2.Driver", beanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", beanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", beanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:h2:mem:grailsDB", beanDef.propertyValues.getPropertyValue('url').value
    }

    void testDataSourcePluginPoolingOn() {

        def config = new ConfigSlurper("test").parse('''
                dataSource {
                    pooled = true
                    driverClassName = "org.h2.Driver"
                    username = "sa"
                    password = ""
                }
                environments {
                    development {
                        dataSource {
                            dbCreate = "create-drop"
                            url = "jdbc:h2:mem:devDB"
                        }
                    }
                    test {
                        dataSource {
                            dbCreate = "update"
                            url = "jdbc:h2:mem:testDb"
                        }
                    }
                    production {
                        dataSource {
                            dbCreate = "update"
                            url = "jdbc:h2:file:prodDb;shutdown=true"
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
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals BasicDataSource.name, beanDef.beanClassName

        assertEquals "org.h2.Driver", beanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", beanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", beanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:h2:mem:testDb", beanDef.propertyValues.getPropertyValue('url').value
    }

    void testDataSourcePluginPoolingOff() {

        def config = new ConfigSlurper("test").parse('''
                dataSource {
                    pooled = false
                    driverClassName = "org.h2.Driver"
                    username = "sa"
                    password = ""
                }
                environments {
                    development {
                        dataSource {
                            dbCreate = "create-drop"
                            url = "jdbc:h2:mem:devDB"
                        }
                    }
                    test {
                        dataSource {
                            dbCreate = "update"
                            url = "jdbc:h2:mem:testDb"
                        }
                    }
                    production {
                        dataSource {
                            dbCreate = "update"
                            url = "jdbc:h2:prodDb"
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
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals DriverManagerDataSource.name, beanDef.beanClassName

        assertEquals "org.h2.Driver", beanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", beanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", beanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:h2:mem:testDb", beanDef.propertyValues.getPropertyValue('url').value
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
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals JndiObjectFactoryBean.name, beanDef.beanClassName

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
