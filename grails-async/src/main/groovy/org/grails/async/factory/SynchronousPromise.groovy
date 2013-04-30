/*
 * Copyright 2012 the original author or authors.
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

import grails.async.Promise
import groovy.transform.CompileStatic

import java.util.concurrent.TimeUnit

/**
 * A promise that executes synchronously, in the same thread as the creator
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class SynchronousPromise<T> implements Promise<T> {
    Closure<T> callable
    def value

    SynchronousPromise(Closure<T> callable) {
        this.callable = callable
    }

    T get() throws Throwable {
        if (value == null) {
            try {
                value = callable.call()
            } catch (e) {
                value = e
            }
        }
        if (value instanceof Throwable) {
            throw value
        }
        return value
    }

    T get(long timeout, TimeUnit units) throws Throwable {
        return get()
    }

    Promise<T> onComplete(Closure callable) {
        try {
            final value = get()
            callable.call(value)
        } catch (e) {
            // ignore
        }
        return this
    }

    Promise<T> onError(Closure callable) {
        try {
            get()
        } catch (e) {
            callable.call(e)
        }
        return this
    }

    Promise<T> then(Closure callable) {
        final value = get()
        return new SynchronousPromise<T>(callable.curry(value))
    }

    Promise<T> leftShift(Closure callable) {
        then callable
    }
}
