package org.codehaus.groovy.grails.web.util;

import java.security.PrivilegedAction;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * Wrapper for value in side a cache that adds timestamp information
 * 
 * prevents "cache storms" with a Lock
 * 
 * @author Lari Hotari
 */
public class CacheEntry<T> {
    T value;
    long createdMillis;
    Lock writeLock=new ReentrantLock();
    
    public CacheEntry(T value) {
        this.value = value;
        resetTimestamp();
    }
   
    public T getValue(long timeout, PrivilegedAction<T> updater) {
        if(timeout < 0 || updater==null) return value;
        
        if(System.currentTimeMillis() - timeout > createdMillis) {
            try {
                long beforeLockingCreatedMillis = createdMillis;
                writeLock.lock();
                if(beforeLockingCreatedMillis == createdMillis || createdMillis == 0L) {
                    value = updater.run();
                    resetTimestamp();
                }
            } finally {
                writeLock.unlock();
            }
        }
        
        return value;
    }

    private void resetTimestamp() {
        createdMillis = System.currentTimeMillis();
    }
    
    public long getCreatedMillis() {
        return createdMillis;
    }
    
    public void expire() {
        createdMillis = 0L;
    }
}
