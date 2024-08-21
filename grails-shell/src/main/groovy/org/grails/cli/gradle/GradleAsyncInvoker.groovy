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
package org.grails.cli.gradle

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



/**
 * @author Graeme Rocher
 */
@CompileStatic
class GradleAsyncInvoker {
    GradleInvoker invoker

    public static final ExecutorService POOL = Executors.newFixedThreadPool(4);

    static {
        Runtime.addShutdownHook {
            try {
                Thread.start {
                    if(!POOL.isTerminated()) {
                        POOL.shutdownNow()
                    }
                }.join(1000)
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    GradleAsyncInvoker(GradleInvoker invoker) {
        this.invoker = invoker
    }

    @Override
    @CompileDynamic
    Object invokeMethod(String name, Object args) {
        POOL.submit {
            invoker."$name"(*args)
        }
    }


}
