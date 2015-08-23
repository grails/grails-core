/*
 * Copyright 2014 original authors
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
package org.grails.async.factory.reactor

import grails.async.Promise
import grails.async.PromiseList
import grails.async.factory.AbstractPromiseFactory
import groovy.transform.CompileStatic
import reactor.Environment
import reactor.fn.Consumer
import reactor.rx.Promises

import java.util.concurrent.TimeUnit


/**
 * A {@link grails.async.PromiseFactory} for Reactor
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class ReactorPromiseFactory extends AbstractPromiseFactory {
    static final boolean REACTOR_PRESENT
    static {
        try {
            REACTOR_PRESENT = Thread.currentThread().contextClassLoader.loadClass("reactor.Environment") != null
        } catch (Throwable e) {
            REACTOR_PRESENT = false
        }
    }

    Environment environment

    ReactorPromiseFactory(Environment environment = new Environment()) {
        this.environment = environment
    }

    @Override
    def <T> Promise<T> createPromise(Closure<T>... closures) {
        if(closures.size() == 1) {
            return new ReactorPromise<T>( applyDecorators(closures[0], null), environment)
        }
        def promiseList = new PromiseList()
        for(p in closures) {
            promiseList << applyDecorators(p, null)
        }
        return promiseList
    }

    @Override
    def <T> Promise<T> createBoundPromise(T value) {
        final variable = Promises.success(environment, value)
        return new ReactorPromise<T>(variable)
    }

    @Override
    def <T> Promise<T> createPromise(Class<T> returnType) {
        new ReactorPromise<T>(Promises.ready(environment, Environment.cachedDispatcher()))
    }

    @Override
    Promise<Object> createPromise() {
        new ReactorPromise<Object>(Promises.ready(environment, Environment.cachedDispatcher()))
    }

    @Override
    def <T> List<T> waitAll(List<Promise<T>> promises) {
        waitAll(promises, -1, TimeUnit.SECONDS)
    }

    @Override
    def <T> List<T> waitAll(List<Promise<T>> promises, long timeout, TimeUnit units) {
        Promises.when( promises.collect() { ((ReactorPromise) it).internalPromise } )
                .await(timeout, units)
    }

    @Override
    def <T> Promise<List<T>> onComplete(List<Promise<T>> promises, Closure<?> callable) {
        def newPromise = Promises.when(promises.collect() { ((ReactorPromise) it).internalPromise })
                                 .onSuccess(callable as Consumer)
        return new ReactorPromise<List<T>>(newPromise)
    }

    @Override
    def <T> Promise<List<T>> onError(List<Promise<T>> promises, Closure<?> callable) {
        def newPromise = Promises.when(promises.collect() { ((ReactorPromise) it).internalPromise })
                                 .onError(callable as Consumer<Throwable>)
        return new ReactorPromise<List<T>>(newPromise)
    }

    static boolean isReactorAvailable() {
        return REACTOR_PRESENT;
    }
}
