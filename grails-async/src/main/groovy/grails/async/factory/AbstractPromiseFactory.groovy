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
package grails.async.factory

import grails.async.Promise
import grails.async.PromiseFactory
import grails.async.PromiseList
import grails.async.PromiseMap
import grails.async.decorator.PromiseDecorator
import grails.async.decorator.PromiseDecoratorLookupStrategy
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.async.factory.BoundPromise

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Abstract implementation of the {@link grails.async.PromiseFactory} interface, subclasses should extend
 * this class to obtain common generic functionality
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
abstract class AbstractPromiseFactory implements PromiseFactory {

    protected Collection<PromiseDecoratorLookupStrategy> lookupStrategies = new ConcurrentLinkedQueue<PromiseDecoratorLookupStrategy>()

    void addPromiseDecoratorLookupStrategy(PromiseDecoratorLookupStrategy lookupStrategy) {
        lookupStrategies.add(lookupStrategy)
    }

    def <T> Promise<T> createBoundPromise(T value) {
        return new BoundPromise<T>(value)
    }

    /**
     * @see PromiseFactory#createPromise(groovy.lang.Closure, java.util.List)
     */
    def <T> Promise<T> createPromise(Closure<T> c, List<PromiseDecorator> decorators) {
        c = applyDecorators(c, decorators)

        return createPromiseInternal(c)
    }

    def <T> Closure<T> applyDecorators(Closure<T> c, List<PromiseDecorator> decorators) {
        List<PromiseDecorator> allDecorators = decorators != null ? new ArrayList<PromiseDecorator>(decorators): new ArrayList<PromiseDecorator>()
        for (PromiseDecoratorLookupStrategy lookupStrategy : lookupStrategies) {
            allDecorators.addAll(lookupStrategy.findDecorators())
        }
        if (!allDecorators.isEmpty()) {
            for(PromiseDecorator d : allDecorators) {
                c = d.decorate(c)
            }
        }
        return c
    }

    /**
     * @see PromiseFactory#createPromise(java.util.List)
     */
    def <T> Promise<List<T>> createPromise(List<Closure<T>> closures) {
        return createPromise(closures,null)
    }

    /**
     * @see PromiseFactory#createPromise(java.util.List, java.util.List)
     */
    def <T> Promise<List<T>> createPromise(List<Closure<T>> closures, List<PromiseDecorator> decorators) {

        List<Closure<T>> newClosures = new ArrayList<Closure<T>>(closures.size())
        for (Closure<T> closure : closures) {
            newClosures.add(applyDecorators(closure, decorators))
        }
        closures = newClosures
        PromiseList<T> promiseList = new PromiseList<T>()

        for (Closure<T> closure : closures) {
            promiseList.add(closure)
        }
        return promiseList
    }

    /**
     * @see PromiseFactory#createPromise(grails.async.Promise[])
     */
    def <T> Promise<List<T>> createPromise(Promise<T>... promises) {
        PromiseList<T> promiseList = new PromiseList<T>()
        for(Promise<T> p : promises) {
            promiseList.add(p)
        }
        return promiseList
    }

    @Override
    def <K, V> Promise<Map<K, V>> createPromise(Map<K, V> map, List<PromiseDecorator> decorators) {
        PromiseMap<K,V> promiseMap = new PromiseMap<K,V>()
        for (Map.Entry<K, V> entry : map.entrySet()) {
            K key = entry.getKey()
            Object value = entry.getValue()
            if (value instanceof Promise) {
                promiseMap.put(key, (Promise<?>)value)
            }
            else if (value instanceof Closure) {
                Closure<?> c = (Closure<?>) value
                applyDecorators(c, decorators)
                promiseMap.put(key, createPromiseInternal(c))
            }
            else {
                promiseMap.put(key, new BoundPromise<V>((V)value))
            }
        }

        return promiseMap
    }
    /**
     * @see PromiseFactory#createPromise(java.util.Map)
     */
    def <K, V> Promise<Map<K, V>> createPromise(Map<K, V> map) {
        createPromise(map, Collections.<PromiseDecorator>emptyList())
    }

    @CompileDynamic
    protected Promise createPromiseInternal(Closure c) {
        return createPromise(c)
    }

    /**
     * @see PromiseFactory#waitAll(grails.async.Promise[])
     */
    @CompileDynamic
    <T> List<T> waitAll(Promise<T>... promises) {
        return waitAll(Arrays.asList(promises))
    }
}
