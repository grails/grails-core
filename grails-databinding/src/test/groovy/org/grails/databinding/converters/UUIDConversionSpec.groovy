package org.grails.databinding.converters


import grails.databinding.SimpleMapDataBindingSource

import grails.databinding.SimpleDataBinder

import spock.lang.Specification

class UUIDConversionSpec extends Specification {

    void 'Binding String to a UUID'() {
        given:
        def binder = new SimpleDataBinder()
        binder.registerConverter new UUIDConverter()
        def testClass = new UUIDTestClass()

        and:
        def givenUUID = '534f7cee-bf88-45f3-96f2-9cae0828cd16'

        when:
        binder.bind testClass, [uuid: givenUUID] as SimpleMapDataBindingSource

        then:
        testClass.uuid instanceof UUID
        testClass.uuid.toString() == givenUUID
    }

    void 'Binding badly formatted string to a UUID'() {
        given:
        def binder = new SimpleDataBinder()
        binder.registerConverter new UUIDConverter()
        def testClass = new UUIDTestClass()

        and:
        def givenUUID = '123-not-a-uuid-3291'

        when:
        binder.bind testClass, [uuid: givenUUID] as SimpleMapDataBindingSource

        then:
        notThrown(IllegalArgumentException)
        testClass.uuid == null
    }

    void 'Binding null to UUID'() {
        given:
        def binder = new SimpleDataBinder()
        binder.registerConverter new UUIDConverter()
        def testClass = new UUIDTestClass()

        and:
        def givenUUID = null

        when:
        binder.bind testClass, [uuid: givenUUID] as SimpleMapDataBindingSource

        then:
        notThrown(IllegalArgumentException)
        testClass.uuid == null
    }
}

class UUIDTestClass {
    UUID uuid
}
