package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class DomainEventsWithMethodsTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [PersonEvent, PersonEvent2, Address, PersonWithOverloadedBeforeValidate, PersonWithNoArgBeforeValidate, PersonWithListArgBeforeValidate]
    }

    // test for GRAILS-4059
    void testLastUpdateDoesntChangeWhenNotDirty() {
        def personClass = ga.getDomainClass(PersonEvent.name).clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        assertNotNull "person should have been saved",p.save()

        p.addToAddresses(postCode:"23209")
        assertNotNull "person should have been updated",p.save(flush:true)

        def address = p.addresses.iterator().next()

        assertTrue "address should have been saved", session.contains(address)
        def current = address.lastUpdated
        assertNotNull "should have created time sstamp",current

        session.flush()

        personClass.executeQuery("select f from PersonEvent f join fetch f.addresses") // cause auto-flush of session

        def now = address.lastUpdated
        assertEquals "The last updated date should not have been changed!", current, now
    }

    // test for GRAILS-4041
    void testNoModifyVersion() {

        def personClass = ga.getDomainClass(PersonEvent2.name).clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save(flush:true)

        p.name = "Body"
        p.save(flush:true)

        assertNotNull "should have modified name to Wilma", personClass.findWhere(name:"Wilma")
    }

    void testDisabledAutoTimestamps() {
        def personClass = ga.getDomainClass(PersonEvent2.name)
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save()
        session.flush()
        session.clear()

        p = personClass.clazz.get(1)

        assertNull p.dateCreated
        assertNull p.lastUpdated

        p.name = "Wilma"
        p.save()
        session.flush()

        assertNull p.dateCreated
        assertNull p.lastUpdated
    }

    void testAutoTimestamps() {
        def personClass = ga.getDomainClass(PersonEvent.name)
        def p = personClass.newInstance()
        p.name = "Fred"
        p.save()
        session.flush()
        session.clear()

        p = personClass.clazz.get(1)
        sleep(2000)

        assertEquals p.dateCreated, p.lastUpdated

        p.name = "Wilma"
        p.save()
        session.flush()

        assertTrue p.dateCreated.before(p.lastUpdated)
    }

    void testOnloadEvent() {
        def personClass = ga.getDomainClass(PersonEvent.name)
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save()
        session.flush()
        session.clear()

        p = personClass.clazz.get(1)
        assertEquals "Bob", p.name
    }

    void testBeforeDeleteEvent() {
        def personClass = ga.getDomainClass(PersonEvent.name)
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save()
        session.flush()

        p.delete()
        session.flush()

        assertTrue "delete event should have fired",p.eventList.contains("before-delete")
    }

    void testBeforeUpdateEvent() {
        def personClass = ga.getDomainClass(PersonEvent2.name).clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save(flush:true)
        session.clear()

        p = personClass.get(1)
        assertEquals "Fred", p.name

        p.name = "Bob"
        assertNotNull p.save(flush:true)

        assertEquals "Wilma", p.name
        session.clear()

        p = personClass.get(1)
        assertEquals "Wilma", p.name
    }

    void testBeforeInsertEvent() {
        def personClass = ga.getDomainClass(PersonEvent.name).clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save(flush:true)
        session.flush()

        assertEquals "Bob", p.name

        session.clear()

        p = personClass.get(1)

        assertEquals "Bob", p.name
        p.name = "Fred"
        p.save(flush:true)

        assertEquals "Fred", p.name
    }

    void testNoArgBeforeValidateWhenCallingSave() {
        def personClass = ga.getDomainClass(PersonWithNoArgBeforeValidate.name).clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save(flush:true)
        session.flush()

        assertEquals 'wrong number of events', 2, p.eventList?.size()
        assertEquals 'before-validate-with-no-arg', p.eventList[0]
        assertEquals 'name-validated', p.eventList[1]
    }

    void testOverloadedBeforeValidateWhenCallingSave() {
        def personClass = ga.getDomainClass(PersonWithOverloadedBeforeValidate.name).clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save(flush:true)
        session.flush()

        assertEquals 'wrong number of events', 2, p.eventList?.size()
        assertEquals 'before-validate-with-no-arg', p.eventList[0]
        assertEquals 'name-validated', p.eventList[1]
    }

    void testNoArgBeforeValidateWhenCallingValidate() {
        def personClass = ga.getDomainClass(PersonWithNoArgBeforeValidate.name).clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        p.validate()

        assertEquals 'wrong number of events', 2, p.eventList?.size()
        assertEquals 'before-validate-with-no-arg', p.eventList[0]
        assertEquals 'name-validated', p.eventList[1]
    }

    void testListArgBeforeValidateWhenCallingSave() {
        def personClass = ga.getDomainClass(PersonWithListArgBeforeValidate.name).clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save(flush:true)
        session.flush()
        assertEquals 'wrong number of events', 2, p.eventList?.size()
        assertEquals 'before-validate-with-list-arg: null', p.eventList[0]
        assertEquals 'name-validated', p.eventList[1]
    }

    void testListArgBeforeValidateWhenCallingValidate() {
        def personClass = ga.getDomainClass(PersonWithListArgBeforeValidate.name).clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        p.validate()

        assertEquals 'wrong number of events', 2, p.eventList?.size()
        assertEquals 'before-validate-with-list-arg: null', p.eventList[0]
        assertEquals 'name-validated', p.eventList[1]

        p = personClass.newInstance()

        p.name = "Fred"
        p.validate(['name'])

        assertEquals 'wrong number of events', 2, p.eventList?.size()
        assertEquals 'before-validate-with-list-arg: [name]', p.eventList[0]
        assertEquals 'name-validated', p.eventList[1]

        p = personClass.newInstance()

        p.name = "Fred"
        p.validate(['name', 'age'])

        assertEquals 'wrong number of events', 2, p.eventList?.size()
        assertEquals 'before-validate-with-list-arg: [name, age]', p.eventList[0]
        assertEquals 'name-validated', p.eventList[1]
    }

    void testOverloadedBeforeValidateWhenCallingValidate() {
        def personClass = ga.getDomainClass(PersonWithOverloadedBeforeValidate.name).clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        p.validate()

        assertEquals 'wrong number of events', 2, p.eventList?.size()
        assertEquals 'before-validate-with-no-arg', p.eventList[0]
        assertEquals 'name-validated', p.eventList[1]

        p = personClass.newInstance()

        p.name = "Fred"
        p.validate(['name'])

        assertEquals 'wrong number of events', 2, p.eventList?.size()
        assertEquals 'before-validate-with-list-arg: [name]', p.eventList[0]
        assertEquals 'name-validated', p.eventList[1]

        p = personClass.newInstance()

        p.name = "Fred"
        p.validate(['name', 'age'])

        assertEquals 'wrong number of events', 2, p.eventList?.size()
        assertEquals 'before-validate-with-list-arg: [name, age]', p.eventList[0]
        assertEquals 'name-validated', p.eventList[1]
    }

    void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

