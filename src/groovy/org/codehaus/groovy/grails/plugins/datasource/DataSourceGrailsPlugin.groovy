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
package org.codehaus.groovy.grails.plugins.datasource;

import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils
import org.apache.commons.dbcp.BasicDataSource
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jndi.JndiObjectFactoryBean
import javax.sql.DataSource
import org.codehaus.groovy.grails.orm.support.TransactionManagerPostProcessor
import org.springframework.context.ApplicationContext
import java.sql.Connection

/**
 * A plug-in that handles the configuration of Hibernate within Grails 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class DataSourceGrailsPlugin {

	def version = grails.util.GrailsUtil.getGrailsVersion()
	def dependsOn = [core:GrailsPluginUtils.getGrailsVersion()]
	
	def watchedResources = "file:./grails-app/conf/DataSource.groovy"
		
	def doWithSpring = {
        transactionManagerPostProcessor(TransactionManagerPostProcessor)
                
        def ds = application.config.dataSource
		if(ds || application.domainClasses.size() > 0) {
            if(ds.jndiName) {
                dataSource(JndiObjectFactoryBean) {
                    jndiName = ds.jndiName
                    expectedType = DataSource
                }
            }
            else {
                def properties = {
                    def driver = ds?.driverClassName ? ds.driverClassName : "org.hsqldb.jdbcDriver"
                    driverClassName = driver
                    url = ds?.url ? ds.url : "jdbc:hsqldb:mem:grailsDB"
                    if(driver == "org.hsqldb.jdbcDriver") {
                        username = ds?.username ? ds.username : "sa"
                        password = ds?.password ? ds.password : ""
                    }
                    else {
                        if(ds?.username)
                            username = ds?.username
                        if(ds?.password)
                            password = ds?.password                         
                    }
                }
                if(ds && !parentCtx?.containsBean("dataSource")) {
                    log.info("[RuntimeConfiguration] Configuring data source for environment: ${grails.util.GrailsUtil.getEnvironment()}");
                    if(ds.pooled) {
                        def bean = dataSource(BasicDataSource, properties)
                        bean.destroyMethod = "close"
                    }
                    else {
                        dataSource(DriverManagerDataSource, properties)
                    }
                }
                else if(!parentCtx?.containsBean("dataSource")) {
                    def bean = dataSource(BasicDataSource, properties)
                    bean.destroyMethod = "close"
                }
            }
        }
        else {
            log.info "No data source or domain classes found. Data source configuration skipped"
        }
	}
		
	def onChange = {
	    restartContainer()
    }

    def onShutdown = { event ->

        ApplicationContext appCtx = event.ctx

        if(appCtx?.containsBean("dataSource")) {
            DataSource dataSource = appCtx.dataSource
            Connection connection
            try {
                connection = dataSource.getConnection()
                def dbName =connection.metaData.databaseProductName
                if(dbName == 'HSQL Database Engine') {
                    connection.createStatement().executeUpdate('SHUTDOWN')
                }
            } finally {
                connection?.close()
            }
        }

    }

}