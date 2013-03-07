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

/**
 * A list of promises
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class PromiseList extends ArrayList<Promise> {

    /**
     * Add a promise to the promise list
     *
     * @param callable The callable
     * @return The promise list
     */
    PromiseList leftShift(Closure callable) {
        this << Promise.create(callable)
        return this
    }

    /**
     * Implementation of add that takes a closure and creates a promise, adding it to the list
     * @param callable The callable
     * @return True if it was added
     */
    boolean add(Closure callable) {
        return super.add(Promise.create(callable))
    }

    /**
     * Execute the given closure when all promises are complete
     *
     * @param callable The callable
     */
    void onComplete(Closure callable ) {
        if (Promise.GparsPromiseCreator.isGparsAvailable()) {
            final gparsPromises = this.collect { (Promise.GparsPromiseCreator.GparsPromise) it }
            Dataflow.whenAllBound( (List<groovyx.gpars.dataflow.Promise>)gparsPromises.collect { Promise.GparsPromiseCreator.GparsPromise it -> it.internalPromise }, callable)
        }
        else {
            throw new IllegalStateException("Cannot register onComplete callback, no asynchronous library found on classpath (Example GPars).")
        }
    }

    void onError(Closure callable) {
        if (Promise.GparsPromiseCreator.isGparsAvailable()) {
            final gparsPromises = this.collect { (Promise.GparsPromiseCreator.GparsPromise) it }
            Dataflow.whenAllBound( (List<groovyx.gpars.dataflow.Promise>)gparsPromises.collect { Promise.GparsPromiseCreator.GparsPromise it -> it.internalPromise }, {List l ->}, callable)
        }
        else {
            throw new IllegalStateException("Cannot register onError callback, no asynchronous library found on classpath (Example GPars).")
        }
    }

    /**
     * Synchronously obtains all the values from all the promises
     * @return The values
     */
    List get() {
        this.iterator().collect { Promise p -> p.get() }
    }

}
