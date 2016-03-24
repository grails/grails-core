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


import groovy.lang.Closure
import groovy.transform.CompileStatic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import grails.async.decorator.PromiseDecorator;
import org.grails.async.factory.SynchronousPromiseFactory;
import org.grails.async.factory.gpars.GparsPromiseFactory;
import org.grails.async.factory.reactor.ReactorPromiseFactory;

/**
 * Factory class for working with {@link Promise} instances
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
public class Promises {

    static PromiseFactory promiseFactory;

    static {
        if (GparsPromiseFactory.isGparsAvailable()) {
            promiseFactory = new GparsPromiseFactory();
        }
        else {
            promiseFactory = new SynchronousPromiseFactory();
        }
    }

    private Promises() {
    }

    public static PromiseFactory getPromiseFactory() {
        if (promiseFactory == null) {
            if (GparsPromiseFactory.isGparsAvailable()) {
                promiseFactory = new GparsPromiseFactory();
            }
            else {
                promiseFactory = new SynchronousPromiseFactory();
            }
        }
        return promiseFactory;
    }

    public static void setPromiseFactory(PromiseFactory promiseFactory) {
        Promises.@promiseFactory = promiseFactory;
    }

    /**
     * @see PromiseFactory#waitAll(grails.async.Promise[])
     */
    public static<T> List<T> waitAll(Promise<T>...promises) {
        return getPromiseFactory().waitAll(promises);
    }

    /**
     * @see PromiseFactory#waitAll(java.util.List)
     */
    public static<T> List<T> waitAll(List<Promise<T>> promises) {
        return getPromiseFactory().waitAll(promises);
    }

    /**
     * @see PromiseFactory#waitAll(java.util.List)
     */
    public static<T> List<T> waitAll(List<Promise<T>> promises, final long timeout, final TimeUnit units) {
        return getPromiseFactory().waitAll(promises, timeout, units);
    }

    /**
     * @see PromiseFactory#onComplete(java.util.List, groovy.lang.Closure)
     */
    public static<T> Promise<List<T>> onComplete(List<Promise<T>> promises, Closure<?> callable ) {
        return getPromiseFactory().onComplete(promises, callable);
    }
    /**
     * @see PromiseFactory#onError(java.util.List, groovy.lang.Closure)
     */
    public static<T> Promise<List<T>> onError(List<Promise<T>> promises, Closure<?> callable ) {
        return getPromiseFactory().onError(promises, callable);
    }
    /**
     * @see PromiseFactory#createPromise(java.util.Map)
     */
    public static<K,V> Promise<Map<K,V>> createPromise(Map<K, V> map) {
        return getPromiseFactory().createPromise(map);
    }
    /**
     * @see PromiseFactory#createPromise(groovy.lang.Closure[])
     */
    public static<T> Promise<T> createPromise(Closure<T>... c) {
        return getPromiseFactory().createPromise(c);
    }

    /**
     * @see PromiseFactory#createPromise(java.util.Map)
     */
    public static<K,V> Promise<Map<K,V>> tasks(Map<K, V> map) {
        return getPromiseFactory().createPromise(map);
    }
    /**
     * @see PromiseFactory#createPromise(groovy.lang.Closure[])
     */
    public static<T> Promise<T> task(Closure<T> c) {
        return getPromiseFactory().createPromise(c);
    }
    /**
     * @see PromiseFactory#createPromise(groovy.lang.Closure[])
     */
    public static<T> Promise<T> tasks(Closure<T>... c) {
        return getPromiseFactory().createPromise(c);
    }
    /**
     * @see PromiseFactory#createPromise(groovy.lang.Closure[])
     */
    public static<T> Promise<List<T>> tasks(List<Closure<T>> closures) {
        return getPromiseFactory().createPromise(closures);
    }

    /**
     * @see grails.async.PromiseFactory#createPromise()
     */
    public static Promise<Object> createPromise() {
        return getPromiseFactory().createPromise();
    }

    /**
     * @see grails.async.PromiseFactory#createPromise(Class)
     */
    public static<T> Promise<T> createPromise(Class<T> returnType) {
        return getPromiseFactory().createPromise(returnType);
    }

    /**
     * @see PromiseFactory#createPromise(groovy.lang.Closure, java.util.List)
     */
    public static<T> Promise<T> createPromise(Closure<T> c, List<PromiseDecorator> decorators) {
        return getPromiseFactory().createPromise(c, decorators);
    }
    /**
     * @see PromiseFactory#createPromise(java.util.List, java.util.List)
     */
    public static<T> Promise<List<T>> createPromise(List<Closure<T>> closures, List<PromiseDecorator> decorators) {
        return getPromiseFactory().createPromise(closures, decorators);
    }
    /**
     * @see PromiseFactory#createPromise(grails.async.Promise[])
     */
    public static <T> Promise<List<T>> createPromise(Promise<T>...promises) {
        return getPromiseFactory().createPromise(promises);
    }

    /**
     * @see PromiseFactory#createBoundPromise(java.lang.Object)
     */
    public static<T> Promise<T> createBoundPromise(T value) {
        return getPromiseFactory().createBoundPromise(value);
    }
}
