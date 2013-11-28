/*
 * Copyright 2011 the original author or authors.
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
package org.codehaus.groovy.grails.web.util;

import java.security.PrivilegedAction;
import java.util.concurrent.Callable;


/**
 * Wrapper for a value inside a cache that adds timestamp information
 * for expiration and prevents "cache storms" with a Lock.
 *
 * JMM happens-before is ensured with AtomicReference.
 *
 * Objects in cache are assumed to not change after publication.
 *
 * @author Lari Hotari
 * @deprecated Use grails.util.CacheEntry
 * @see grails.util.CacheEntry
 */
public class CacheEntry<T> extends grails.util.CacheEntry<T> {
    public CacheEntry(T value) {
        super(value);
    }

    @Deprecated
    public T getValue(long timeout, final PrivilegedAction<T> updater) {
        return super.getValue(timeout, new Callable<T>() {
            @Override
            public T call() throws Exception {
                return updater.run();
            }
        });
    }
}
