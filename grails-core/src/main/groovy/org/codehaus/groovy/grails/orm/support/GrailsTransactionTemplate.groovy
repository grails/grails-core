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

package org.codehaus.groovy.grails.orm.support

import groovy.transform.CompileStatic
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionException
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.interceptor.DefaultTransactionAttribute
import org.springframework.transaction.interceptor.TransactionAttribute
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

/**
 * Template class that simplifies programmatic transaction demarcation and
 * transaction exception handling.
 *
 * @author Kazuki YAMAMOTO
 */
@CompileStatic
class GrailsTransactionTemplate {

    private TransactionTemplate transactionTemplate
    private TransactionAttribute transactionAttribute

    GrailsTransactionTemplate(PlatformTransactionManager transactionManager) {
        this(transactionManager, new DefaultTransactionAttribute())
    }

    GrailsTransactionTemplate(PlatformTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
        this.transactionTemplate = new TransactionTemplate(transactionManager, transactionDefinition)
        this.transactionAttribute = new DefaultTransactionAttribute()
    }

    GrailsTransactionTemplate(PlatformTransactionManager transactionManager, TransactionAttribute transactionAttribute) {
        this.transactionTemplate = new TransactionTemplate(transactionManager, transactionAttribute)
        this.transactionAttribute = transactionAttribute
    }

    Object execute(Closure action) throws TransactionException {
        try {
            Object result = transactionTemplate.execute(new TransactionCallback() {
                Object doInTransaction(TransactionStatus status) {
                    try {
                        return action.call(status)
                    }
                    catch (Throwable e) {
                        if (transactionAttribute.rollbackOn(e)) {
                            if (e instanceof RuntimeException) {
                                throw e
                            } else {
                                throw new ThrowableHolderException(e)
                            }
                        } else {
                            return new ThrowableHolder(e)
                        }
                    }
                }
            })

            if (result instanceof ThrowableHolder) {
                throw result.getThrowable()
            } else {
                return result
            }
        }
        catch (ThrowableHolderException e) {
            throw e.getCause()
        }
    }

    /**
     * Internal holder class for a Throwable, used as a return value
     * from a TransactionCallback (to be subsequently unwrapped again).
     */
    private static class ThrowableHolder {

        private final Throwable throwable;

        ThrowableHolder(Throwable throwable) {
            this.throwable = throwable;
        }

        Throwable getThrowable() {
            return this.throwable;
        }
    }

    /**
     * Internal holder class for a Throwable, used as a RuntimeException to be
     * thrown from a TransactionCallback (and subsequently unwrapped again).
     */
    private static class ThrowableHolderException extends RuntimeException {

        ThrowableHolderException(Throwable throwable) {
            super(throwable);
        }

        @Override
        public String toString() {
            return getCause().toString();
        }
    }
}
