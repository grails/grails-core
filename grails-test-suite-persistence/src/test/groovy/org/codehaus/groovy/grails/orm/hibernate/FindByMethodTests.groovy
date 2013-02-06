package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class FindByMethodTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [FindByMethodBook, FindByMethodUser, FindByBooleanPropertyBook, Highway, Person, Pet, Nose, Face]
    }
	
	void testNullAsSoleParameter() {
		def bookClass = ga.getDomainClass(FindByMethodBook.name).clazz
		assertNotNull bookClass.findAllByReleaseDate(null)
		
		// per GRAILS-3463, this second call was throwing MissingMethodException
		assertNotNull bookClass.findAllByReleaseDate(null)
	}

    void testNullParameters() {
        def bookClass = ga.getDomainClass(FindByMethodBook.name).clazz

        assertNotNull bookClass.newInstance(title:"The Stand").save()

        assertNotNull bookClass.findByReleaseDate(null)
        assertNotNull bookClass.findByTitleAndReleaseDate("The Stand", null)
    }

    void testFindByIsNotNull() {
        def userClass = ga.getDomainClass(FindByMethodUser.name).clazz

        userClass.newInstance(firstName:"Bob").save()
        userClass.newInstance(firstName:"Jerry").save()
        userClass.newInstance(firstName:"Fred").save(flush:true)

        def users = userClass.findAllByFirstNameIsNotNull()
        users = userClass.findAllByFirstNameIsNotNull()

        assertEquals 3, users.size()
    }

    // test for GRAILS-3712
    void testFindByWithJoinQueryOnAssociation() {

        def User = ga.getDomainClass(FindByMethodUser.name).clazz

        def user = User.newInstance(firstName:"Stephen")
        assertNotNull user.addToBooks(title:"The Shining")
                          .addToBooks(title:"The Stand")
                          .save(flush:true)

        session.clear()

        user = User.findByFirstName("Stephen", [fetch:[books:'eager']])
        assertEquals 2, user.books.size()
    }

    void testBooleanPropertyQuery() {
        def highwayClass = ga.getDomainClass(Highway.name).clazz
        assertNotNull highwayClass.newInstance(bypassed: true, name: 'Bypassed Highway').save()
        assertNotNull highwayClass.newInstance(bypassed: true, name: 'Bypassed Highway').save()
        assertNotNull highwayClass.newInstance(bypassed: false, name: 'Not Bypassed Highway').save()
        assertNotNull highwayClass.newInstance(bypassed: false, name: 'Not Bypassed Highway').save()

        def highways = highwayClass.findAllBypassedByName('Not Bypassed Highway')
        assertEquals 0, highways?.size()

        highways = highwayClass.findAllNotBypassedByName('Not Bypassed Highway')
        assertEquals 2, highways?.size()
        assertEquals 'Not Bypassed Highway', highways[0].name
        assertEquals 'Not Bypassed Highway', highways[1].name

        highways = highwayClass.findAllBypassedByName('Bypassed Highway')
        assertEquals 2, highways?.size()
        assertEquals 'Bypassed Highway', highways[0].name
        assertEquals 'Bypassed Highway', highways[1].name

        highways = highwayClass.findAllNotBypassedByName('Bypassed Highway')
        assertEquals 0, highways?.size()

        highways = highwayClass.findAllBypassed()
        assertEquals 2, highways?.size()
        assertEquals 'Bypassed Highway', highways[0].name
        assertEquals 'Bypassed Highway', highways[1].name

        highways = highwayClass.findAllNotBypassed()
        assertEquals 2, highways?.size()
        assertEquals 'Not Bypassed Highway', highways[0].name
        assertEquals 'Not Bypassed Highway', highways[1].name

        def highway = highwayClass.findNotBypassed()
        assertEquals 'Not Bypassed Highway', highway?.name

        highway = highwayClass.findBypassed()
        assertEquals 'Bypassed Highway', highway?.name

        highway = highwayClass.findNotBypassedByName('Not Bypassed Highway')
        assertEquals 'Not Bypassed Highway', highway?.name

        highway = highwayClass.findBypassedByName('Bypassed Highway')
        assertEquals 'Bypassed Highway', highway?.name

        def bookClass = ga.getDomainClass(FindByBooleanPropertyBook.name).clazz
        assertNotNull bookClass.newInstance(author: 'Jeff', title: 'Fly Fishing For Everyone', published: false).save()
        assertNotNull bookClass.newInstance(author: 'Jeff', title: 'DGGv2', published: true).save()
        assertNotNull bookClass.newInstance(author: 'Graeme', title: 'DGGv2', published: true).save()
        assertNotNull bookClass.newInstance(author: 'Dierk', title: 'GINA', published: true).save()

        def book = bookClass.findPublishedByAuthor('Jeff')
        assertEquals 'Jeff', book.author
        assertEquals 'DGGv2', book.title

        book = bookClass.findPublishedByAuthor('Graeme')
        assertEquals 'Graeme', book.author
        assertEquals 'DGGv2', book.title

        book = bookClass.findPublishedByTitleAndAuthor('DGGv2', 'Jeff')
        assertEquals 'Jeff', book.author
        assertEquals 'DGGv2', book.title

        book = bookClass.findNotPublishedByAuthor('Jeff')
        assertEquals 'Fly Fishing For Everyone', book.title

        book = bookClass.findPublishedByTitleOrAuthor('Fly Fishing For Everyone', 'Dierk')
        assertEquals 'GINA', book.title

        assertNotNull bookClass.findPublished()

        book = bookClass.findNotPublished()
        assertEquals 'Fly Fishing For Everyone', book?.title

        def books = bookClass.findAllPublishedByTitle('DGGv2')
        assertEquals 2, books?.size()

        books = bookClass.findAllPublished()
        assertEquals 3, books?.size()

        books = bookClass.findAllNotPublished()
        assertEquals 1, books?.size()

        books = bookClass.findAllPublishedByTitleAndAuthor('DGGv2', 'Graeme')
        assertEquals 1, books?.size()

        books = bookClass.findAllPublishedByAuthorOrTitle('Graeme', 'GINA')
        assertEquals 2, books?.size()

        books = bookClass.findAllNotPublishedByAuthor('Jeff')
        assertEquals 1, books?.size()

        books = bookClass.findAllNotPublishedByAuthor('Graeme')
        assertEquals 0, books?.size()
    }

    void testQueryByPropertyWith_By_InName() {
        // GRAILS-5929
        def bookClass = ga.getDomainClass(FindByMethodBook.name).clazz

        assertNotNull bookClass.newInstance(title:"The Stand", writtenBy: 'Stephen King').save()

        def results = bookClass.findAllByWrittenByAndTitle('Stephen King', 'The Stand')
        assertEquals 1, results?.size()
    }

    void testDynamicFinderWithMultipleAnd() {
        def jakeB = new Person(firstName: 'Jake', lastName: 'Brown', age: 11).save()
        new Person(firstName: 'Jeff', lastName: 'Brown', age: 41).save()
        new Person(firstName: 'Zack', lastName: 'Galifianakis', age: 41).save()
        new Person(firstName: 'Zack', lastName: 'Brown', age: 14).save()

        def people = Person.findAllByFirstNameAndLastNameInListAndAgeGreaterThan('Zack', ['Brown', 'Galifianakis'], 99)
        assertEquals 0, people.size()

        people = Person.findAllByFirstNameAndLastNameInListAndAgeGreaterThan('Zack', ['Brown', 'Galifianakis'], 25)
        assertEquals 1, people.size()

        people = Person.findAllByFirstNameAndLastNameInListAndAgeGreaterThan('Zack', ['Brown', 'Galifianakis'], 5)
        assertEquals 2, people.size()

        def person = Person.findByFirstNameAndLastNameAndAge('Jake', 'Brown', 41)
        assertNull person

        person = Person.findByFirstNameAndLastNameAndAge('Jake', 'Brown', 11)
        assertNotNull person
        assertEquals 'Jake', person.firstName
        assertEquals 'Brown', person.lastName
        assertEquals 11, person.age
        assertEquals jakeB.id, person.id
    }

    void testDynamicFinderWithMultipleOr() {
        def jakeB = new Person(firstName: 'Jake', lastName: 'Brown', age: 11).save()
        new Person(firstName: 'Jeff', lastName: 'Brown', age: 41).save()
        new Person(firstName: 'Zack', lastName: 'Galifianakis', age: 41).save()
        new Person(firstName: 'Zack', lastName: 'Brown', age: 14).save()

        def people = Person.findAllByFirstNameOrLastNameOrAge('Jake', 'Galifianakis', 99)
        assertEquals 2, people.size()
        assertNotNull people.find { it.firstName == 'Jake' && it.lastName == 'Brown' && it.age == 11 }
        assertNotNull people.find { it.firstName == 'Zack' && it.lastName == 'Galifianakis' && it.age == 41 }

        people = Person.findAllByFirstNameOrLastNameOrAgeInList('Bob', 'Newhard', [1, 2, 3, 41])
        assertEquals 2, people.size()
        assertNotNull people.find { it.firstName == 'Jeff' && it.lastName == 'Brown' && it.age == 41 }
        assertNotNull people.find { it.firstName == 'Zack' && it.lastName == 'Galifianakis' && it.age == 41 }
    }
}

@Entity
class FindByMethodBook {
    Long id
    Long version
    String title
    Date releaseDate
    String writtenBy
    static constraints  = {
        releaseDate(nullable:true)
        writtenBy(nullable: true)
    }
}
@Entity
class FindByMethodUser {
    Long id
    Long version
    String firstName

    Set books
    static hasMany = [books:FindByMethodBook]
}
@Entity
class FindByBooleanPropertyBook {
    Long id
    Long version
    String author
    String title
    Boolean published
}
@Entity
class Highway {
    Long id
    Long version
    Boolean bypassed
    String name
}
