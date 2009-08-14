package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.event.FlushEventListener
import org.codehaus.groovy.grails.commons.ConfigurationHolder

public class AutoFlushPropertyTests extends AbstractGrailsHibernateTests {

    void onSetUp() {
        this.gcl.parseClass('''
          import grails.persistence.*

          @Entity
          class Band {
              String name
          }
          '''
        )
    }

    void testFlushIsDisabledByDefault() {
        def flushCount = 0
        def listener = { flushEvent ->
            ++flushCount
        } as FlushEventListener
        session.listeners.setFlushEventListeners(listener as FlushEventListener[])
        def band = createBand('Tool')
        assert band.save()
        band.merge()
        band.delete()
        assertEquals 'Wrong flush count', 0, flushCount
    }

    void testFlushPropertyTrue() {
        try {
            def config = new ConfigSlurper().parse("grails.gorm.autoFlush = true");

            ConfigurationHolder.config = config
            def flushCount = 0
            def listener = { flushEvent ->
                ++flushCount
            } as FlushEventListener
            session.listeners.setFlushEventListeners(listener as FlushEventListener[])
            def band = createBand('Tool')
            assert band.save()
            assertEquals 'Wrong flush count after save', 1, flushCount
            band.merge()
            assertEquals 'Wrong flush count after merge', 2, flushCount
            band.delete()
            assertEquals 'Wrong flush count after delete', 3, flushCount
        }
        finally {
            ConfigurationHolder.config = null
        }
    }

    void testFlushPropertyFalse() {
        try {
            def config = new ConfigSlurper().parse("grails.gorm.autoFlush = false");

            ConfigurationHolder.config = config
            def flushCount = 0
            def listener = { flushEvent ->
                ++flushCount
            } as FlushEventListener
            session.listeners.setFlushEventListeners(listener as FlushEventListener[])
            def band = createBand('Tool')
            assert band.save()
            band.merge()
            band.delete()
            assertEquals 'Wrong flush count', 0, flushCount
        }
        finally {
            ConfigurationHolder.config = null
        }
    }

    void testTrueFlushArgumentOverridesFalsePropertySetting() {
        try {
            def config = new ConfigSlurper().parse("grails.gorm.autoFlush = false");

            ConfigurationHolder.config = config
            def flushCount = 0
            def listener = { flushEvent ->
                ++flushCount
            } as FlushEventListener
            session.listeners.setFlushEventListeners(listener as FlushEventListener[])
            def band = createBand('Tool')
            assert band.save(flush: true)
            assertEquals 'Wrong flush count after save', 1, flushCount
            band.merge(flush: true)
            assertEquals 'Wrong flush count after merge', 2, flushCount
            band.delete(flush: true)
            assertEquals 'Wrong flush count after delete', 3, flushCount
        }
        finally {
            ConfigurationHolder.config = null
        }
    }

    void testFalseFlushArgumentOverridesTruePropertySetting() {
        try {
            def config = new ConfigSlurper().parse("grails.gorm.autoFlush = true");

            ConfigurationHolder.config = config
            def flushCount = 0
            def listener = { flushEvent ->
                ++flushCount
            } as FlushEventListener
            session.listeners.setFlushEventListeners(listener as FlushEventListener[])
            def band = createBand('Tool')
            assert band.save(flush: false)
            band.merge(flush: false)
            band.delete(flush: false)
            assertEquals 'Wrong flush count', 0, flushCount
        }
        finally {
            ConfigurationHolder.config = null
        }
    }

    void testMapWithoutFlushEntryRespectsTruePropertySetting() {
        try {
            def config = new ConfigSlurper().parse("grails.gorm.autoFlush = true");

            ConfigurationHolder.config = config
            def flushCount = 0
            def listener = { flushEvent ->
                ++flushCount
            } as FlushEventListener
            session.listeners.setFlushEventListeners(listener as FlushEventListener[])
            def band = createBand('Tool')
            assert band.save([:])
            assertEquals 'Wrong flush count after save', 1, flushCount
            band.merge([:])
            assertEquals 'Wrong flush count after merge', 2, flushCount
            band.delete([:])
            assertEquals 'Wrong flush count after delete', 3, flushCount
        }
        finally {
            ConfigurationHolder.config = null
        }
    }

    void testMapWithoutFlushEntryRespectsFalsePropertySetting() {
        try {
            def config = new ConfigSlurper().parse("grails.gorm.autoFlush = false");

            ConfigurationHolder.config = config
            def flushCount = 0
            def listener = { flushEvent ->
                ++flushCount
            } as FlushEventListener
            session.listeners.setFlushEventListeners(listener as FlushEventListener[])
            def band = createBand('Tool')
            assert band.save([:])
            band.merge([:])
            band.delete([:])
            assertEquals 'Wrong flush count', 0, flushCount
        }
        finally {
            ConfigurationHolder.config = null
        }
    }

    private createBand(name) {
        def bandClass = ga.getDomainClass("Band")
        def band = bandClass.newInstance()
        band.name = name
        band
    }
}