package org.codehaus.groovy.grails.orm.hibernate

class FindOrCreateByPersistenceMethodTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [Person]
    }

    void testFindOrCreateByForARecordThatDoesExistInTheDatabase() {
        def ginger = new Person(firstName: 'Ginger', lastName: 'Baker').save(flush: true)
        assertNotNull 'save should not have returned null', ginger

        def person = Person.findOrCreateByLastName('Baker')
        assertNotNull 'findOrCreateByLastName should not have returned null', person
        assertEquals 'wrong firstName', 'Ginger', person.firstName
        assertEquals 'wrong lastName', 'Baker', person.lastName
        assertEquals 'wrong id', ginger.id, person.id

        person = Person.findOrCreateByFirstName('Ginger')
        assertNotNull 'findOrCreateByFirstName should not have returned null', person
        assertEquals 'wrong firstName', 'Ginger', person.firstName
        assertEquals 'wrong lastName', 'Baker', person.lastName
        assertEquals 'wrong id', ginger.id, person.id

        person = Person.findOrCreateByLastNameAndFirstName('Baker', 'Ginger')
        assertNotNull 'findOrCreateByLastNameAndFirstName should not have returned null', person
        assertEquals 'wrong firstName', 'Ginger', person.firstName
        assertEquals 'wrong lastName', 'Baker', person.lastName
        assertEquals 'wrong id', ginger.id, person.id
    }

    void testFindOrCreateByForARecordThatDoesNotExistInTheDatabase() {
        def person = Person.findOrCreateByLastName('Clapton')
        assertNotNull 'person should not have been null', person
        assertNull 'firstName should have been null', person.firstName
        assertEquals 'wrong lastName', 'Clapton', person.lastName
        assertNull 'id should have been null', person.id
    }

    void testFindOrCreateByWithAnAndClause() {
        def person = Person.findOrCreateByFirstNameAndLastName('Jack', 'Bruce')
        assertNotNull 'person should not have been null', person
        assertEquals 'wrong firstName', 'Jack', person.firstName
        assertEquals 'wrong lastName', 'Bruce', person.lastName
        assertNull 'id should have been null', person.id
    }

    void testPatternsWhichShouldThrowMissingMethodException() {
        shouldFail(MissingMethodException) {
            Person.findOrCreateByLastNameLike('B%')
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByLastNameIlike('B%')
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByLastNameInList(['Joe', 'Bob'])
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByFirstNameOrLastName('Ginger', 'Baker')
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByFirstNameRlike('B')
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByFirstNameNotEqual('B')
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByFirstNameGreaterThan('B')
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByFirstNameLessThan('B')
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByFirstNameIsNull()
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByFirstNameIsNotNull()
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByFirstNameBetween('A', 'C')
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByFirstNameGreaterThanEquals('A')
        }
        shouldFail(MissingMethodException) {
            Person.findOrCreateByFirstNameLessThanEquals('A')
        }
    }
}
