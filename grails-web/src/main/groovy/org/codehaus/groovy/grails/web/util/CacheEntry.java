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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wrapper for a value inside a cache that adds timestamp information
 * for expiration and prevents "cache storms" with a Lock.
 *
 * JMM happens-before is ensured with AtomicReference.
 *
 * Objects in cache are assumed to not change after publication.
 *
 * @author Lari Hotari
 */
public class CacheEntry<T> {
    protected AtomicReference<T> valueRef=new AtomicReference<T>(null);
    protected long createdMillis;
    protected Lock writeLock=new ReentrantLock();

    public CacheEntry(T value) {
        this.valueRef.set(value);
        resetTimestamp();
    }

    /**
     * gets the current value from the entry and updates it if it's older than timeout
     *
     * updater is a callback for creating an updated value.
     *
     * @param timeout
     * @param updater
     * @return The atomic reference
     */
    public T getValue(long timeout, PrivilegedAction<T> updater) {
        if (timeout < 0 || updater==null) return valueRef.get();

        if (hasExpired(timeout)) {
            try {
                long beforeLockingCreatedMillis = createdMillis;
                writeLock.lock();
                if (shouldUpdate(beforeLockingCreatedMillis)) {
                    valueRef.set(updater.run());
                    resetTimestamp();
                }
            } finally {
                writeLock.unlock();
            }
        }

        return valueRef.get();
    }

    public T getValue() {
        return valueRef.get();
    }

    protected boolean hasExpired(long timeout) {
        return System.currentTimeMillis() - timeout > createdMillis;
    }

    protected boolean shouldUpdate(long beforeLockingCreatedMillis) {
        return beforeLockingCreatedMillis == createdMillis || createdMillis == 0L;
    }

    protected void resetTimestamp() {
        createdMillis = System.currentTimeMillis();
    }

    public long getCreatedMillis() {
        return createdMillis;
    }

    public void expire() {
        createdMillis = 0L;
    }
}
