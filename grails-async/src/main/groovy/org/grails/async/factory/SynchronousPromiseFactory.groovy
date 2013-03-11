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
package org.grails.async.factory

import grails.async.BoundPromise
import grails.async.Promise
import grails.async.PromiseList
import groovy.transform.CompileStatic

/**
 * A {@link grails.async.PromiseFactory} implementation that constructors promises that execute synchronously.
 * Useful for testing environments.
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class SynchronousPromiseFactory extends AbstractPromiseFactory {
    @Override
    protected def <T> Promise<T> createPromisesInternal(Closure<T>... closures) {
        if (closures.length == 1) {
            try {
                final value = closures[0].call()
                return new BoundPromise<T>(value)
            } catch (Throwable e) {
                return new BoundPromise(e)
            }
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
        try {
            List<T> values = promises.collect { Promise<T> p -> p.get() }
            callable.call(values)
        } catch (Throwable e) {
            // ignore
        }
    }

    def <T> void onError(List<Promise<T>> promises, Closure callable) {
        try {
            promises.each{ Promise<T> p -> p.get() }
        } catch (Throwable e) {
            callable.call(e)
        }
    }
}
