/*
 * Copyright 2013 the original author or authors.
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

import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic

import org.grails.core.artefact.ServiceArtefactHandler
import org.grails.spring.TypeSpecifyableTransactionProxyFactoryBean
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.MethodInvokingFactoryBean
import org.springframework.core.AliasRegistry

/**
 * Registers bean aliases for service beans provided by plugins.
 *
 * If a plugin named ReportingPlugin provides a service artifact named
 * PrintingService, the corresponding bean name will be reportingPrintingService.
 * This post processor will create a printingService alias which points to the
 * reportingPrintingService bean as long as there is not another bean in the
 * context named printingService.
 *
 * @since 2.3
 * @author Jeff Scott Brown
 */
@CompileStatic
class ServiceBeanAliasPostProcessor implements BeanFactoryPostProcessor {

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        def beanNames = beanFactory.beanDefinitionNames
        Map<String, List<String>> aliasNamesToListOfBeanNames = [:].withDefault { key -> []}
        for(String beanName : beanNames) {
            if(beanName.endsWith('Service')) {
                def beanDefinition = beanFactory.getBeanDefinition(beanName)
                def beanClassName = beanDefinition.beanClassName
                
                if(beanClassName) {
                    String serviceClassName

                    if(beanClassName.endsWith('Service')) {
                        serviceClassName = beanClassName
                    } else if(TypeSpecifyableTransactionProxyFactoryBean.name == beanClassName){
                        def ctorArgumentValues = beanDefinition.constructorArgumentValues
                        if(ctorArgumentValues.argumentCount == 1) {
                            def ctorArgumentValue = ctorArgumentValues.getArgumentValue(0, Class)?.value
                            if(ctorArgumentValue instanceof Class) {
                                Class argumentClass = ctorArgumentValue
                                if(argumentClass.name.endsWith('Service')) {
                                    serviceClassName = argumentClass.name
                                }
                            }
                        }
                    }

                    if(serviceClassName) {
                        // The steps below are a more reliable mechanism for identifying service beans
                        // than looking for bean names that end in 'Service'.  There are a number
                        // of checks below but they are String comparisons and instanceof checks
                        // which should all perform well, and are only applied for the small
                        // subset of bean names that end with ServiceClass
                        String beanServiceClassBeanWrapperName = "${serviceClassName}ServiceClass"
                        if(beanNames.contains(beanServiceClassBeanWrapperName)) {
                            def wrapperBeanDef = beanFactory.getBeanDefinition(beanServiceClassBeanWrapperName)
                            def wrapperBeanClassName = wrapperBeanDef.beanClassName
                            if(MethodInvokingFactoryBean.name == wrapperBeanClassName) {
                                def propertyValues = wrapperBeanDef.getPropertyValues()
                                def targetMethod = propertyValues.getPropertyValue('targetMethod')?.value
                                if('getArtefact' == targetMethod) {
                                    def arguments = propertyValues.getPropertyValue('arguments')?.value
                                    if(arguments instanceof List) {
                                        def argumentList = (List)arguments
                                        if(argumentList.size() == 2) {
                                            def firstArgument = argumentList[0]
                                            if(ServiceArtefactHandler.TYPE == firstArgument) {
                                                def secondArgument = argumentList[1]
                                                if(secondArgument instanceof String && ((String)secondArgument).endsWith('Service')) {
                                                    String serviceClassBeanWrapperClassName = secondArgument
                                                    if(serviceClassBeanWrapperClassName == serviceClassName) {
                                                        def indexOfLastDot = serviceClassName.lastIndexOf(".")
                                                        def shortName = serviceClassName[(indexOfLastDot+1)..-1]
                                                        def potentialAliasName = GrailsNameUtils.getPropertyName(shortName)

                                                        // if the alias name does not conflict with another bean name,
                                                        // add it to the Map for consideration
                                                        if(!beanNames.contains(potentialAliasName)) {
                                                            def aliasExists = false
                                                            if(beanFactory instanceof AliasRegistry) {
                                                                aliasExists = beanFactory.isAlias(potentialAliasName)
                                                            }
                                                            if(!aliasExists) {
                                                                aliasNamesToListOfBeanNames[potentialAliasName] << beanName
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        aliasNamesToListOfBeanNames.each { String aliasName, List<String> listOfBeanNames ->
            // only register the alias if their is only
            // one candidate target bean for the alias
            if(listOfBeanNames.size() == 1) {
                beanFactory.registerAlias listOfBeanNames[0], aliasName
            }
        }
    }
}
