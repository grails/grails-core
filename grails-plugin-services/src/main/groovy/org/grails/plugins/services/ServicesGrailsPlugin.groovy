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
package org.grails.plugins.services

import grails.config.Config
import grails.config.Settings
import grails.plugins.Plugin
import grails.util.GrailsUtil
import groovy.transform.CompileStatic
import org.grails.spring.context.support.MapBasedSmartPropertyOverrideConfigurer
import org.springframework.context.ConfigurableApplicationContext

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import grails.core.GrailsServiceClass
import org.grails.core.artefact.ServiceArtefactHandler
import org.grails.transaction.GroovyAwareNamedTransactionAttributeSource
import org.grails.spring.TypeSpecifyableTransactionProxyFactoryBean
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.transaction.annotation.Transactional

/**
 * Configures services in the Spring context.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
class ServicesGrailsPlugin extends Plugin  {

    def version = GrailsUtil.getGrailsVersion()
    def loadAfter = ['hibernate', 'hibernate4']


    def watchedResources = ["file:./grails-app/services/**/*Service.groovy",
                            "file:./plugins/*/grails-app/services/**/*Service.groovy"]

    Closure doWithSpring() {{->
        def application = grailsApplication
        Config config = application.config

        final boolean springTransactionManagement = config.getProperty(Settings.SPRING_TRANSACTION_MANAGEMENT, Boolean.class, true)

        if(springTransactionManagement) {
            xmlns tx:"http://www.springframework.org/schema/tx"
            tx.'annotation-driven'('transaction-manager':'transactionManager')
        }

        for (GrailsServiceClass serviceClass in application.serviceClasses) {
            def providingPlugin = manager?.getPluginForClass(serviceClass.clazz)

            String beanName
            if (providingPlugin && !serviceClass.shortName.toLowerCase().startsWith(providingPlugin.name.toLowerCase())) {
                beanName = "${providingPlugin.name}${serviceClass.shortName}"
            } else {
                beanName = serviceClass.propertyName
            }
            def scope = serviceClass.getPropertyValue("scope")
            def lazyInit = serviceClass.hasProperty("lazyInit") ? serviceClass.getPropertyValue("lazyInit") : true

            "${serviceClass.fullName}ServiceClass"(MethodInvokingFactoryBean) { bean ->
                bean.lazyInit = lazyInit
                targetObject = application
                targetMethod = "getArtefact"
                arguments = [ServiceArtefactHandler.TYPE, serviceClass.fullName]
            }


            def hasDataSource = (config?.dataSources || application.domainClasses)
            if (springTransactionManagement && hasDataSource && shouldCreateTransactionalProxy(serviceClass)) {
                def props = new Properties()

                String attributes = 'PROPAGATION_REQUIRED'
                String datasourceName = serviceClass.datasource
                String suffix = datasourceName == GrailsServiceClass.DEFAULT_DATA_SOURCE ? '' : "_$datasourceName"
                if ( config.getProperty("dataSources.${datasourceName == GrailsServiceClass.DEFAULT_DATA_SOURCE ? 'dataSource' : datasourceName}.readOnly", Boolean, false) ) {
                    attributes += ',readOnly'
                }
                props."*" = attributes

                "${beanName}"(TypeSpecifyableTransactionProxyFactoryBean, serviceClass.clazz) { bean ->
                    if (scope) bean.scope = scope
                    bean.lazyInit = lazyInit
                    target = { innerBean ->
                        innerBean.lazyInit = lazyInit
                        innerBean.factoryBean = "${serviceClass.fullName}ServiceClass"
                        innerBean.factoryMethod = "newInstance"
                        innerBean.autowire = "byName"
                        if (scope) innerBean.scope = scope
                    }
                    proxyTargetClass = true
                    transactionAttributeSource = new GroovyAwareNamedTransactionAttributeSource(transactionalAttributes:props)
                    transactionManager = ref("transactionManager$suffix")
                }
            }
            else {
                "${beanName}"(serviceClass.getClazz()) { bean ->
                    bean.autowire =  true
                    bean.lazyInit = lazyInit
                    if (scope) {
                        bean.scope = scope
                    }
                }
            }
        }

        serviceBeanAliasPostProcessor(ServiceBeanAliasPostProcessor)
    }}

    @CompileStatic
    boolean shouldCreateTransactionalProxy(GrailsServiceClass serviceClass) {
        Class javaClass = serviceClass.clazz

        try {
            serviceClass.transactional &&
              !AnnotationUtils.findAnnotation(javaClass, grails.transaction.Transactional) &&
              !AnnotationUtils.findAnnotation(javaClass, Transactional) &&
                 !javaClass.methods.any { Method m -> AnnotationUtils.findAnnotation(m, Transactional) != null ||
                                                        AnnotationUtils.findAnnotation(m, grails.transaction.Transactional) != null}
        }
        catch (e) {
            return false
        }
    }

    void onChange(Map<String,Object> event) {
        if (!event.source || !applicationContext) {
            return
        }

        if (event.source instanceof Class) {
            def application = grailsApplication
            def ctx = applicationContext

            Class javaClass = event.source
            // do nothing for abstract classes
            if (Modifier.isAbstract(javaClass.modifiers)) return
            def serviceClass = (GrailsServiceClass) application.addArtefact(ServiceArtefactHandler.TYPE, (Class) event.source)
            def serviceName = "${serviceClass.propertyName}"
            def scope = serviceClass.getPropertyValue("scope")

            final boolean springTransactionManagement = config.getProperty(Settings.SPRING_TRANSACTION_MANAGEMENT, Boolean.class, true)

            String datasourceName = serviceClass.datasource
            String suffix = datasourceName == GrailsServiceClass.DEFAULT_DATA_SOURCE ? '' : "_$datasourceName"

            if (springTransactionManagement && shouldCreateTransactionalProxy(serviceClass) && ctx.containsBean("transactionManager$suffix")) {

                def props = new Properties()
                String attributes = 'PROPAGATION_REQUIRED'
                if (application.config["dataSource$suffix"].readOnly) {
                    attributes += ',readOnly'
                }
                props."*" = attributes

                beans {
                    "${serviceClass.fullName}ServiceClass"(MethodInvokingFactoryBean) {
                        targetObject = application
                        targetMethod = "getArtefact"
                        arguments = [ServiceArtefactHandler.TYPE, serviceClass.fullName]
                    }
                    "${serviceName}"(TypeSpecifyableTransactionProxyFactoryBean, serviceClass.clazz) { bean ->
                        if (scope) bean.scope = scope
                        target = { innerBean ->
                            innerBean.factoryBean = "${serviceClass.fullName}ServiceClass"
                            innerBean.factoryMethod = "newInstance"
                            innerBean.autowire = "byName"
                            if (scope) innerBean.scope = scope
                        }
                        proxyTargetClass = true
                        transactionAttributeSource = new GroovyAwareNamedTransactionAttributeSource(transactionalAttributes:props)
                        transactionManager = ref("transactionManager$suffix")
                    }
                }

            }
            else {
                beans {
                    "$serviceName"(serviceClass.getClazz()) { bean ->
                        bean.autowire =  true
                        if (scope) {
                            bean.scope = scope
                        }
                    }
                }

            }
        }
    }
}
