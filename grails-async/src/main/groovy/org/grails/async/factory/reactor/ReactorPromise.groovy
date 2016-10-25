/*
 * Copyright 2014 original authors
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
package org.grails.async.factory.reactor

import grails.async.Promise
import groovy.transform.CompileStatic
import reactor.Environment
import reactor.fn.Consumer
import reactor.fn.Supplier
import reactor.rx.Promises

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


/**
 * A {@link Promise} implementation for Reactor
 *
 * @author Graeme Rocher
 * @since 3.0
 *
 * @deprecated Reactor promise integration is deprecated and will be removed in a future version of Grails
 */
@CompileStatic
@Deprecated
class ReactorPromise<T> implements Promise<T> {

    reactor.rx.Promise<T> internalPromise

    ReactorPromise(Closure<T> callable, Environment environment) {
        this.internalPromise = Promises.task(environment, Environment.cachedDispatcher(), callable as Supplier<T>)
    }

    ReactorPromise(reactor.rx.Promise promise) {
        if(promise == null ) throw new IllegalArgumentException("Argument [promise] cannot be null")
        this.internalPromise = promise
    }

    @Override
    boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("Cancellation not supported")
    }

    @Override
    boolean isCancelled() {
        return false
    }

    @Override
    boolean isDone() {
        return internalPromise.isComplete()
    }

    @Override
    Promise<T> accept(T value) {
        this.internalPromise.accept(value)
        return this
    }

    @Override
    T get() throws Throwable {
        internalPromise.await()
    }

    @Override
    T get(long timeout, TimeUnit units) throws Throwable {
        T res = (T)internalPromise.await(timeout, units)
        if(!internalPromise.success) {
            throw new TimeoutException()
        }
        else {
            return res
        }
    }

    @Override
    Promise<T> onComplete(Closure callable) {
        new ReactorPromise<T>( internalPromise.onComplete( callable as Consumer ) )
    }

    @Override
    Promise<T> onError(Closure callable) {
        new ReactorPromise<T>( internalPromise.onError(callable as Consumer<Throwable>) )
    }

    @Override
    Promise<T> then(Closure callable) {
        return new ReactorPromise<T>( internalPromise.onSuccess( callable as Consumer<T> ) )
    }
}
