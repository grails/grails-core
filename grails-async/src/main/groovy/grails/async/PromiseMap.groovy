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
 * A map-like structure for promises that allows waiting for all values in the map to be populated before
 * executing a callback
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class PromiseMap<K,V> implements Promise<Map<K,V>> {

    protected LinkedHashMap<K, Promise> promises = [:]
    protected LinkedHashMap<Promise, K> promisesKeys = [:]

    PromiseMap() {
    }

    PromiseMap(Map<K, V> values) {
        accept(values)
    }

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
        return promisesKeys.keySet().every() { Promise p -> p.isDone() }
    }

    @Override
    Promise<Map<K, V>> accept(Map<K, V> values) {
        values.each { K key, value ->
            if (value instanceof Promise) {
                put(key, (Promise) value)
            } else if (value instanceof Closure) {
                put(key, (Closure) value)
            } else {
                put(key, value)
            }
        }
        return this
    }

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
     * Put any value and return a promise for that value
     * @param k The key
     * @param value The value
     * @return The promise
     */
    Promise put(K k, value) {
        put(k, Promises.createBoundPromise(value))
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
        def promise = Promises.createPromise(callable)
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
    Promise putAt(String k, value) {
        put((K)k, value)
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
        Promises.waitAll(new ArrayList<>(promises), timeout, units)
        Map<K,V> newMap = [:]
        for(Promise<V> p in promises) {
            def value = p.get()
            newMap[promisesKeys.get(p)] = value
        }
        return newMap
    }

    Promise<Map<K, V>> onComplete(Closure callable) {
        def promises = promises.values().toList()
        Promises.onComplete(promises) { List values ->
            Map<K,V> newMap = [:]
            int i = 0
            for(value in values) {
                def p = promises[i]
                K key = promisesKeys.get(p)
                newMap.put((K)key, (V)value)
                i++
            }
            callable.call(newMap)
            return newMap
        }
        return this
    }

    Promise<Map<K, V>> onError(Closure callable) {
        Promises.onError(promises.values().toList(), callable)
        return this
    }

    Promise<Map<K, V>> then(Closure callable) {

        onComplete callable
    }

    Promise<Map<K, V>> leftShift(Closure callable) {
        then callable
    }
}
