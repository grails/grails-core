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

import org.codehaus.groovy.grails.transaction.GrailsTransactionAttribute
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionException
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.interceptor.DefaultTransactionAttribute
import org.springframework.transaction.interceptor.TransactionAttribute
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionSynchronizationManager
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

    Object executeAndRollback(Closure action) throws TransactionException {
        boolean unbinding = shouldUnbindResources()
        Map<Object, Object> preboundResources = null
        try {
            if(unbinding) {
                preboundResources = unbindResources()
            }
            Object result = transactionTemplate.execute(new TransactionCallback() {
                Object doInTransaction(TransactionStatus status) {
                    try {
                        return action.call(status)
                    }
                    catch (Throwable e) {
                        return new ThrowableHolder(e)
                    } finally {
                        status.setRollbackOnly()
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
        finally {
            if(unbinding && preboundResources != null) {
                bindResources(preboundResources)
            }
        }
    }

    Object execute(Closure action) throws TransactionException {
        boolean unbinding = shouldUnbindResources()
        Map<Object, Object> preboundResources = null
        try {
            if(unbinding) {
                preboundResources = unbindResources()
            }
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
                    } finally {
                        boolean inheritRollbackOnly = true
                        if(transactionAttribute instanceof GrailsTransactionAttribute) {
                            inheritRollbackOnly = transactionAttribute.isInheritRollbackOnly()    
                        } 
                        if(inheritRollbackOnly && status.isRollbackOnly()) {
                            status.setRollbackOnly()
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
        finally {
            if(unbinding && preboundResources != null) {
                bindResources(preboundResources)
            }
        }
    }

    protected boolean shouldUnbindResources() {
        return transactionAttribute instanceof GrailsTransactionAttribute && transactionAttribute.isUnbindResources()
    }

    /**
     * Internal holder class for a Throwable, used as a return value
     * from a TransactionCallback (to be subsequently unwrapped again).
     */
    static class ThrowableHolder {

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
    static class ThrowableHolderException extends RuntimeException {

        ThrowableHolderException(Throwable throwable) {
            super(throwable);
        }

        @Override
        public String toString() {
            return getCause().toString();
        }
    }
    
    /**
     * Unbind all thread local bound resources in TransactionSynchronizationManager
     * @return
     */
    protected Map<Object, Object> unbindResources() {
        Map<Object, Object> resourceMap = Collections.unmodifiableMap(new LinkedHashMap(TransactionSynchronizationManager.getResourceMap()));
        for(Map.Entry<Object,Object> entry : resourceMap.entrySet()) {
            TransactionSynchronizationManager.unbindResource(entry.getKey());
        }
        return resourceMap;
    }
    
    /**
     * Bind all resources in the map given as parameter with TransactionSynchronizationManager.bindResource method
     * @param resourceMap
     */
    protected bindResources(Map<Object, Object> resourceMap) {
        for(Map.Entry<Object,Object> entry : resourceMap.entrySet()) {
            TransactionSynchronizationManager.bindResource(entry.getKey(), entry.getValue());
        }
    }
}
