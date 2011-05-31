package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.hibernate.event.FlushEventListener

class AutoFlushPropertyTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [AutoFlushBand]
    }

   protected void onTearDown() {
        ConfigurationHolder.config = null
   }

    void testFlushIsDisabledByDefault() {
        def flushCount = 0
        def listener = { flushEvent -> ++flushCount } as FlushEventListener
        session.listeners.flushEventListeners = listener as FlushEventListener[]
        def band = createBand('Tool')
        assertNotNull band.save()
        band.merge()
        band.delete()
        assertEquals 'Wrong flush count', 0, flushCount
    }

    void testFlushPropertyTrue() {
        ga.config.grails.gorm.autoFlush = true
		ga.configChanged()
		
        def flushCount = 0
        def listener = { flushEvent -> ++flushCount } as FlushEventListener
        session.listeners.flushEventListeners = listener as FlushEventListener[]
        def band = createBand('Tool')
        assertNotNull band.save()
        assertEquals 'Wrong flush count after save', 1, flushCount
        band.merge()
        assertEquals 'Wrong flush count after merge', 2, flushCount
        band.delete()
        assertEquals 'Wrong flush count after delete', 3, flushCount
    }

    void testFlushPropertyFalse() {
        ga.config.grails.gorm.autoFlush = false
		ga.configChanged()

        def flushCount = 0
        def listener = { flushEvent -> ++flushCount } as FlushEventListener
        session.listeners.flushEventListeners = listener as FlushEventListener[]
        def band = createBand('Tool')
        assertNotNull band.save()
        band.merge()
        band.delete()
        assertEquals 'Wrong flush count', 0, flushCount
    }

    void testTrueFlushArgumentOverridesFalsePropertySetting() {
        ga.config.grails.gorm.autoFlush = true
		ga.configChanged()

        def flushCount = 0
        def listener = { flushEvent -> ++flushCount } as FlushEventListener
        session.listeners.flushEventListeners = listener as FlushEventListener[]
        def band = createBand('Tool')
        assert band.save(flush: true)
        assertEquals 'Wrong flush count after save', 1, flushCount
        band.merge(flush: true)
        assertEquals 'Wrong flush count after merge', 2, flushCount
        band.delete(flush: true)
        assertEquals 'Wrong flush count after delete', 3, flushCount
    }

    void testFalseFlushArgumentOverridesTruePropertySetting() {
        ga.config.grails.gorm.autoFlush = true
		ga.configChanged()

        def flushCount = 0
        def listener = { flushEvent -> ++flushCount } as FlushEventListener
        session.listeners.flushEventListeners = listener as FlushEventListener[]
        def band = createBand('Tool')
        assertNotNull band.save(flush: false)
        band.merge(flush: false)
        band.delete(flush: false)
        assertEquals 'Wrong flush count', 0, flushCount
    }

    void testMapWithoutFlushEntryRespectsTruePropertySetting() {
        ga.config.grails.gorm.autoFlush = true
		ga.configChanged()

        def flushCount = 0
        def listener = { flushEvent -> ++flushCount } as FlushEventListener
        session.listeners.flushEventListeners = listener as FlushEventListener[]
        def band = createBand('Tool')
        assertNotNull band.save([:])
        assertEquals 'Wrong flush count after save', 1, flushCount
        band.merge([:])
        assertEquals 'Wrong flush count after merge', 2, flushCount
        band.delete([:])
        assertEquals 'Wrong flush count after delete', 3, flushCount
    }

    void testMapWithoutFlushEntryRespectsFalsePropertySetting() {
        ga.config.grails.gorm.autoFlush = false
		ga.configChanged()

        def flushCount = 0
        def listener = { flushEvent -> ++flushCount } as FlushEventListener
        session.listeners.flushEventListeners = listener as FlushEventListener[]
        def band = createBand('Tool')
        assertNotNull band.save([:])
        band.merge([:])
        band.delete([:])
        assertEquals 'Wrong flush count', 0, flushCount
    }

    private createBand(name) {
        def bandClass = ga.getDomainClass(AutoFlushBand.name)
        def band = bandClass.newInstance()
        band.name = name
        band
    }
}


@Entity
class AutoFlushBand {
    String name
}