'''
    }
}


@Entity
class PersonWithNoArgBeforeValidate {
    String name
    Integer age
    def eventList = []
    def beforeValidate() { eventList << "before-validate-with-no-arg" }
    static constraints = {
        name validator: { val, per ->
            per.eventList << 'name-validated'
            true
        }
    }
}
@Entity
class PersonWithListArgBeforeValidate {
    String name
    Integer age
    def eventList = []
    def beforeValidate(List propertiesToValidate) {
        eventList << "before-validate-with-list-arg: ${propertiesToValidate}".toString()
    }
    static constraints = {
        name validator: { val, per ->
            per.eventList << 'name-validated'
            true
        }
    }
}
@Entity
class PersonWithOverloadedBeforeValidate {
    String name
    Integer age
    def eventList = []
    def beforeValidate() { eventList << "before-validate-with-no-arg" }
    def beforeValidate(List propertiesToValidate) {
        eventList << "before-validate-with-list-arg: ${propertiesToValidate}".toString()
    }
    static constraints = {
        name validator: { val, per ->
            per.eventList << 'name-validated'
            true
        }
    }
}
@Entity
class PersonEvent {
    Long id
    Long version
    String name
    Date dateCreated
    Date lastUpdated

    def afterLoad() {
        eventList << "after-load"
        name = "Bob"
    }

    def eventList = []
    def beforeDelete() { eventList << "before-delete" }
    def beforeUpdate() { eventList << "before-update" }
    def beforeInsert() { name = "Bob" }

    static hasMany = [addresses:Address]
}

@Entity
class Address {
    String postCode
    Date lastUpdated
    static belongsTo = [person:PersonEvent]
}

class PersonEvent2 {
    Long id
    Long version
    String name
    Date dateCreated
    Date lastUpdated

    def eventList = []
    def beforeUpdate() {
        name = "Wilma"
    }
    static mapping = {
        autoTimestamp false
    }
    static constraints = {
        dateCreated nullable:true
        lastUpdated nullable:true
    }
}
