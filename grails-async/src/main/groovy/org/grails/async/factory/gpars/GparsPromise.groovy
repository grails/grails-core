/*
 * Copyright 2013 SpringSource
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
package org.grails.async.factory.gpars

import grails.async.Promise
import grails.async.Promises
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovyx.gpars.dataflow.Dataflow

import java.util.concurrent.TimeUnit

/**
 * Implementation of {@link Promise} interface for Gpars
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class  GparsPromise<T> implements Promise<T> {

    groovyx.gpars.dataflow.Promise internalPromise

    GparsPromise(groovyx.gpars.dataflow.Promise internalPromise) {
        this.internalPromise = internalPromise
    }

    GparsPromise(Closure callable) {
        internalPromise = Dataflow.task(callable)
    }

    @Override
    boolean cancel(boolean mayInterruptIfRunning) {
        if(isDone()) {
            return false
        }
        else {
            throw new UnsupportedOperationException("Cancellation not supported")
        }
    }

    @Override
    boolean isCancelled() {
        return false
    }

    @Override
    boolean isDone() {
        return internalPromise.isBound() || internalPromise.isError()
    }

    T get() {
        internalPromise.get()
    }

    T get(long timeout, TimeUnit units) throws Throwable {
        internalPromise.get(timeout, units)
    }

    @Override
    @CompileDynamic
    Promise<T> accept(T value) {
        internalPromise << value
        return this
    }

    Promise<T> leftShift(Closure callable) {
        then callable
    }

    @SuppressWarnings("unchecked")
    Promise onComplete(Closure callable) {
        callable = Promises.promiseFactory.applyDecorators(callable, null)
        internalPromise.whenBound { val ->
            if (!(val instanceof Throwable)) {
                callable.call(val)
            }
        }
        return this
    }

    @SuppressWarnings("unchecked")
    Promise onError(Closure callable) {
        callable = Promises.promiseFactory.applyDecorators(callable, null)
        internalPromise.whenBound { val ->
            if (val instanceof Throwable) {
                callable.call(val)
            }
        }
        return this
    }

    @SuppressWarnings("unchecked")
    Promise then(Closure callable) {
        callable = Promises.promiseFactory.applyDecorators(callable, null)
        return new GparsPromise(internalPromise.then(callable))
    }
}