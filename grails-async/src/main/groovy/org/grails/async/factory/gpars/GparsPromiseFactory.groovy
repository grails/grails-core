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
import grails.async.PromiseFactory
import grails.async.PromiseList
import grails.async.Promises
import groovy.transform.CompileStatic
import groovyx.gpars.dataflow.Dataflow
import org.grails.async.factory.AbstractPromiseFactory

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
            GPARS_PRESENT = Thread.currentThread().contextClassLoader.loadClass("groovyx.gpars.dataflow.Dataflow") != null
        } catch (Throwable e) {
            GPARS_PRESENT = false
        }
    }
    static boolean isGparsAvailable() {
        GPARS_PRESENT
    }

    @Override
    def <T> Promise<T> createPromisesInternal(Closure<T>[] closures) {
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

    def <T> void onComplete(List<Promise<T>> promises, Closure callable) {
        final gparsPromises = promises.collect { (GparsPromise) it }
        Dataflow.whenAllBound( (List<groovyx.gpars.dataflow.Promise>)gparsPromises.collect { GparsPromise it -> it.internalPromise }, callable)
    }

    def <T> void onError(List<Promise<T>> promises, Closure callable) {
        final gparsPromises = promises.collect { (GparsPromise) it }
        Dataflow.whenAllBound( (List<groovyx.gpars.dataflow.Promise>)gparsPromises.collect { GparsPromise it -> it.internalPromise }, {List l ->}, callable)
    }


}
