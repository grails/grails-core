/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.plugins.datasource

import grails.util.Environment
import grails.util.GrailsUtil
import grails.util.Metadata

import javax.sql.DataSource

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.tomcat.jdbc.pool.DataSource as TomcatDataSource
import grails.core.GrailsApplication
import org.grails.core.cfg.ConfigurationHelper
import org.grails.core.exceptions.GrailsConfigurationException
import org.grails.transaction.TransactionManagerPostProcessor
import org.grails.transaction.ChainedTransactionManagerPostProcessor
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.jmx.support.JmxUtils
import org.springframework.jndi.JndiObjectFactoryBean
import org.springframework.util.ClassUtils

/**
 * Handles the configuration of a DataSource within Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class DataSourceGrailsPlugin {

    private static final Log log = LogFactory.getLog(DataSourceGrailsPlugin)
    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core: version]

    def watchedResources = "file:./grails-app/conf/DataSource.groovy"

    def doWithSpring = {
        if (!parentCtx?.containsBean('transactionManager')) {
            def whitelistPattern=application?.flatConfig?.'grails.transaction.chainedTransactionManagerPostProcessor.whitelistPattern'
            def blacklistPattern=application?.flatConfig?.'grails.transaction.chainedTransactionManagerPostProcessor.blacklistPattern'
            chainedTransactionManagerPostProcessor(ChainedTransactionManagerPostProcessor, application?.config, whitelistPattern, blacklistPattern)
        }
        transactionManagerPostProcessor(TransactionManagerPostProcessor)

        def dsConfigs = [:]
        if (!application.config.dataSource && application.domainClasses.size() == 0) {
            log.info "No default data source or domain classes found. Default datasource configuration skipped"
        }
        else {
            dsConfigs.dataSource = application.config.dataSource
        }

        application.config.each { name, value ->
            if (name.startsWith('dataSource_') && value instanceof ConfigObject) {
                dsConfigs[name] = value
            }
        }
        
        createDatasource.delegate = delegate
        dsConfigs.each { name, ds -> createDatasource name, ds }
        
        embeddedDatabaseShutdownHook(EmbeddedDatabaseShutdownHook)

        this.registerTomcatJMXMBeans(application, delegate)
    }

    void registerTomcatJMXMBeans(application, beanBuilderDelegate) {
        if(!(application.config?.dataSource?.containsKey('jmxExport') && !application.config.dataSource.jmxExport)) {
            try {
                def jmxMBeanServer = JmxUtils.locateMBeanServer()
                if(jmxMBeanServer) {
                    beanBuilderDelegate.tomcatJDBCPoolMBeanExporter(TomcatJDBCPoolMBeanExporter) { bean ->
                        if (application instanceof GrailsApplication) {
                            grailsApplication = application
                        }
                        server = jmxMBeanServer
                    }
                }
            } catch(e) {
                if(!Environment.isDevelopmentMode() && Environment.isWarDeployed()) {
                    log.warn("Cannot locate JMX MBeanServer. Disabling autoregistering dataSource pools to JMX.", e)
                }
            }
        }
    }

    def createDatasource = { String datasourceName, ds ->
        boolean isDefault = datasourceName == 'dataSource'
        String suffix = isDefault ? '' : datasourceName[10..-1]
        String unproxiedName = "dataSourceUnproxied$suffix"
        String lazyName = "dataSourceLazy$suffix"

        if (parentCtx?.containsBean(datasourceName)) {
            return
        }

        if (ds.jndiName) {
            "$unproxiedName"(JndiObjectFactoryBean) {
                jndiName = ds.jndiName
                expectedType = DataSource
            }
            "$lazyName"(LazyConnectionDataSourceProxy, ref(unproxiedName))
            "$datasourceName"(TransactionAwareDataSourceProxy, ref(lazyName))
            return
        }

        boolean readOnly = Boolean.TRUE.equals(ds.readOnly)
        boolean pooled = !Boolean.FALSE.equals(ds.pooled)

        String driver = ds.driverClassName ?: "org.h2.Driver"

        final String hsqldbDriver = "org.hsqldb.jdbcDriver"
        if (hsqldbDriver.equals(driver) && !ClassUtils.isPresent(hsqldbDriver, getClass().classLoader)) {
            throw new GrailsConfigurationException("Database driver [" + hsqldbDriver +
                "] for HSQLDB not found. Since Grails 2.0 H2 is now the default database. You need to either " +
                "add the 'org.h2.Driver' class as your database driver and change the connect URL format " +
                "(for example 'jdbc:h2:mem:devDb') in DataSource.groovy or add HSQLDB as a dependency of your application.")
        }

        boolean defaultDriver = (driver == "org.h2.Driver")

        String pwd
        boolean passwordSet = false
        if (ds.password) {
            pwd = resolvePassword(ds, application)
            passwordSet = true
        }
        else if (defaultDriver) {
            pwd = ''
            passwordSet = true
        }

        "abstractGrailsDataSourceBean$suffix" {
            driverClassName = driver

            if (pooled) {
                defaultReadOnly = readOnly
            }

            url = ds.url ?: "jdbc:h2:mem:grailsDB;MVCC=TRUE;LOCK_TIMEOUT=10000"

            String theUsername = ds.username ?: (defaultDriver ? "sa" : null)
            if (theUsername != null) {
                username = theUsername
            }

            if (passwordSet) password = pwd

            // support for setting custom properties (for example maxActive) on the dataSource bean
            def dataSourceProperties = ds.properties
            if (dataSourceProperties != null) {
                if (dataSourceProperties instanceof Map) {
                    for (entry in dataSourceProperties) {
                        log.debug("Setting property on dataSource bean $entry.key -> $entry.value")
                        delegate."${entry.key}" = entry.value
                    }
                }
                else {
                    log.warn("dataSource.properties is not an instanceof java.util.Map, ignoring")
                }
            }
        }

        def parentConfig = { dsBean ->
            dsBean.parent = 'abstractGrailsDataSourceBean' + suffix
        }

        String desc = isDefault ? 'data source' : "data source '$datasourceName'"
        log.info "[RuntimeConfiguration] Configuring $desc for environment: $Environment.current"

        Class dsClass = pooled ? TomcatDataSource : readOnly ? ReadOnlyDriverManagerDataSource : DriverManagerDataSource

        def bean = "$unproxiedName"(dsClass, parentConfig)
        if (pooled) {
            bean.destroyMethod = "close"
        }

        "$lazyName"(LazyConnectionDataSourceProxy, ref(unproxiedName))
        "$datasourceName"(TransactionAwareDataSourceProxy, ref(lazyName))
        
        // transactionManager beans will get overridden in Hibernate plugin
        "transactionManager$suffix"(DataSourceTransactionManager, ref(lazyName))
    }

    String resolvePassword(ds, application) {

        if (!ds.passwordEncryptionCodec) {
            return ds.password
        }

        def encryptionCodec = ds.passwordEncryptionCodec
        if (encryptionCodec instanceof Class) {
            try {
                return encryptionCodec.decode(ds.password)
            }
            catch (e) {
                throw new GrailsConfigurationException(
                    "Error decoding dataSource password with codec [$encryptionCodec.name]: $e.message", e)
            }
        }

        encryptionCodec = encryptionCodec.toString()
        def codecClass = application.codecClasses.find {
            it.name.equalsIgnoreCase(encryptionCodec) || it.fullName == encryptionCodec
        }?.clazz

        try {
            if (!codecClass) {
                codecClass = Class.forName(encryptionCodec, true, application.classLoader)
            }
            if (codecClass) {
                return codecClass.decode(ds.password)
            }
            else {
                throw new GrailsConfigurationException(
                    "Error decoding dataSource password. Codec class not found for name [$encryptionCodec]")
            }
        }
        catch (ClassNotFoundException e) {
            throw new GrailsConfigurationException(
                "Error decoding dataSource password. Codec class not found for name [$encryptionCodec]: $e.message", e)
        }
        catch (e) {
            throw new GrailsConfigurationException(
                "Error decoding dataSource password with codec [$encryptionCodec]: $e.message", e)
        }
    }


    def onChange = { event ->
        if (!event.source) {
            return
        }

        final application = event.application
        final slurper = ConfigurationHelper.getConfigSlurper(Environment.current.name, application)

        final newConfig = slurper.parse(event.source)
        application.config.merge(newConfig)

        // TODO: Handle reloading of the dataSource bean
    }

    def onShutdown = { event ->
        if (Metadata.getCurrent().isWarDeployed() || Environment.isFork()) {
            deregisterJDBCDrivers()
        }
    }

    private void deregisterJDBCDrivers() {
        try {
            DataSourceUtils.clearJdbcDriverRegistrations()
        }
        catch (e) {
            log.debug "Error deregistering JDBC driver [$driver]: $e.message", e
        }
    }
}
