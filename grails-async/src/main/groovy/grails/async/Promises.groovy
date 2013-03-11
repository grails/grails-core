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
import org.grails.async.factory.gpars.GparsPromiseFactory

/**
 * Factory class for working with {@link Promise} instances
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class Promises {

    static PromiseFactory promiseFactory

    static {
        if (GparsPromiseFactory.isGparsAvailable()) {
            promiseFactory = new GparsPromiseFactory()
        }
        else {
            // TODO: synchronous factory
            throw new IllegalStateException("no asynchronous library found on classpath (Example GPars).")
        }
    }

    /**
     * @see PromiseFactory#waitAll(grails.async.Promise[])
     */
    static<T> List<T> waitAll(Promise<T>...promises) {
        promiseFactory.waitAll(promises)
    }

    /**
     * @see PromiseFactory#waitAll(java.util.List)
     */
    static<T> List<T> waitAll(List<Promise<T>> promises) {
        promiseFactory.waitAll(promises)
    }

    /**
     * @see PromiseFactory#onComplete(java.util.List, groovy.lang.Closure)
     */
    static<T> void onComplete(List<T> promises, Closure callable ) {
        promiseFactory.onComplete(promises, callable)
    }
    /**
     * @see PromiseFactory#onError(java.util.List, groovy.lang.Closure)
     */
    static<T> void onError(List<T> promises, Closure callable ) {
        promiseFactory.onError(promises, callable)
    }
    /**
     * @see PromiseFactory#createPromise(java.util.Map)
     */
    static<K,V> Promise<Map<K,V>> createPromise(Map<K, Object> map) {
        promiseFactory.createPromise(map)
    }
    /**
     * @see PromiseFactory#createPromise(groovy.lang.Closure[])
     */
    static<T> Promise<T> createPromise(Closure<T>... c) {
        promiseFactory.createPromise(c)
    }
    /**
     * @see PromiseFactory#createPromise(grails.async.Promise[])
     */
    static <T> Promise<List<T>> createPromise(Promise<T>...promises) {
        promiseFactory.createPromise(promises)
    }

}
