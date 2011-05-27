package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class FindOrSaveWherePersistenceMethodTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [Person]
    }

    void testFindOrSaveWhereForNonExistingRecord() {
        def domainClass = ga.getDomainClass(Person.name).clazz

        def person = domainClass.findOrSaveWhere(firstName: 'Robert', lastName: 'Fripp')

        assertNotNull 'findOrSaveWhere should have returned a Person', person
        assertEquals 'Robert', person.firstName
        assertEquals 'Fripp', person.lastName
        assertNotNull person.id
    }

    void testFindOrSaveWhereForExistingRecord() {
        def domainClass = ga.getDomainClass(Person.name).clazz

        def person = domainClass.newInstance()
        person.firstName = 'Adrian'
        person.lastName = 'Belew'
        assertNotNull 'save failed', person.save()

        def personId = person.id
        assertNotNull 'id should not have been ull', personId

        person = domainClass.findOrSaveWhere(firstName: 'Adrian', lastName: 'Belew')
        assertNotNull 'findOrSaveWhere should not have returned null', person
        assertEquals 'Adrian', person.firstName
        assertEquals 'Belew', person.lastName
        assertEquals personId, person.id
    }
}

