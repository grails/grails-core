package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class FindOrCreateWherePersistenceMethodTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [Person]
    }

    void testFindOrCreateWhereForNonExistingRecord() {
        def domainClass = ga.getDomainClass(Person.name).clazz

        def person = domainClass.findOrCreateWhere(firstName: 'Robert', lastName: 'Fripp')

        assertNotNull 'findOrCreateWhere should have returned a Person', person
        assertEquals 'Robert', person.firstName
        assertEquals 'Fripp', person.lastName
    }

    void testFindOrCreateWhereForExistingRecord() {
        def domainClass = ga.getDomainClass(Person.name).clazz

        def person = domainClass.newInstance()
        person.firstName = 'Adrian'
        person.lastName = 'Belew'
        assertNotNull 'save failed', person.save()

        def personId = person.id
        assertNotNull 'id should not have been ull', personId

        person = domainClass.findOrCreateWhere(firstName: 'Adrian', lastName: 'Belew')
        assertNotNull 'findOrCreateWhere should not have returned null', person
        assertEquals 'Adrian', person.firstName
        assertEquals 'Belew', person.lastName
        assertEquals personId, person.id
    }
}

