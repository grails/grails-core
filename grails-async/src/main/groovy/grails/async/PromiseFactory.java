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
package grails.async;

import groovy.lang.Closure;

import java.util.List;
import java.util.Map;

/**
 * An interface capable of creating {@link Promise} instances
 *
 * @author Graeme Rocher
 * @since 2.3
 */
public interface PromiseFactory {

    /**
     * Creates a promise from the given map where the values of the map are either closures or Promise instances
     *
     * @param map The map
     * @return A promise
     */
     <K,V> Promise<Map<K,V>> createPromise(Map<K, Object> map);

    /**
     * Creates a promise from one or more other promises
     *
     * @param promises The promises
     * @return The promise
     */
    <T> Promise<java.util.List<T>> createPromise(Promise<T>...promises);

    /**
     * Creates a promise from one or many closures
     *
     * @param c One or many closures
     * @return A promise
     */
    <T> Promise<T> createPromise(Closure<T>... c);

    /**
     * Adds a promise decorator to the factory
     *
     * @param decorator The promise decorator
     */
    void addDecorator(Promise.Decorator decorator);

    /**
     * Removes all registered decorators
     */
    void removeDecorators();

    /**
     * Synchronously waits for all promises to complete returning a list of values
     *
     * @param promises The promises
     * @return The list of bound values
     */
    <T> List<T> waitAll(Promise<T>...promises);
    /**
     * Synchronously waits for all promises to complete returning a list of values
     *
     * @param promises The promises
     * @return The list of bound values
     */
    <T> List<T> waitAll(List<Promise<T>> promises);

    /**
     * Executes the given callback when the list of promises completes
     *
     * @param promises The promises
     * @param callable The callback to execute
     */
    <T> void onComplete(List<T> promises, Closure callable );
    /**
     * Executes the given callback if an error occurs for the list of promises
     *
     * @param promises The promises The promises
     * @param callable The error callback to execute
     */
    <T> void onError(List<T> promises, Closure callable );
}