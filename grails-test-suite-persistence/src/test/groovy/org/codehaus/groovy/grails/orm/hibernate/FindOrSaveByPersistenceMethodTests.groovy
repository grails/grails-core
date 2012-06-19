package org.codehaus.groovy.grails.orm.hibernate

class FindOrSaveByPersistenceMethodTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [Person, Pet]
    }

    void testFindOrSaveByWithMultipleAndInExpression() {
        new Person(firstName: 'Jake', lastName: 'Brown', age: 11).save()
        new Person(firstName: 'Jeff', lastName: 'Brown', age: 41).save()
        new Person(firstName: 'Zack', lastName: 'Galifianakis', age: 41).save()
        def zackB = new Person(firstName: 'Zack', lastName: 'Brown', age: 14).save()
        assertNotNull zackB.id

        assertEquals 4, Person.count()

        def person = Person.findOrSaveByFirstNameAndLastNameAndAge('Zack', 'Brown', 14)
        assertNotNull 'findOrSaveBy should not have returned null', person
        assertEquals zackB.id, person.id
        assertEquals 'Zack', person.firstName
        assertEquals 'Brown', person.lastName
        assertEquals 14, person.age

        assertEquals 4, Person.count()

        person = Person.findOrSaveByFirstNameAndLastNameAndAge('Johnny', 'Winter', 68)
        assertNotNull 'findOrSaveBy should not have returned null', person
        assertEquals 'Johnny', person.firstName
        assertEquals 'Winter', person.lastName
        assertEquals 68, person.age

        assertEquals 5, Person.count()
    }

    void testFindOrSaveByForARecordThatDoesExistInTheDatabase() {
        def ginger = new Person(firstName: 'Ginger', lastName: 'Baker').save(flush: true)
        assertNotNull 'save should not have returned null', ginger

        def person = Person.findOrSaveByLastName('Baker')
        assertNotNull 'findOrSaveByLastName should not have returned null', person
        assertEquals 'wrong firstName', 'Ginger', person.firstName
        assertEquals 'wrong lastName', 'Baker', person.lastName
        assertEquals 'wrong id', ginger.id, person.id

        person = Person.findOrCreateByFirstName('Ginger')
        assertNotNull 'findOrSaveByFirstName should not have returned null', person
        assertEquals 'wrong firstName', 'Ginger', person.firstName
        assertEquals 'wrong lastName', 'Baker', person.lastName
        assertEquals 'wrong id', ginger.id, person.id

        person = Person.findOrSaveByLastNameAndFirstName('Baker', 'Ginger')
        assertNotNull 'findOrCreateByLastNameAndFirstName should not have returned null', person
        assertEquals 'wrong firstName', 'Ginger', person.firstName
        assertEquals 'wrong lastName', 'Baker', person.lastName
        assertEquals 'wrong id', ginger.id, person.id
    }

    void testFindOrSaveByForARecordThatDoesNotExistInTheDatabase() {
        def person = Person.findOrSaveByLastName('Clapton')
        assertNotNull 'person should not have been null', person
        assertNull 'firstName should have been null', person.firstName
        assertEquals 'wrong lastName', 'Clapton', person.lastName
        assertTrue 'person should have failed validation', person.hasErrors()
        assertNotNull 'firstName should have failed validation', person.errors.getFieldError('firstName')
        assertNull 'id should  have been null', person.id
    }

    void testFindOrSaveByWithAnAndClause() {
        def person = Person.findOrSaveByFirstNameAndLastName('Jack', 'Bruce')
        assertNotNull 'person should not have been null', person
        assertEquals 'wrong firstName', 'Jack', person.firstName
        assertEquals 'wrong lastName', 'Bruce', person.lastName
        assertNotNull 'id should not have been null', person.id
    }

    void testPatternsWhichShouldThrowMissingMethodException() {
        shouldFail(MissingMethodException) {
            Person.findOrSaveByLastNameLike('B%')
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByLastNameIlike('B%')
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByLastNameInList(['Joe', 'Bob'])
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByFirstNameOrLastName('Ginger', 'Baker')
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByFirstNameRlike('B')
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByFirstNameNotEqual('B')
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByFirstNameGreaterThan('B')
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByFirstNameLessThan('B')
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByFirstNameIsNull()
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByFirstNameIsNotNull()
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByFirstNameBetween('A', 'C')
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByFirstNameGreaterThanEquals('A')
        }
        shouldFail(MissingMethodException) {
            Person.findOrSaveByFirstNameLessThanEquals('A')
        }
    }
}
