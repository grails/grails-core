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
     * @return
     */
    public T getValue(long timeout, PrivilegedAction<T> updater) {
        if(timeout < 0 || updater==null) return valueRef.get();

        if(hasExpired(timeout)) {
            try {
                long beforeLockingCreatedMillis = createdMillis;
                writeLock.lock();
                if(shouldUpdate(beforeLockingCreatedMillis)) {
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
