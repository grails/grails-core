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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionDefinition

/**
 * Establishes a rollback only transaction for running a test in.
 */
class GrailsTestTransactionInterceptor {

    static final String TRANSACTIONAL = "transactional"

    ApplicationContext applicationContext
    protected final PlatformTransactionManager transactionManager
    protected TransactionStatus transactionStatus

    GrailsTestTransactionInterceptor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext

        if (applicationContext.containsBean("transactionManager")) {
            transactionManager = applicationContext.transactionManager
        }
    }

    /**
     * Establishes a transaction.
     */
    void init() {
        if (!transactionManager) {
            return
        }

        if (transactionStatus == null) {
            transactionStatus = transactionManager.getTransaction(new DefaultTransactionDefinition())
        }
        else {
            throw new RuntimeException("init() called on test transaction interceptor during transaction")
        }
    }

    /**
     * Rolls back the current transaction.
     */
    void destroy() {
        if (!transactionManager) {
            return
        }

        if (transactionStatus) {
            transactionManager.rollback(transactionStatus)
            transactionStatus = null
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
