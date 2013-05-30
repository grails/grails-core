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
package org.grails.async.factory.reactor

import grails.async.Promise
import groovy.transform.CompileStatic
import reactor.core.Environment
import reactor.core.Promise as P

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
/**
 * Implementation of {@link P} interface for Reactor
 *
 * @author Stephane Maldini
 * @since 2.3
 */
@CompileStatic
class ReactorPromise<T> implements Promise<T> {

    P<T> internalPromise

    ReactorPromise(P internalPromise) {
        this.internalPromise = internalPromise
    }

    ReactorPromise(Closure<T> callable, Environment environment = null) {
        internalPromise = reactor.core.Promise.<T> from(callable).using(environment).get()
    }

    T get() {
        internalPromise.await()
    }

    T get(long timeout, TimeUnit units) throws Throwable {
        T res = internalPromise.await(timeout, units)
        if (!internalPromise.success) {
            throw new TimeoutException()
        } else {
            res
        }
    }

    Promise leftShift(Closure callable) {
        then callable
    }

    @SuppressWarnings("unchecked")
    Promise<T> onComplete(Closure callable) {
        internalPromise.onSuccess(callable)
        this
    }

    @SuppressWarnings("unchecked")
    Promise<T> onError(Closure callable) {
        internalPromise.onError(callable)
        this
    }

    @SuppressWarnings("unchecked")
    Promise then(Closure callable) {
        new ReactorPromise(internalPromise.then(callable))
    }
}