package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

class FindOrCreateWherePersistenceMethodTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [Person]
    }

    void testFindOrCreateWhereForNonExistingRecord() {
        if(notYetImplemented()) return

        def domainClass = ga.getDomainClass(Person.name).clazz

        def person = domainClass.findWhere(firstName: 'Robert', lastName: 'Fripp')

        assertNotNull 'findOrCreateWhere should have returned a Person', person
        assertEquals 'Robert', person.firstName
        assertEquals 'Fripp', person.lastName
    }
}

@Entity
class Person {
    String firstName
    String lastName
}

