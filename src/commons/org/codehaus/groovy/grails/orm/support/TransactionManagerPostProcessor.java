/*
 * Copyright 2004-2006 Graeme Rocher
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
package org.codehaus.groovy.grails.orm.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This is a bean post processor that injects the platform transaction
 * manager into beans that implement the {@link TransactionManagerAware}
 * interface.
 * @author Graeme Rocher
 * @since 0.4
 */
public class TransactionManagerPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements BeanFactoryAware {
    private ConfigurableListableBeanFactory beanFactory;
    private PlatformTransactionManager transactionManager;
    private boolean initialized = false;

    /**
     * Gets the platform transaction manager from the bean factory if
     * there is one.
     * @param beanFactory The bean factory handling this post processor.
     * @throws BeansException
     */
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException(
                    "TransactionManagerPostProcessor requires a ConfigurableListableBeanFactory");
	    }

	    this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    /**
     * Injects the platform transaction manager into the given bean if
     * that bean implements the {@link TransactionManagerAware} interface.
     * @param bean The bean to process.
     * @param name The name of the bean.
     * @return The bean after the transaction manager has been injected.
     * @throws BeansException
     */
    public synchronized boolean postProcessAfterInstantiation(Object bean, String name) throws BeansException {
        // Lazily retrieve the transaction manager from the bean factory.
        // Attempting to retrieve it within 'setBeanFactory()' blocks
        // other bean post processors from processing the beans in the
        // factory!
        if (!this.initialized) {
            // Fetch the names of all the beans that are of type
            // PlatformTransactionManager. Note that we have to pass
            // "false" for the last argument to avoid eager initialisation,
            // otherwise we end up in an endless loop (it triggers the
            // current method).
            String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                    this.beanFactory,
                    PlatformTransactionManager.class,
                    false,
                    false);

            // If at least one is found, use the first of them as the
            // transaction manager for the application.
            if (beanNames.length > 0) {
                this.transactionManager = (PlatformTransactionManager)beanFactory.getBean(beanNames[0]);
            }

            // Don't attempt to retrieve the transaction manager again.
            this.initialized = true;
        }
        
        if (bean instanceof TransactionManagerAware) {
            TransactionManagerAware tma = (TransactionManagerAware) bean;
            tma.setTransactionManager(this.transactionManager);
        }
        return true;
    }
}
