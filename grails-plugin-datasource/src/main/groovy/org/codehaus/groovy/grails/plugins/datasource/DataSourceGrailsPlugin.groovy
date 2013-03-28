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
package org.codehaus.groovy.grails.plugins.datasource

import grails.util.Environment
import grails.util.GrailsUtil
import grails.util.Metadata

import java.sql.Connection

import javax.sql.DataSource

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.tomcat.jdbc.pool.DataSource as TomcatDataSource
import org.codehaus.groovy.grails.commons.cfg.ConfigurationHelper
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException
import org.codehaus.groovy.grails.orm.support.TransactionManagerPostProcessor
import org.springframework.beans.factory.BeanIsNotAFactoryException
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
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

    def doWithWebDescriptor = { xml ->

        // only configure if explicitly enabled or in dev mode if not disabled
        def enabled = application.config.grails.dbconsole.enabled
        if (!(enabled instanceof Boolean)) {
            enabled = Environment.current == Environment.DEVELOPMENT
        }
        if (!enabled) {
            return
        }

        String urlPattern = (application.config.grails.dbconsole.urlRoot ?: '/dbconsole') + '/*'

        def listeners = xml.'listener'

        listeners[listeners.size() - 1] + {
            'servlet' {
                'servlet-name'('H2Console')
                'servlet-class'('org.h2.server.web.WebServlet')
                'init-param' {
                    'param-name'('-webAllowOthers')
                    'param-value'('true')
                }
                'load-on-startup'('2')
            }

            'servlet-mapping' {
                'servlet-name'('H2Console')
                'url-pattern'(urlPattern)
            }
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

        ApplicationContext appCtx = event.ctx

        appCtx.getBeansOfType(DataSource).each { String name, DataSource dataSource ->
            shutdownDatasource dataSource, name, appCtx
        }

        if (Metadata.getCurrent().isWarDeployed() || Environment.isFork()) {
            deregisterJDBCDrivers()
        }
    }

    void shutdownDatasource(DataSource dataSource, String name, ctx) {
        Connection connection
        try {
            connection = dataSource.getConnection()
            try {
                def dbName = connection.metaData.databaseProductName
                if (dbName == 'HSQL Database Engine' || dbName == 'H2') {
                    connection.createStatement().executeUpdate('SHUTDOWN')
                }
            } catch (e) {
                // already closed, ignore
            }
        }
        catch (e) {
            log.error "Error shutting down datasource: $e.message", e
        }
        finally {
            try { connection?.close() } catch (ignored) {}
            try {
                if (dataSource.respondsTo('close')) {
                    boolean shouldClose = true
                    try {
                        def factory = ctx.getBean('&' + name)
                        if (factory instanceof JndiObjectFactoryBean) {
                            shouldClose = false
                        }
                    }
                    catch (BeanIsNotAFactoryException e) {
                        // not using JNDI
                    }

                    if (shouldClose) {
                        dataSource.close()
                    }
                }
            }
            catch (ignored) {}
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
