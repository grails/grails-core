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
package org.grails.async.factory.gpars

import grails.async.Promise
import grails.async.PromiseList
import groovy.transform.CompileStatic
import groovyx.gpars.GParsConfig
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowVariable

import grails.async.factory.AbstractPromiseFactory

import java.util.concurrent.TimeUnit

/**
 * GPars implementation of the {@link grails.async.PromiseFactory} interface
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class GparsPromiseFactory extends AbstractPromiseFactory{
    static final boolean GPARS_PRESENT
    static {
        try {
            GPARS_PRESENT = Thread.currentThread().contextClassLoader.loadClass("groovyx.gpars.GParsConfig") != null
        } catch (Throwable e) {
            GPARS_PRESENT = false
        }
    }
    static boolean isGparsAvailable() {
        GPARS_PRESENT
    }

    GparsPromiseFactory() {
        try {
            GParsConfig.setPoolFactory(new LoggingPoolFactory())
        } catch (IllegalArgumentException iae) {
            // ignore
        }
    }

    @Override
    def <T> Promise<T> createBoundPromise(T value) {
        final variable = new DataflowVariable()
        variable << value
        return new GparsPromise<T>(variable)
    }

    @Override
    def <T> Promise<T> createPromise(Class<T> returnType) {
        final variable = new DataflowVariable()
        return new GparsPromise<T>(variable)
    }

    @Override
    Promise<Object> createPromise() {
        final variable = new DataflowVariable()
        return new GparsPromise<Object>(variable)
    }

    @Override
    def <T> Promise<T> createPromise(Closure<T>... closures) {
        if (closures.length == 1) {
            final callable = closures[0]
            return new GparsPromise(applyDecorators(callable, null))
        }

        def promiseList = new PromiseList()
        for (p in closures) {
            applyDecorators(p, null)
            promiseList << p
        }
        return promiseList
    }

    private static Closure<List<?>> originalValuesClosure = { List<?> values -> values }

    @Override
    def <T> List<T> waitAll(List<Promise<T>> promises) {
        final groovyx.gpars.dataflow.Promise<List<T>> promise = (groovyx.gpars.dataflow.Promise<List<T>>)Dataflow.whenAllBound(toGparsPromises(promises), originalValuesClosure)
        return promise.get()
    }

    @Override
    def <T> List<T> waitAll(List<Promise<T>> promises, long timeout, TimeUnit units) {
        final groovyx.gpars.dataflow.Promise<List<T>> promise = (groovyx.gpars.dataflow.Promise<List<T>>)Dataflow.whenAllBound(toGparsPromises(promises), originalValuesClosure)
        return promise.get(timeout, units)

    }

    def <T> List<groovyx.gpars.dataflow.Promise<T>> toGparsPromises(List<Promise<T>> promises) {
        final List<groovyx.gpars.dataflow.Promise<T>> dataflowPromises = promises.collect() { it -> (groovyx.gpars.dataflow.Promise<T>)((GparsPromise<T>)it).internalPromise }
        dataflowPromises
    }

    def <T> Promise<List<T>> onComplete(List<Promise<T>> promises, Closure<?> callable) {
        new GparsPromise<List<T>>(
            Dataflow.whenAllBound(toGparsPromises(promises), callable)
        )
    }

    def <T> Promise<List<T>> onError(List<Promise<T>> promises, Closure<?> callable) {
        new GparsPromise<List<T>>(
            Dataflow.whenAllBound(toGparsPromises(promises), {List l ->}, callable)
        )
    }
}
