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
 * Factory class for working with {@link Promise} instances
 *
 * @author Graeme Rocher
 * @since 2.3
 */
class Promises {

    private static final List<Promise.Decorator> decorators = []

    /**
     * Adds a promise decorator
     *
     * @param decorator The decorator
     */
    static void addDecorator(Promise.Decorator decorator) {
        decorators << decorator
    }

    /**
     * Removes all decorators
     */
    static void removeDecorators() {
        decorators.clear()
    }

    /**
     * Creates a promise that returns a map. The passed argument should contain values that are either closures or Promise instances.
     *
     * @param map The map
     * @return The promise
     */
    static<K,V> Promise<Map<K,V>> createPromise(Map<K, Object> map) {
        if (GparsPromiseCreator.isGparsAvailable()) {
            return GparsPromiseCreator.createPromise( map )
        }
        else {
            throw new IllegalStateException("Cannot create promise, no asynchronous library found on classpath (Example GPars).")
        }
    }
    /**
     * Creates a promise from a closure
     *
     * @param c The closure
     * @return The promise
     */
    static<T> Promise<T> createPromise(Closure<T>... c) {
        if (decorators) {
            c.eachWithIndex { Closure<T> closure, int i ->
                for(d in decorators) {
                    closure = d.decorate(closure)
                }
                c[i] = closure
            }
        }
        if (GparsPromiseCreator.isGparsAvailable()) {
            if (c.length == 1) {
                return GparsPromiseCreator.createPromise( c[0] )
            }
            else {
                return (Promise<T>)GparsPromiseCreator.createPromises( c )
            }
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
    static PromiseList createPromise(Promise...promises) {
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

        static Promise createPromise(Map map) {
            def promiseMap = new PromiseMap()

            map.each { key, value ->
                if (value instanceof Promise) {
                    promiseMap.put(key, (Promise)value)
                }
                if (value instanceof Closure) {
                    promiseMap.put(key, (Closure)value)
                }
            }
            return promiseMap
        }

        static PromiseList createPromises(Promise...promises) {
            def promiseList = new PromiseList()
            for(p in promises) {
                promiseList << p
            }

            return promiseList
        }

        static PromiseList createPromises(Closure...promises) {
            def promiseList = new PromiseList()
            for(p in promises) {
                promiseList << p
            }

            return promiseList
        }

        @CompileStatic
        static class  GparsPromise<T> implements Promise<T> {

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
            Promise<T> leftShift(Closure callable) {
                then callable
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
