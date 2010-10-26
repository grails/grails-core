package org.codehaus.groovy.grails.orm.hibernate.support

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.hibernate.Session
import org.hibernate.SessionFactory
import java.util.concurrent.CountDownLatch

/**
 * @author Luke Daley
 * @since 1.4
 */
class HibernatePersistenceContextInterceptorTests extends AbstractGrailsHibernateTests {

    protected void setUp() {
        super.setUp()
        
        // We are going to manage the session ourselves through the interceptor
        def holder = TransactionSynchronizationManager.getResource(sessionFactory)
        def s = holder.session
        TransactionSynchronizationManager.unbindResource(sessionFactory)
        SessionFactoryUtils.releaseSession(s, sessionFactory)       
    }

    void testSimpleLifecycle() {
        def interceptor = getInterceptor()
        assertEquals("interceptor open", false, interceptor.open)
        interceptor.init()
        assertEquals("interceptor open", true, interceptor.open)
        interceptor.destroy()
        assertEquals("interceptor open", false, interceptor.open)
        interceptor.init()
        assertEquals("interceptor open", true, interceptor.open)
        interceptor.destroy()
        assertEquals("interceptor open", false, interceptor.open)
    }

    void testNestedLifecycle() {
        def interceptor = getInterceptor()
        assertEquals("interceptor open", false, interceptor.open)
        
        interceptor.init()
        assertEquals("interceptor open", true, interceptor.open)
        interceptor.init()
        assertEquals("interceptor open", true, interceptor.open)
        interceptor.destroy()
        assertEquals("interceptor open", true, interceptor.open)
        interceptor.destroy()
        assertEquals("interceptor open", false, interceptor.open)
        
        interceptor.init()
        assertEquals("interceptor open", true, interceptor.open)
        interceptor.destroy()
        assertEquals("interceptor open", false, interceptor.open)
    }
    
    void testMultithreadedLifecycle() {
        def interceptor = getInterceptor()
        def latch = new CountDownLatch(1)

        Thread.start {
            interceptor.init()
            latch.await()
            interceptor.destroy()
        }

        try {
            assertEquals("interceptor open", false, interceptor.open)
            interceptor.init()
            assertEquals("interceptor open", true, interceptor.open)
            interceptor.destroy()
            assertEquals("interceptor open", false, interceptor.open)
        } finally {
            latch.countDown()
        }
    }
    
    void testMultiThreadedNestedLifecycle() {
        def interceptor = getInterceptor()
        def latch = new CountDownLatch(1)

        Thread.start {
            interceptor.init()
            interceptor.init()
            latch.await()
            interceptor.destroy()
            interceptor.destroy()
        }

        try {
            assertEquals("interceptor open", false, interceptor.open)
            interceptor.init()
            assertEquals("interceptor open", true, interceptor.open)
            interceptor.destroy()
            assertEquals("interceptor open", false, interceptor.open)
        } finally {
            latch.countDown()
        }
    }
        
    protected getInterceptor() {
        appCtx.persistenceInterceptor
    }
}
