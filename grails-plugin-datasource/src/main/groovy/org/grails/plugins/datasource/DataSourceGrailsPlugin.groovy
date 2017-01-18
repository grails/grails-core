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

import grails.core.GrailsApplication
import grails.plugins.Plugin
import grails.util.Environment
import grails.util.GrailsUtil
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.grails.datastore.gorm.jdbc.connections.DataSourceConnectionSourceFactory
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSources
import org.grails.datastore.mapping.core.connections.ConnectionSourcesInitializer
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.transaction.ChainedTransactionManagerPostProcessor
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jmx.support.JmxUtils
import org.springframework.util.ClassUtils


/**
 * Handles the configuration of a DataSource within Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class DataSourceGrailsPlugin extends Plugin {

    private static final Log log = LogFactory.getLog(DataSourceGrailsPlugin)
    public static final String TRANSACTION_MANAGER_WHITE_LIST_PATTERN = 'grails.transaction.chainedTransactionManagerPostProcessor.whitelistPattern'
    public static final String TRANSACTION_MANAGER_BLACK_LIST_PATTERN = 'grails.transaction.chainedTransactionManagerPostProcessor.blacklistPattern'
    public static final String TRANSACTION_MANAGER_ENABLED = 'grails.transaction.chainedTransactionManagerPostProcessor.enabled'
    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core: version]

    @Override
    Closure doWithSpring() {{->
        GrailsApplication application = grailsApplication

        if (pluginManager.hasGrailsPlugin('hibernate4') || pluginManager.hasGrailsPlugin('hibernate5')) {

            if (!springConfig.unrefreshedApplicationContext?.containsBean('transactionManager')) {
                Boolean enabled = config.getProperty(TRANSACTION_MANAGER_ENABLED, Boolean, false)
                if (enabled) {
                    def whitelistPattern=config.getProperty(TRANSACTION_MANAGER_WHITE_LIST_PATTERN, '')
                    def blacklistPattern=config.getProperty(TRANSACTION_MANAGER_BLACK_LIST_PATTERN,'')
                    chainedTransactionManagerPostProcessor(ChainedTransactionManagerPostProcessor, config, whitelistPattern ?: null, blacklistPattern ?: null)
                }
            }

            if (ClassUtils.isPresent('org.h2.Driver', this.class.classLoader)) {
                embeddedDatabaseShutdownHook(EmbeddedDatabaseShutdownHook)
            }

        } else {
            DataSourceConnectionSourceFactory factory = new DataSourceConnectionSourceFactory()

            ConnectionSources sources = ConnectionSourcesInitializer.create(factory, config)

            for (ConnectionSource source: sources.allConnectionSources) {
                String dsName = "dataSource"
                String tmName = "transactionManager"
                if (source.name != ConnectionSource.DEFAULT) {
                    String suffix = "_${source.name}"
                    dsName = "${dsName}${suffix}"
                    tmName = "${tmName}${suffix}"
                }
                "$dsName"(InstanceFactoryBean, source.source)
                "$tmName"(DataSourceTransactionManager, ref(dsName))
            }

        }

        if(config.getProperty('dataSource.jmxExport', Boolean, false) && ClassUtils.isPresent('org.apache.tomcat.jdbc.pool.DataSource')) {
            try {
                def jmxMBeanServer = JmxUtils.locateMBeanServer()
                if(jmxMBeanServer) {
                    tomcatJDBCPoolMBeanExporter(TomcatJDBCPoolMBeanExporter) { bean ->
                        delegate.grailsApplication = application
                        server = jmxMBeanServer
                    }
                }
            } catch(e) {
                if(!Environment.isDevelopmentMode() && Environment.isWarDeployed()) {
                    log.warn("Cannot locate JMX MBeanServer. Disabling autoregistering dataSource pools to JMX.", e)
                }
            }
        }
    }}

    @Override
    void onShutdown(Map<String, Object> event) {
        try {
            DataSourceUtils.clearJdbcDriverRegistrations()
        }
        catch (e) {
            log.debug "Error deregistering JDBC drivers: $e.message", e
        }
    }

}
