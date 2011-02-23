package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 28, 2007
 */
class FindByMethodTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
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
class FindByMethodUser {
    Long id
    Long version
    String firstName

    Set books
    static hasMany = [books:FindByMethodBook]
}
class FindByBooleanPropertyBook {
    Long id
    Long version
    String author
    String title
    Boolean published
}
class Highway {
    Long id
    Long version
    Boolean bypassed
    String name
}
'''
    }

    void testNullParameters() {
        def bookClass = ga.getDomainClass("FindByMethodBook").clazz

        assertNotNull bookClass.newInstance(title:"The Stand").save()

        assertNotNull bookClass.findByReleaseDate(null)
        assertNotNull bookClass.findByTitleAndReleaseDate("The Stand", null)
    }

    void testFindByIsNotNull() {
        def userClass = ga.getDomainClass("FindByMethodUser").clazz

        userClass.newInstance(firstName:"Bob").save()
        userClass.newInstance(firstName:"Jerry").save()
        userClass.newInstance(firstName:"Fred").save(flush:true)

        def users = userClass.findAllByFirstNameIsNotNull()
        users = userClass.findAllByFirstNameIsNotNull()

        assertEquals 3, users.size()
    }

    // test for GRAILS-3712
    void testFindByWithJoinQueryOnAssociation() {

        def User = ga.getDomainClass("FindByMethodUser").clazz

        def user = User.newInstance(firstName:"Stephen")
        assertNotNull user.addToBooks(title:"The Shining")
                          .addToBooks(title:"The Stand")
                          .save(flush:true)

        session.clear()

        user = User.findByFirstName("Stephen", [fetch:[books:'eager']])
        assertEquals 2, user.books.size()
    }

    void testBooleanPropertyQuery() {
        def highwayClass = ga.getDomainClass('Highway').clazz
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

        def bookClass = ga.getDomainClass("FindByBooleanPropertyBook").clazz
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
        def bookClass = ga.getDomainClass("FindByMethodBook").clazz

        assertNotNull bookClass.newInstance(title:"The Stand", writtenBy: 'Stephen King').save()

        def results = bookClass.findAllByWrittenByAndTitle('Stephen King', 'The Stand')
        assertEquals 1, results?.size()
    }
}
