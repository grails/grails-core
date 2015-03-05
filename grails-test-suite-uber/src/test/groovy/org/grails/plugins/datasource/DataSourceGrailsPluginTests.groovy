package org.grails.plugins.datasource

import grails.core.DefaultGrailsApplication
import grails.spring.BeanBuilder
import grails.util.Holders
import groovy.sql.Sql
import org.grails.config.PropertySourcesConfig

import javax.sql.DataSource

import org.apache.tomcat.jdbc.pool.DataSource as TomcatDataSource
import org.grails.web.servlet.context.support.GrailsRuntimeConfigurator
import org.grails.commons.test.AbstractGrailsMockTests
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.jndi.JndiObjectFactoryBean

class DataSourceGrailsPluginTests extends AbstractGrailsMockTests {

    private configurator

    @Override
    protected void onSetUp() {
        Holders.setPluginManager(null)
        Holders.setGrailsApplication(null)
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

        def appCtx = createBeanBuilder(config).createApplicationContext()

        DataSource dataSource = appCtx.getBean("dataSource")
        assert dataSource

        new DataSourceGrailsPlugin().onShutdown()

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
        assertEquals TomcatDataSource.name, beanDef.beanClassName

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
        assertEquals TomcatDataSource.name, beanDef.beanClassName

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
        assertEquals TomcatDataSource.name, beanDef.beanClassName

        assert beanDef.parentName == 'abstractGrailsDataSourceBean'
        assertEquals "org.h2.Driver", parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", parentBeanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:h2:mem:grailsDB;MVCC=TRUE;LOCK_TIMEOUT=10000", parentBeanDef.propertyValues.getPropertyValue('url').value
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
        assertEquals TomcatDataSource.name, beanDef.beanClassName

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
            dataSources {
              dataSource {
                    pooled = true
                    driverClassName = "com.oracle.jdbcDriver"
                    url = "jdbc:oracle::someserver"
                    username = "foo"
                    password = "blah"
              }
              ds2 {
                    driverClassName = "org.h2.Driver"
                    url = "jdbc:h2:mem:testDb2"
                    username = "user"
                    password = "pass"
                    pooled = false
                    dbCreate = "update"
                    readOnly = true
              }
              ds3 {
                    driverClassName = "org.h2.Driver"
                    url = "jdbc:h2:mem:testDb3"
                    username = "sa"
                    password = ""
                    dbCreate = "create-drop"
                    readOnly = true
              }
              ds4 {
                    driverClassName = "org.h2.Driver"
                    url = "jdbc:h2:mem:testDb4"
              }
              jndi {
                    jndiName = "java:comp/env/myDataSource"
              }
            }
        ''')

        def bb = createBeanBuilder(config)

        // default
        def beanDef = bb.getBeanDefinition('dataSource')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceLazy')
        assertEquals LazyConnectionDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals TomcatDataSource.name, beanDef.beanClassName

        def parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean')

        assertEquals "foo", parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals "blah", parentBeanDef.propertyValues.getPropertyValue('password').value
        assertFalse parentBeanDef.propertyValues.getPropertyValue('defaultReadOnly').value

        assertEquals "com.oracle.jdbcDriver", parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "jdbc:oracle::someserver", parentBeanDef.propertyValues.getPropertyValue('url').value

        // ds2
        beanDef = bb.getBeanDefinition('dataSource_ds2')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceLazy_ds2')
        assertEquals LazyConnectionDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied_ds2')
        assertEquals 'pooled false, readOnly true', ReadOnlyDriverManagerDataSource.name, beanDef.beanClassName

        parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean_ds2')

        assertEquals 'user', parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals 'pass', parentBeanDef.propertyValues.getPropertyValue('password').value
        assertNull 'not a TomcatDataSource', parentBeanDef.propertyValues.getPropertyValue('defaultReadOnly')

        assertEquals 'org.h2.Driver', parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals 'jdbc:h2:mem:testDb2', parentBeanDef.propertyValues.getPropertyValue('url').value

        // ds3
        beanDef = bb.getBeanDefinition('dataSource_ds3')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceLazy_ds3')
        assertEquals LazyConnectionDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied_ds3')
        assertEquals 'pooled default true, readOnly true', TomcatDataSource.name, beanDef.beanClassName

        parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean_ds3')

        assertEquals 'sa', parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals '', parentBeanDef.propertyValues.getPropertyValue('password').value
        assertTrue parentBeanDef.propertyValues.getPropertyValue('defaultReadOnly').value

        assertEquals 'org.h2.Driver', parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals 'jdbc:h2:mem:testDb3', parentBeanDef.propertyValues.getPropertyValue('url').value

        // ds4
        beanDef = bb.getBeanDefinition('dataSource_ds4')
        assertEquals TransactionAwareDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceLazy_ds4')
        assertEquals LazyConnectionDataSourceProxy.name, beanDef.beanClassName

        beanDef = bb.getBeanDefinition('dataSourceUnproxied_ds4')
        assertEquals TomcatDataSource.name, beanDef.beanClassName

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

    void testProxying() {

        def config = new ConfigSlurper().parse('''
            dataSources {
              dataSource {
                    pooled = true
                    driverClassName = "org.h2.Driver"
                    url = "jdbc:h2:mem:testDb1"
                    username = "sa"
                    password = ""
              }
              ds2 {
                    pooled = true
                    driverClassName = "org.h2.Driver"
                    url = "jdbc:h2:tcp://localhost:1234/~/test"
                    username = "sa"
                    password = ""
              }
          }
        ''')

        def ctx = createBeanBuilder(config).createApplicationContext()

        def dataSource = ctx.dataSource
        def dataSourceLazy = ctx.dataSourceLazy
        def dataSourceUnproxied = ctx.dataSourceUnproxied
        assert dataSource.targetDataSource.is(dataSourceLazy)
        assert dataSourceLazy.targetDataSource.is(dataSourceUnproxied)

        dataSource.getConnection().close()
        Sql sql = new Sql(dataSource)
        sql.execute('create table thing (foo int)')

        def dataSource_ds2 = ctx.dataSource_ds2
        def dataSourceLazy_ds2 = ctx.dataSourceLazy_ds2
        def dataSourceUnproxied_ds2 = ctx.dataSourceUnproxied_ds2
        assert dataSource_ds2.targetDataSource.is(dataSourceLazy_ds2)
        assert dataSourceLazy_ds2.targetDataSource.is(dataSourceUnproxied_ds2)

        // succeeds because no work is done
        dataSource_ds2.getConnection().close()

        String message = shouldFail() {
            sql = new Sql(dataSource_ds2)
            sql.execute('create table thing (foo int)')
        }
        assert message.contains('Connection is broken')
    }

    // doesn't actually test MVCC, mostly just that it's a valid URL
    void testMvccUrlOption() {
        def config = new ConfigSlurper().parse '''
            dataSource {
                driverClassName = "org.h2.Driver"
                url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
                username = "sa"
                password = ""
                pooled = true
                dbCreate = "create-drop"
            }
        '''

        def bb = createBeanBuilder(config)

        def beanDef = bb.getBeanDefinition('dataSourceUnproxied')
        assertEquals TomcatDataSource.name, beanDef.beanClassName
        assert beanDef.parentName == 'abstractGrailsDataSourceBean'

        def parentBeanDef = bb.getBeanDefinition('abstractGrailsDataSourceBean')
        assertEquals "org.h2.Driver", parentBeanDef.propertyValues.getPropertyValue('driverClassName').value
        assertEquals "sa", parentBeanDef.propertyValues.getPropertyValue('username').value
        assertEquals "", parentBeanDef.propertyValues.getPropertyValue('password').value
        assertEquals "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000", parentBeanDef.propertyValues.getPropertyValue('url').value
    }

    private createAppCtx(config) {

        ga.config = new PropertySourcesConfig().merge(config)

        def corePlugin = gcl.loadClass("org.grails.plugins.CoreGrailsPlugin")
        def dsPlugin = gcl.loadClass("org.grails.plugins.datasource.DataSourceGrailsPlugin")

        configurator = new GrailsRuntimeConfigurator(ga, ctx)
        configurator.configure(ctx.getServletContext())
    }

    private BeanBuilder createBeanBuilder(config) {
        def bb = new BeanBuilder(binding: new Binding([application: [config: config]]))
        def plugin = new DataSourceGrailsPlugin()
        def application = new DefaultGrailsApplication()
        application.config = new PropertySourcesConfig()
        application.config.merge(config)
        plugin.grailsApplication = application
        bb.beans plugin.doWithSpring()
        return bb
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
