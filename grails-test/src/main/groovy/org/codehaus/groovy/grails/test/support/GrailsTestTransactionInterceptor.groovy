
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
package org.codehaus.groovy.grails.test.support

import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.springframework.context.ApplicationContext
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition

/**
 * Establishes a rollback only transaction for running a test in.
 */
class GrailsTestTransactionInterceptor {

    static final String TRANSACTIONAL = "transactional"

    ApplicationContext applicationContext
    protected List transactionManagers = []
    protected List transactionStatuses = []

    GrailsTestTransactionInterceptor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext

        List<String> transactionManagerNames = applicationContext.beanDefinitionNames.findAll { String name ->
            return name == 'transactionManager' || name.startsWith('transactionManager_')
        }

        transactionManagerNames.eachWithIndex { String name, i ->
            AbstractPlatformTransactionManager tm = applicationContext.getBean(name)
            if(i > 0) {
                tm.setTransactionSynchronization(AbstractPlatformTransactionManager.SYNCHRONIZATION_NEVER)
            }
            transactionManagers << tm
        }
    }

    /**
     * Establishes a transaction.
     */
    void init() {
        transactionManagers.each {transactionManager ->
            transactionStatuses << transactionManager.getTransaction(new DefaultTransactionDefinition())
        }
        
    }

    /**
     * Rolls back the current transaction.
     */
    void destroy() {
        transactionStatuses.eachWithIndex { transactionStatus, i ->
            transactionManagers[i].rollback(transactionStatus)
        }
        
        transactionStatuses.clear()
    }

    /**
     * A test is non transactional if it defines an instance or static property name 'transactional' with
     * a value of {@code false}.
     */
    boolean isTransactional(test) {
        def value = GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue(test, TRANSACTIONAL)
        !(value instanceof Boolean) || (Boolean) value
    }
}
