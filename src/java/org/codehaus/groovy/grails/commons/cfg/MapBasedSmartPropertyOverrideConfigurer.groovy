/*
 * Copyright 2009 the original author or authors.
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

package org.codehaus.groovy.grails.commons.cfg

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.BeanDefinition

import org.springframework.transaction.interceptor.TransactionProxyFactoryBean
import org.springframework.beans.factory.FactoryBean

import org.springframework.beans.factory.BeanCreationException

/**
 * Applies property configuration from a Map with bean names as keys and bean properties as name/value Maps
 * (i.e. Map&lt;String,Map&lt;String,Object&gt;&gt;), trying to handle FactoryBeans specially by applying
 * the properties to the <em>actual</em> bean.
 *
 * @author Luke Daley
 */
class MapBasedSmartPropertyOverrideConfigurer implements BeanFactoryPostProcessor {

    final Map beans
    final ClassLoader classLoader

    MapBasedSmartPropertyOverrideConfigurer(Map beans, ClassLoader classLoader) {
        this.beans = beans
        this.classLoader = classLoader
    }

    void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
        if (beans) {
            beans.each { beanName, beanProperties ->
                if (!(beanProperties instanceof Map))
                    throw new IllegalArgumentException("Entry in bean config for bean '" + beanName + "' must be a Map")

                beanProperties.each { beanPropertyName, beanPropertyValue ->
                    applyPropertyValue(factory, beanName, beanPropertyName, beanPropertyValue)
                }
            }
        }
    }

    protected void applyPropertyValue(ConfigurableListableBeanFactory factory, String beanName, String property, Object value) {
        def bd = getTargetBeanDefinition(factory, beanName)
        if (bd != null) {
            bd.propertyValues.addPropertyValue(property, value)
        }
    }

    protected BeanDefinition getTargetBeanDefinition(ConfigurableListableBeanFactory factory, String beanName) {
        if (factory.containsBeanDefinition(beanName)) {
            getTargetBeanDefinition(factory, beanName, factory.getBeanDefinition(beanName))
        } else {
            null
        }
    }

    protected BeanDefinition getTargetBeanDefinition(ConfigurableListableBeanFactory factory, String beanName, BeanDefinition beanDefinition) {
        if (beanDefinition.factoryBeanName) {
            beanDefinition
        } else {
            getTargetBeanDefinition(factory, beanName, beanDefinition, classLoader.loadClass(beanDefinition.beanClassName))
        }
    }

    protected BeanDefinition getTargetBeanDefinition(ConfigurableListableBeanFactory factory, String beanName, BeanDefinition beanDefinition, Class beanClass) {
        if (FactoryBean.isAssignableFrom(beanClass)) {
            getTargetBeanDefinitionForFactoryBean(factory, beanName, beanDefinition, beanClass)
        } else {
            beanDefinition
        }
    }

    protected BeanDefinition getTargetBeanDefinitionForFactoryBean(ConfigurableListableBeanFactory factory, String beanName, BeanDefinition beanDefinition, Class beanClass) {
        if (TransactionProxyFactoryBean.isAssignableFrom(beanClass)) {
            getTargetBeanDefinition(factory, beanName, beanDefinition.propertyValues.getPropertyValue("target").value)
        } else {
            throw new BeanCreationException(beanName, "Unable to determine target bean definition for FactoryBeans of type " + beanClass)
        }
    }
}