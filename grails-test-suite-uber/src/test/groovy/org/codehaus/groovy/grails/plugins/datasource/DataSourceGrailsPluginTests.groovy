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

    private configurator

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

        def appCtx = createAppCtx(config)

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

        def appCtx = createAppCtx(config)

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

        def bb = createBeanBuilder(config)

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

        def bb = createBeanBuilder(config)

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

        def bb = createBeanBuilder(config)

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

        def bb = createBeanBuilder(config)

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

        def bb = createBeanBuilder(config)

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

        def bb = createBeanBuilder(config)

        def beanDef = bb.getBeanDefinition('dataSource')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals JndiObjectFactoryBean.name, beanDef.beanClassName

        assertEquals "java:comp/env/myDataSource", beanDef.propertyValues.getPropertyValue('jndiName').value
        assertEquals DataSource, beanDef.propertyValues.getPropertyValue('expectedType').value
    }

    void testMultipleDataSources() {

        def config = new ConfigSlurper().parse('''
              dataSource {
                    pooled = true
                    driverClassName = "com.oracle.jdbcDriver"
                    url = "jdbc:oracle::someserver"
                    username = "foo"
                    password = "blah"
              }
              dataSource_ds2 {
                    driverClassName = "org.h2.Driver"
                    url = "jdbc:h2:mem:testDb2"
                    username = "user"
                    password = "pass"
                    pooled = false
                    dbCreate = "update"
                    readOnly = true
              }
              dataSource_ds3 {
                    driverClassName = "org.h2.Driver"
                    url = "jdbc:h2:mem:testDb3"
                    username = "sa"
                    password = ""
                    dbCreate = "create-drop"
                    readOnly = true
              }
              dataSource_ds4 {
                    driverClassName = "org.h2.Driver"
                    url = "jdbc:h2:mem:testDb4"
              }
              dataSource_jndi {
                    jndiName = "java:comp/env/myDataSource"
              }
        ''')

        def bb = createBeanBuilder(config)

        // default
        def beanDef = bb.getBeanDefinition('dataSource')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals BasicDataSource.name, beanDef.beanClassName

        def parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean')

        assertEquals "foo", parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals "blah", parentBeanDef.propertyValues.getPropertyValue('password').value
        assertFalse parentBeanDef.propertyValues.getPropertyValue('defaultReadOnly').value

        assertEquals "com.oracle.jdbcDriver", parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "jdbc:oracle::someserver", parentBeanDef.propertyValues.getPropertyValue('url').value

        // ds2
        beanDef = bb.getBeanDefinition('dataSource_ds2')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied_ds2')
        assertEquals 'pooled false, readOnly true', ReadOnlyDriverManagerDataSource.name, beanDef.beanClassName

        parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean_ds2')

        assertEquals 'user', parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals 'pass', parentBeanDef.propertyValues.getPropertyValue('password').value
        assertNull 'not a BasicDataSource', parentBeanDef.propertyValues.getPropertyValue('defaultReadOnly')

        assertEquals 'org.h2.Driver', parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals 'jdbc:h2:mem:testDb2', parentBeanDef.propertyValues.getPropertyValue('url').value

        // ds3
        beanDef = bb.getBeanDefinition('dataSource_ds3')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied_ds3')
        assertEquals 'pooled default true, readOnly true', BasicDataSource.name, beanDef.beanClassName

        parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean_ds3')

        assertEquals 'sa', parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals '', parentBeanDef.propertyValues.getPropertyValue('password').value
        assertTrue parentBeanDef.propertyValues.getPropertyValue('defaultReadOnly').value

        assertEquals 'org.h2.Driver', parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals 'jdbc:h2:mem:testDb3', parentBeanDef.propertyValues.getPropertyValue('url').value

        // ds4
        beanDef = bb.getBeanDefinition('dataSource_ds4')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied_ds4')
        assertEquals BasicDataSource.name, beanDef.beanClassName

        parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean_ds4')

        assertEquals 'sa', parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals '', parentBeanDef.propertyValues.getPropertyValue('password').value

        assertEquals 'org.h2.Driver', parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals 'jdbc:h2:mem:testDb4', parentBeanDef.propertyValues.getPropertyValue('url').value
        assertFalse parentBeanDef.propertyValues.getPropertyValue('defaultReadOnly').value

        // jndi
        beanDef = bb.getBeanDefinition('dataSource_jndi')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied_jndi')
        assertEquals JndiObjectFactoryBean.name, beanDef.beanClassName

        assertEquals "java:comp/env/myDataSource", beanDef.propertyValues.getPropertyValue('jndiName').value
        assertEquals DataSource, beanDef.propertyValues.getPropertyValue('expectedType').value
    }

    private createAppCtx(config) {

        ga.config = config
        gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")

        configurator = new GrailsRuntimeConfigurator(ga, ctx)
        configurator.configure(ctx.getServletContext())
    }

    private BeanBuilder createBeanBuilder(config) {
        def bb = new BeanBuilder(binding: new Binding([application: [config: config]]))
        bb.beans new DataSourceGrailsPlugin().doWithSpring
        bb
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
