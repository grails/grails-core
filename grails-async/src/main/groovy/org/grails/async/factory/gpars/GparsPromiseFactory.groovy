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
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowVariable
import org.grails.async.factory.AbstractPromiseFactory


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
            GPARS_PRESENT = Thread.currentThread().contextClassLoader.loadClass("groovyx.gpars.dataflow.Dataflow") != null
        } catch (Throwable e) {
            GPARS_PRESENT = false
        }
    }
    static boolean isGparsAvailable() {
        GPARS_PRESENT
    }

    @Override
    def <T> Promise<T> createBoundPromise(T value) {
        final variable = new DataflowVariable()
        variable << value
        return new GparsPromise<T>(variable)
    }

    @Override
    def <T> Promise<T> createPromise(Closure<T>... closures) {
        if (closures.length == 1) {
            return new GparsPromise(closures[0])
        }
        else {
            def promiseList = new PromiseList()
            for(p in closures) {
                promiseList << p
            }
            return promiseList
        }
    }

    @Override
    def <T> List<T> waitAll(List<Promise<T>> promises) {
        final gparsPromises = promises.collect { (GparsPromise) it }
        final List<groovyx.gpars.dataflow.Promise<T>> dataflowPromises = gparsPromises.collect() { GparsPromise it -> it.internalPromise }
        final groovyx.gpars.dataflow.Promise<List<T>> promise = Dataflow.whenAllBound(dataflowPromises, { List<T> values -> values })
        return promise.get()
    }

    def <T> Promise<List<T>> onComplete(List<Promise<T>> promises, Closure callable) {
        final gparsPromises = promises.collect { (GparsPromise) it }
        new GparsPromise<List<T>>(
                Dataflow.whenAllBound( (List<groovyx.gpars.dataflow.Promise>)gparsPromises.collect { GparsPromise it -> it.internalPromise }, callable)
        )

    }

    def <T> Promise<List<T>> onError(List<Promise<T>> promises, Closure callable) {
        final gparsPromises = promises.collect { (GparsPromise) it }
        new GparsPromise<List<T>>(
            Dataflow.whenAllBound( (List<groovyx.gpars.dataflow.Promise>)gparsPromises.collect { GparsPromise it -> it.internalPromise }, {List l ->}, callable)
        )
    }


}
