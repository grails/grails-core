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
package org.codehaus.groovy.grails.orm.hibernate.plugins;

import org.codehaus.groovy.grails.validation.*
import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils
import org.codehaus.groovy.grails.orm.hibernate.ConfigurableLocalSessionFactoryBean;
import org.codehaus.groovy.grails.orm.hibernate.support.*
import org.codehaus.groovy.grails.orm.hibernate.validation.*
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springmodules.beans.factory.config.MapToPropertiesFactoryBean;
import org.springframework.orm.hibernate3.support.OpenSessionInViewInterceptor;
import org.springframework.orm.hibernate3.HibernateAccessor;


/**
 * A plug-in that handles the configuration of Hibernate within Grails 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class HibernateGrailsPlugin {

	def version = GrailsPluginUtils.getGrailsVersion()
	def dependsOn = [dataSource:version,
	                 domainClass:version,
	                 i18n:version,
	                 core: version]
	                 
	def loadAfter = ['controllers']	                 
	
	def watchedResources = "**/grails-app/domain/*.groovy"

	def doWithSpring = {
			def vendorToDialect = new Properties()
			def hibernateDialects = application.classLoader.getResource("hibernate-dialects.properties")
			if(hibernateDialects) {
				def p = new Properties()
				p.load(hibernateDialects.openStream())
				p.each { entry ->
					vendorNameDialectMappings[entry.value] = "org.hibernate.dialect.${e.key}".toString() 
				}
			}
			def ds = application.grailsDataSource
			if(ds || application.domainClasses.size() > 0) {
			    println "configuring Hibernate with ds"
                def hibProps = [:]
                if(ds && ds.loggingSql) {
                    hibProps."hibernate.show_sql" = "true"
                    hibProps."hibernate.format_sql" = "true"
                }
                if(ds && ds.dialect) {
                    hibProps."hibernate.dialect" = ds.dialect.name
                }
                else {
                    dialectDetector(HibernateDialectDetectorFactoryBean) {
                        dataSource = dataSource
                        vendorNameDialectMappings = vendorToDialect
                    }
                    hibProps."hibernate.dialect" = dialectDetector
                }
                if(!ds) {
                    hibProps."hibernate.hbm2ddl.auto" = "create-drop"
                }
                else if(ds.dbCreate) {
                    hibProps."hibernate.hbm2ddl.auto" = ds.dbCreate
                }

                hibernateProperties(MapToPropertiesFactoryBean) {
                    map = hibProps
                }
                sessionFactory(ConfigurableLocalSessionFactoryBean) {
                    dataSource = dataSource
                    if(application.classLoader.getResource("hibernate.cfg.xml")) {
                        configLocation = "classpath:hibernate.cfg.xml"
                    }
                    if(ds?.configClass) {
                        configClass = ds.configClass
                    }
                    hibernateProperties = hibernateProperties
                    grailsApplication = ref("grailsApplication", true)
                    classLoader = classLoader
                }
                transactionManager(HibernateTransactionManager) {
                    sessionFactory = sessionFactory
                }
                persistenceInterceptor(HibernatePersistenceContextInterceptor) {
                    sessionFactory = sessionFactory
                }

                if(manager?.hasGrailsPlugin("controllers")) {
                    openSessionInViewInterceptor(OpenSessionInViewInterceptor) {
                        flushMode = HibernateAccessor.FLUSH_AUTO
                        sessionFactory = sessionFactory
                    }
                    grailsUrlHandlerMapping.interceptors << openSessionInViewInterceptor
                }

            }


	}
	
	def doWithApplicationContext = { ctx ->
	    if(ctx.containsBean('sessionFactory')) {
            def factory = new PersistentConstraintFactory(ctx.sessionFactory, UniqueConstraint.class)
            ConstrainedProperty.registerNewConstraint(UniqueConstraint.UNIQUE_CONSTRAINT, factory);
        }
	}
	
	def onChange = {
			
	}

}