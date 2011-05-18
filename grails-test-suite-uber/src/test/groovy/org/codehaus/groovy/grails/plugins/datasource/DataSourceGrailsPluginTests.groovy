package org.codehaus.groovy.grails.plugins.datasource;

import grails.spring.BeanBuilder

import javax.sql.DataSource

import org.apache.commons.dbcp.BasicDataSource
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.jndi.JndiObjectFactoryBean
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

class DataSourceGrailsPluginTests extends AbstractGrailsMockTests {



    @Override protected void onSetUp() {
        PluginManagerHolder.setPluginManager(null)
    }

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

        ga.config = config
        gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")

        def configurator = new GrailsRuntimeConfigurator(ga, ctx)
        def appCtx = configurator.configure(ctx.getServletContext())

        DataSource dataSource = appCtx.getBean("dataSource")
        assert dataSource

        configurator.pluginManager.shutdown()

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

        ga.config = config
        gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")

        def configurator = new GrailsRuntimeConfigurator(ga, ctx)
        def appCtx = configurator.configure(ctx.getServletContext())

        DataSource dataSource = appCtx.getBean("dataSource")
        assert dataSource

        configurator.pluginManager.shutdown()
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
        def parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals BasicDataSource.name, beanDef.beanClassName

        assertNotNull parentBeanDef.propertyValues.getPropertyValue('username')
        assertNotNull parentBeanDef.propertyValues.getPropertyValue('password')
        assertEquals "foo", parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals "blah", parentBeanDef.propertyValues.getPropertyValue('password').value

        assertEquals "com.oracle.jdbcDriver", parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "jdbc:oracle::someserver", parentBeanDef.propertyValues.getPropertyValue('url').value
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

        def parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean')
        def beanDef = bb.getBeanDefinition('dataSource')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals BasicDataSource.name, beanDef.beanClassName

        assert beanDef.parentName == 'abstractGrailsDataSourceBean'
        assertNull beanDef.propertyValues.getPropertyValue('username')
        assertNull beanDef.propertyValues.getPropertyValue('password')
        assertEquals "com.oracle.jdbcDriver", parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "jdbc:oracle::someserver", parentBeanDef.propertyValues.getPropertyValue('url').value
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
        def parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals BasicDataSource.name, beanDef.beanClassName

        assert beanDef.parentName == 'abstractGrailsDataSourceBean'
        assertEquals "org.h2.Driver", parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", parentBeanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:h2:mem:grailsDB", parentBeanDef.propertyValues.getPropertyValue('url').value
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
        def parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals BasicDataSource.name, beanDef.beanClassName

        assertEquals "org.h2.Driver", parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", parentBeanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:h2:mem:testDb", parentBeanDef.propertyValues.getPropertyValue('url').value
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
        def parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals DriverManagerDataSource.name, beanDef.beanClassName

        assertEquals "org.h2.Driver", parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", parentBeanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:h2:mem:testDb", parentBeanDef.propertyValues.getPropertyValue('url').value
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
