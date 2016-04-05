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
package grails.async

import groovy.transform.CompileStatic

import java.util.concurrent.TimeUnit

/**
 * A list of promises
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class PromiseList<T> implements Promise<List<T>> {

    protected List<Promise> promises = []
    protected List<T> initialized

    @Override
    Promise<List<T>> accept(List<T> value) {
        initialized = value
        return this
    }
    /**
     * Add a promise to the promise list
     *
     * @param callable The callable
     * @return The promise list
     */
    PromiseList leftShift(Closure callable) {
        promises << Promises.createPromise(callable)
        return this
    }

    /**
     * Add a value as a bound promise to the list of values
     *
     * @param callable The callable
     * @return The promise list
     */
    PromiseList leftShift(value) {
        promises << Promises.createBoundPromise(value)
        return this
    }

    /**
     * Add a promise to the promise list
     *
     * @param callable The callable
     * @return The promise list
     */
    PromiseList leftShift(Promise p) {
        promises << p
        return this
    }

    /**
     * Implementation of add that adds any value as a bound promise
     * @param callable The callable
     * @return True if it was added
     */
    boolean add(value) {
        return promises.add(Promises.createBoundPromise(value))
    }

    /**
     * Implementation of add that takes a closure and creates a promise, adding it to the list
     * @param callable The callable
     * @return True if it was added
     */
    boolean add(Closure callable) {
        return promises.add(Promises.createPromise(callable))
    }

    /**
     * Implementation of add that takes a promise, adding it to the list
     * @param callable The callable
     * @return True if it was added
     */
    boolean add(Promise<T> p) {
        return promises.add(p)
    }

    /**
     * Execute the given closure when all promises are complete
     *
     * @param callable The callable
     */
    @SuppressWarnings("unchecked")
    Promise onComplete(Closure callable ) {
        Promises.onComplete(promises, callable)
        return this
    }

    @SuppressWarnings("unchecked")
    Promise onError(Closure callable) {
        Promises.onError(promises, callable)
        return this
    }

    @SuppressWarnings("unchecked")
    Promise then(Closure callable) {
        Promises.onComplete(promises, { List values -> values}).then(callable)
    }

    /**
     * Synchronously obtains all the values from all the promises
     * @return The values
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning) {
        return false
    }

    @Override
    boolean isCancelled() {
        return false
    }

    @Override
    boolean isDone() {
        return promises.every() { Promise p -> p.isDone() }
    }

    List get() {
        if(initialized != null) {
            return initialized
        }
        else {
            Promises.waitAll(promises)
        }
    }

    @Override
    List get(long timeout, TimeUnit units) throws Throwable {
        if(initialized != null) {
            return initialized
        }
        else {
            Promises.waitAll(promises, timeout, units)
        }
    }
}
