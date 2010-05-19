package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.event.FlushEventListener
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class AutoFlushPropertyTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class Band {
    String name
}
'''
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
        def config = new ConfigSlurper().parse("grails.gorm.autoFlush = true")

        ConfigurationHolder.config = config
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
        def config = new ConfigSlurper().parse("grails.gorm.autoFlush = false")

        ConfigurationHolder.config = config
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
        def config = new ConfigSlurper().parse("grails.gorm.autoFlush = false")

        ConfigurationHolder.config = config
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
        def config = new ConfigSlurper().parse("grails.gorm.autoFlush = true")

        ConfigurationHolder.config = config
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
        def config = new ConfigSlurper().parse("grails.gorm.autoFlush = true")

        ConfigurationHolder.config = config
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
        def config = new ConfigSlurper().parse("grails.gorm.autoFlush = false")

        ConfigurationHolder.config = config
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
        def bandClass = ga.getDomainClass("Band")
        def band = bandClass.newInstance()
        band.name = name
        band
    }
}
