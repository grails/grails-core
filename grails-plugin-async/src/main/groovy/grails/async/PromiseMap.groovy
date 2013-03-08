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
 * A map-like structure for promises that allows waiting for all values in the map to be populated before
 * executing a callback
 *
 * @author Graeme Rocher
 * @since 2.3
 *
 */
@CompileStatic
class PromiseMap<K,V> implements Promise<Map<K,V>> {

    protected LinkedHashMap<K, Promise> promises = [:]
    protected LinkedHashMap<Promise, K> promisesKeys = [:]

    /**
     * @return The size the map
     */
    int size() {
        promises.size()
    }

    /**
     * @return Whether the map is empty
     */
    boolean isEmpty() {
        promises.isEmpty()
    }

    /**
     * @param o The key
     * @return Whether the promise map contains the given key
     */
    boolean containsKey(K o) {
        promises.containsKey(o)
    }

    /**
     * Gets a promise instance for the given key
     *
     * @param o The key
     * @return A promise
     */
    Promise get(K o) {
        promises.get(o)
    }

    /**
     * Adds a promise for the given key
     *
     * @param k The key
     * @param promise The promise
     * @return The previous promise
     */
    Promise put(K k, Promise promise) {
        promisesKeys.put(promise, k)
        promises.put(k, promise)
    }

    /**
     * Adds a promise for the given key
     *
     * @param k The key
     * @param promise The promise
     * @return The previous promise
     */
    Promise put(K k, Closure callable) {
        def promise = Promises.create(callable)
        promisesKeys.put(promise, k)
        promises.put(k, promise)
    }

    /**
     * Gets a promise instance for the given key
     *
     * @param o The key
     * @return A promise
     */
    Promise getAt(K o) {
        get(o)
    }

    /**
     * Adds a promise for the given key
     *
     * @param k The key
     * @param promise The promise
     * @return The previous promise
     */
    Promise putAt(String k, Promise promise) {
        put((K)k, promise)
    }

    /**
     * Adds a promise for the given key
     *
     * @param k The key
     * @param promise The promise
     * @return The previous promise
     */
    Promise putAt(String k, Closure promise) {
        put((K)k, promise)
    }

    /**
     * Adds a promise for the given key
     *
     * @param k The key
     * @param promise The promise
     * @return The previous promise
     */
    Promise putAt(Integer k, Promise promise) {
        put((K)k, promise)
    }

    /**
     * Adds a promise for the given key
     *
     * @param k The key
     * @param promise The promise
     * @return The previous promise
     */
    Promise putAt(Integer k, Closure promise) {
        put((K)k, promise)
    }

    /**
     * Synchronously return the populated map with all values obtained from promises used
     * inside the populated map
     *
     * @return A map where the values are obtained from the promises
     */
    Map<K, V> get() throws Throwable {
        def promises = promises.values()
        Map<K,V> newMap = [:]
        for(Promise<V> p in promises) {
            def value = p.get()
            newMap[promisesKeys.get(p)] = value
        }
        return newMap
    }

    /**
     * Synchronously return the populated map with all values obtained from promises used
     * inside the populated map
     *
     * @param  timeout The timeout period
     * @param units The timeout units
     * @return A map where the values are obtained from the promises
     */
    Map<K, V> get(long timeout, TimeUnit units) throws Throwable {
        def promises = promises.values()
        Map<K,V> newMap = [:]
        for(Promise<V> p in promises) {
            def value = p.get(timeout,units)
            newMap[promisesKeys.get(p)] = value
        }
        return newMap
    }

    Promise<Map<K, V>> onComplete(Closure<Map<K, V>> callable) {
        if (Promises.GparsPromiseCreator.isGparsAvailable()) {
            def promises = promises.values()
            final gparsPromises = promises.collect { (Promises.GparsPromiseCreator.GparsPromise) it }
            Dataflow.whenAllBound( (List<groovyx.gpars.dataflow.Promise>)gparsPromises.collect { Promises.GparsPromiseCreator.GparsPromise it -> it.internalPromise }) { List values ->
                Map<K,V> newMap = [:]
                values.eachWithIndex { V value, int i ->
                    def p = promises[i]
                    K key = promisesKeys.get(p)
                    newMap.put(key, value)
                }

                callable.call(newMap)
            }
            return this
        }
        else {
            throw new IllegalStateException("Cannot register onComplete callback, no asynchronous library found on classpath (Example GPars).")
        }
    }

    Promise<Map<K, V>> onError(Closure callable) {
        if (Promises.GparsPromiseCreator.isGparsAvailable()) {
            final gparsPromises = promises.values().collect { (Promises.GparsPromiseCreator.GparsPromise) it }
            Dataflow.whenAllBound( (List<groovyx.gpars.dataflow.Promise>)gparsPromises.collect { Promises.GparsPromiseCreator.GparsPromise it -> it.internalPromise }, {List l ->}, callable)
            return this
        }
        else {
            throw new IllegalStateException("Cannot register onError callback, no asynchronous library found on classpath (Example GPars).")
        }
    }

    Promise<Map<K, V>> then(Closure<Map<K, V>> callable) {
        onComplete callable
    }

    Promise<Map<K, V>> leftShift(Closure<Map<K, V>> callable) {
        onComplete callable
    }
}
