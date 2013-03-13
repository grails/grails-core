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
package org.grails.async.factory;

import grails.async.*;
import groovy.lang.Closure;
import org.grails.async.decorator.PromiseDecorator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of the {@link grails.async.PromiseFactory} interface
 *
 * @author Graeme Rocher
 * @since 2.3
 */
public abstract class AbstractPromiseFactory implements PromiseFactory{

    @Override
    public <T> Promise<T> createPromise(Closure<T> c, List<PromiseDecorator> decorators) {
        if (!decorators.isEmpty()) {
            for(PromiseDecorator d : decorators) {
                c = d.decorate(c);
            }
        }

        return createPromise(c);
    }

    @Override
    public <T> Promise<List<T>> createPromise(List<Closure<T>> closures, List<PromiseDecorator> decorators) {

        if(!decorators.isEmpty()) {
            List<Closure<T>> newClosures = new ArrayList<Closure<T>>(closures.size());
            for (Closure<T> closure : closures) {
                for (PromiseDecorator decorator : decorators) {
                    closure = decorator.decorate(closure);
                }
                newClosures.add(closure);
            }
            closures = newClosures;
        }
        PromiseList<T> promiseList = new PromiseList<T>();

        for (Closure<T> closure : closures) {
            promiseList.add(closure);
        }
        return promiseList;
    }

    /**
     * @see PromiseFactory#createPromise(grails.async.Promise[])
     */
    public <T> Promise<List<T>> createPromise(Promise<T>... promises) {
        PromiseList<T> promiseList = new PromiseList<T>();
        for(Promise p : promises) {
            promiseList.add(p);
        }
        return promiseList;
    }
    /**
     * @see PromiseFactory#createPromise(java.util.Map)
     */
    public <K, V> Promise<Map<K, V>> createPromise(Map<K, Object> map) {
        PromiseMap<K,V> promiseMap = new PromiseMap<K,V>();
        for (Map.Entry<K, Object> entry : map.entrySet()) {
            K key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Promise) {
                promiseMap.put(key, (Promise)value);
            }
            else if (value instanceof Closure) {
                promiseMap.put(key, (Closure)value);
            }
            else {
                promiseMap.put(key, new BoundPromise<V>((V)value));
            }
        }

        return promiseMap;
    }

    public abstract  <T> List<T> waitAll(List<Promise<T>> promises);

    public <T> List<T> waitAll(Promise<T>... promises) {
        return waitAll(Arrays.asList(promises));
    }
}
