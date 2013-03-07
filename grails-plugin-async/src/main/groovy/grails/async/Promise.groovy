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
import groovy.transform.PackageScope
import groovyx.gpars.dataflow.Dataflow

import java.util.concurrent.TimeUnit

/**
 * Encapsulates the notion of a Promise, a Future-like interface designed to easy integration of asynchronous functions
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
public abstract class Promise<T> {

    /**
     * Retrieves the result, blocking until the value is available
     *
     * @return The result
     */
    abstract T get() throws Throwable

    /**
     * Retrieves the result, blocking until the value is available or the timeout is reached
     *
     * @param timeout The timeout
     * @param units The timeout units
     * @return The value
     * @throws Throwable
     */
    abstract T get(final long timeout, final TimeUnit units) throws Throwable;

    /**
     * Execute the given closure when the promise completes
     *
     * @param callable
     * @return The Promise
     */
    abstract Promise<T> onComplete(Closure<T> callable)

    /**
     * Execute the given closure when an error occurs
     *
     * @param callable
     * @return The Promise
     */
    abstract Promise<T> onError(Closure<T> callable)

    /**
     * Same as #onComplete
     */
    abstract Promise<T> then(Closure<T> callable)

    /**
     * Same as #then
     */
    Promise<T> leftShift(Closure<T> callable) {
        then callable
    }

    /**
     * Creates a promise from a closure
     *
     * @param c The closure
     * @return The promise
     */
    static Promise<T> create(Closure<T> c) {
         if (GparsPromiseCreator.isGparsAvailable()) {
             return GparsPromiseCreator.createPromise( c )
         }
         else {
             throw new IllegalStateException("Cannot create promise, no asynchronous library found on classpath (Example GPars).")
         }
    }

    /**
     * Creates a promise from one or more other promises
     *
     * @param promises The promises
     * @return The promise
     */
    static PromiseList create(Promise<T>...promises) {
        if (GparsPromiseCreator.isGparsAvailable()) {
            return GparsPromiseCreator.createPromises( promises )
        }
        else {
            throw new IllegalStateException("Cannot create promise, no asynchronous library found on classpath (Example GPars).")
        }
    }

    @CompileStatic
    @PackageScope
    static class GparsPromiseCreator {
        static final boolean GPARS_PRESENT
        static {
            try {
                GPARS_PRESENT = Thread.currentThread().contextClassLoader.loadClass("groovyx.gpars.dataflow.Dataflow") != null
            } catch (Throwable e) {
                GPARS_PRESENT = false
            }
        }
        static boolean isGparsAvailable() {
            GPARS_PRESENT
        }

        static Promise createPromise(Closure callable) {
            return new GparsPromise(callable)
        }

        static PromiseList createPromises(Promise...promises) {
            def promiseList = new PromiseList()
            promiseList.addAll(promises)

            return promiseList
        }

        @CompileStatic
        static class  GparsPromise<T> extends Promise<T> {

            groovyx.gpars.dataflow.Promise internalPromise

            GparsPromise(groovyx.gpars.dataflow.Promise internalPromise) {
                this.internalPromise = internalPromise
            }
            GparsPromise(Closure callable) {
                internalPromise = Dataflow.task(callable)
            }

            @Override
            T get() {
                internalPromise.get()
            }

            @Override
            T get(long timeout, TimeUnit units) throws Throwable {
                internalPromise.get(timeout, units)
            }

            @Override
            Promise onComplete(Closure callable) {
                internalPromise.whenBound { val ->
                    if ( !(val instanceof Throwable)) {
                        callable.call(val)
                    }
                }
                return this
            }

            @Override
            Promise onError(Closure callable) {
                internalPromise.whenBound { val ->
                    if ( val instanceof Throwable) {
                        callable.call(val)
                    }
                }
                return this
            }

            @Override
            Promise then(Closure callable) {
                return new GparsPromise(internalPromise.then(callable))
            }
        }
    }
}