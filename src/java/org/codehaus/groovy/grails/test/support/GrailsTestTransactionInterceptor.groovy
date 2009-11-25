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

import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.context.ApplicationContext

import org.codehaus.groovy.grails.commons.GrailsClassUtils

/**
 * Establishes a rollback only transaction for running a test in.
 */
class GrailsTestTransactionInterceptor {

    static final String TRANSACTIONAL = "transactional"
    
    ApplicationContext applicationContext
    
    GrailsTestTransactionInterceptor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }
    
    /**
     * Wraps {@code body} in a rollback only transaction.
     * 
     * Note: it is the callers responsibility to verify that {@code body} should be run in a transaction.
     */
    void doInTransaction(Closure body) {
        if (applicationContext.containsBean("transactionManager")) {
            def template = new TransactionTemplate(applicationContext.getBean("transactionManager"))
            template.execute([
                doInTransaction: { TransactionStatus status -> 
                    status.setRollbackOnly()
                    body()
                    null
                }
            ] as TransactionCallback)
        } else {
            throw new RuntimeException("Cannot run test in transaction as there is no transactionManager defined")
        }
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