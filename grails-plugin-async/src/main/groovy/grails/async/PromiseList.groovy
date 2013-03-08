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
import groovyx.gpars.dataflow.Dataflow

import java.util.concurrent.TimeUnit

/**
 * A list of promises
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class PromiseList implements Promise<List> {

    protected def List<Promise> promises = []

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
    boolean add(Promise p) {
        return promises.add(p)
    }

    /**
     * Execute the given closure when all promises are complete
     *
     * @param callable The callable
     */
    Promise onComplete(Closure callable ) {
        if (Promises.GparsPromiseCreator.isGparsAvailable()) {
            final gparsPromises = promises.collect { (Promises.GparsPromiseCreator.GparsPromise) it }
            Dataflow.whenAllBound( (List<groovyx.gpars.dataflow.Promise>)gparsPromises.collect { Promises.GparsPromiseCreator.GparsPromise it -> it.internalPromise }, callable)
            return this
        }
        else {
            throw new IllegalStateException("Cannot register onComplete callback, no asynchronous library found on classpath (Example GPars).")
        }
    }

    Promise onError(Closure callable) {
        if (Promises.GparsPromiseCreator.isGparsAvailable()) {
            final gparsPromises = promises.collect { (Promises.GparsPromiseCreator.GparsPromise) it }
            Dataflow.whenAllBound( (List<groovyx.gpars.dataflow.Promise>)gparsPromises.collect { Promises.GparsPromiseCreator.GparsPromise it -> it.internalPromise }, {List l ->}, callable)
            return this
        }
        else {
            throw new IllegalStateException("Cannot register onError callback, no asynchronous library found on classpath (Example GPars).")
        }
    }

    @Override
    Promise then(Closure callable) {
        onComplete callable
    }
/**
     * Synchronously obtains all the values from all the promises
     * @return The values
     */
    List get() {
        this.iterator().collect { Promise p -> p.get() }
    }

    @Override
    List get(long timeout, TimeUnit units) throws Throwable {
        this.iterator().collect { Promise p -> p.get(timeout, units) }
    }
}
